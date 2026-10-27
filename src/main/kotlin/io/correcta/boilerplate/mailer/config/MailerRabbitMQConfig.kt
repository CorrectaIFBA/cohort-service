package io.correcta.boilerplate.mailer.config

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MailerRabbitMQConfig {
    @Bean
    fun rabbitMessageConverter(): MessageConverter = Jackson2JsonMessageConverter()
}
