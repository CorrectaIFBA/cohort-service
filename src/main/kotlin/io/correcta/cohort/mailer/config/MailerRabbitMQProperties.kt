package io.correcta.cohort.mailer.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mailer.rabbitmq")
data class MailerRabbitMQProperties(
  val exchange: String = "send-transactional-mail-exchange",
  val routingKey: String = "send-transactional-mail-routing-key",
) {
  init {
    require(listOf(exchange, routingKey).all { it.isNotBlank() && it.length <= 200 })
  }
}