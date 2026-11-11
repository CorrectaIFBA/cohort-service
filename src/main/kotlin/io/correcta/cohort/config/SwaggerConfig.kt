package io.correcta.cohort.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** OpenAPI configuration. */
@Configuration
class SwaggerConfig {
  /** Declares the bearer authentication scheme used by the gateway. */
  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    .components(
      Components()
        .addSecuritySchemes(
          "bearerAuth",
          SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT"),
        ),
    )
}