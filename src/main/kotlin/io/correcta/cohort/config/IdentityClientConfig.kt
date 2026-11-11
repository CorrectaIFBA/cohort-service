package io.correcta.cohort.config

import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean

/** Feign configuration for [io.correcta.cohort.clients.IdentityClient]. */
class IdentityClientConfig {

  /** Adds the `X-Internal-Token` header to every identity-service request. */
  @Bean
  fun internalTokenInterceptor(@Value("\${clients.identity-service.internal-token}") token: String): RequestInterceptor = RequestInterceptor { it.header(INTERNAL_TOKEN_HEADER, token) }

  companion object {
    const val INTERNAL_TOKEN_HEADER = "X-Internal-Token"
  }
}