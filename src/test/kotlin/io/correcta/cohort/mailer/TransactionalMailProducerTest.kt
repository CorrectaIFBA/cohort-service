package io.correcta.cohort.mailer

import io.correcta.cohort.mailer.config.MailerRabbitMQConfig
import io.correcta.cohort.mailer.config.MailerRabbitMQProperties
import io.correcta.cohort.mailer.requests.SendTransactionalMailRequest
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TransactionalMailProducerTest {

  private val rabbitTemplate = mock<RabbitTemplate>()
  private val producer = TransactionalMailProducer(rabbitTemplate, MailerRabbitMQProperties(), "https://app.example/")

  @BeforeEach
  fun startTransaction() {
    TransactionSynchronizationManager.initSynchronization()
  }

  @AfterEach
  fun endTransaction() {
    TransactionSynchronizationManager.clearSynchronization()
  }

  @Test
  fun `queues the invitation e-mail after commit`() {
    val invitation = Fixtures.invitation(expiresAt = Instant.parse("2026-03-10T15:30:00Z"))
    invitation.cohort.title = "Algoritmos"

    producer.sendCohortInvitationEmail(invitation, Fixtures.profile().copy(displayName = "Ana Souza"), "Prof. Carlos")
    verifyNoInteractions(rabbitTemplate)

    commit()

    val captor = argumentCaptor<Any>()
    verify(rabbitTemplate).convertAndSend(eq("send-transactional-mail-exchange"), eq("send-transactional-mail-routing-key"), captor.capture())
    val request = assertIs<SendTransactionalMailRequest>(captor.firstValue)

    assertEquals("cohort-invitation-email-invitation-student", request.id)
    assertEquals("student@example.edu", request.to.single().email)
    assertEquals("Ana", request.to.single().name)
    assertEquals("Ana, você foi convidado para a turma Algoritmos no Correcta", request.subject)
    assertEquals("email-templates", request.template.bucket)
    assertEquals("templates/cohort-invitation/v1.html", request.template.key)
    assertEquals("estudante", request.data["role"])
    assertEquals("Prof. Carlos", request.data["inviterName"])
    assertEquals("10 de março às 12:30", request.data["expiresAt"])
    assertEquals("https://app.example/classes/invitation/cohort-1", request.data["acceptUrl"])
  }

  @Test
  fun `sends no expiration when the invitation does not expire`() {
    producer.sendCohortInvitationEmail(Fixtures.invitation(expiresAt = null), Fixtures.profile(), null)
    commit()

    val captor = argumentCaptor<Any>()
    verify(rabbitTemplate).convertAndSend(any<String>(), any<String>(), captor.capture())
    assertNull((captor.firstValue as SendTransactionalMailRequest).data["expiresAt"])
  }

  @Test
  fun `does not fail when the broker is unavailable`() {
    whenever(rabbitTemplate.convertAndSend(any<String>(), any<String>(), any<Any>())).doThrow(mock<AmqpException>())

    producer.sendCohortInvitationEmail(Fixtures.invitation(), Fixtures.profile(), null)

    assertDoesNotThrow { commit() }
  }

  @Test
  fun `validates the queue properties`() {
    assertIs<Jackson2JsonMessageConverter>(MailerRabbitMQConfig().rabbitMessageConverter())
    assertEquals("exchange", MailerRabbitMQProperties("exchange", "key").exchange)
    assertThrows<IllegalArgumentException> { MailerRabbitMQProperties(exchange = " ") }
    assertThrows<IllegalArgumentException> { MailerRabbitMQProperties(exchange = "a".repeat(201)) }
    assertThrows<IllegalArgumentException> { MailerRabbitMQProperties(routingKey = "") }
    assertThrows<IllegalArgumentException> { MailerRabbitMQProperties(routingKey = "a".repeat(201)) }
  }

  private fun commit() = TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
}