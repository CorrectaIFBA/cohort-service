package io.correcta.boilerplate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.EnableAspectJAutoProxy

@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = ["io.correcta.boilerplate.clients"])
@SpringBootApplication
class BoilerplateServiceApplication

fun main(args: Array<String>) {
    runApplication<BoilerplateServiceApplication>(*args)
}
