package io.correcta.cohort

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.EnableAspectJAutoProxy

@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = ["io.correcta.cohort.clients"])
@SpringBootApplication
class BoilerplateServiceApplication

fun main(args: Array<String>) {
  runApplication<BoilerplateServiceApplication>(*args)
}