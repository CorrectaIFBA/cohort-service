package io.correcta.cohort.mailer.config

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** RabbitMQ configuration for the mailer. */
@Configuration
class MailerRabbitMQConfig {
  /** Serializes messages as JSON. */
  @Bean
  fun rabbitMessageConverter(): MessageConverter = Jackson2JsonMessageConverter()
}