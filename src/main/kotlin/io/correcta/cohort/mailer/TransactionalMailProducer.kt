package io.correcta.cohort.mailer

import io.correcta.cohort.entities.CohortMembershipInvitation
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.mailer.config.MailerRabbitMQProperties
import io.correcta.cohort.mailer.requests.RecipientRef
import io.correcta.cohort.mailer.requests.SendTransactionalMailRequest
import io.correcta.cohort.mailer.requests.TemplateRef
import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Queues transactional e-mails on the mailer RabbitMQ exchange. */
@Component
class TransactionalMailProducer(
  val rabbitTemplate: RabbitTemplate,
  val mailerRabbitMQProperties: MailerRabbitMQProperties,
  @Value("\${mailer.app-url}") val appUrl: String,
) {

  private val log: Logger = LoggerFactory.getLogger(TransactionalMailProducer::class.java)

  /**
   * Queues the invitation e-mail for [invitee].
   *
   * The message is sent only after the current transaction commits. A broker failure is logged and does not fail the request.
   */
  fun sendCohortInvitationEmail(
    invitation: CohortMembershipInvitation,
    invitee: InstitutionalProfileResponse,
    inviterName: String?,
  ) {
    val firstName = invitee.displayName.substringBefore(' ')
    val cohortName = invitation.cohort.title
    val request = SendTransactionalMailRequest(
      id = "cohort-invitation-email-${invitation.id}",
      to = listOf(RecipientRef(email = invitee.email, name = firstName)),
      subject = "$firstName, você foi convidado para a turma $cohortName no Correcta",
      template = TemplateRef(
        bucket = "email-templates",
        key = "templates/cohort-invitation/v1.html",
      ),
      data = mapOf(
        "firstName" to firstName,
        "cohortName" to cohortName,
        "role" to ROLE_LABEL.getValue(invitation.role),
        "inviterName" to inviterName,
        "expiresAt" to invitation.expiresAt?.let(EXPIRATION_FORMAT::format),
        "acceptUrl" to "${appUrl.trimEnd('/')}/classes/invitation/${invitation.cohort.id}",
      ),
    )

    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
      override fun afterCommit() {
        try {
          rabbitTemplate.convertAndSend(
            mailerRabbitMQProperties.exchange,
            mailerRabbitMQProperties.routingKey,
            request,
          )
        } catch (exception: AmqpException) {
          log.error("Could not queue the e-mail for cohort invitation {}", invitation.id, exception)
        }
      }
    })
  }

  companion object {
    private val ROLE_LABEL = mapOf(
      CohortRole.STUDENT to "estudante",
      CohortRole.FACULTY to "docente",
    )

    private val EXPIRATION_FORMAT = DateTimeFormatter
      .ofPattern("d 'de' MMMM 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
      .withZone(ZoneId.of("America/Bahia"))
  }
}