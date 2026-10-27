package io.correcta.boilerplate.config

import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean

class IdentityClientConfig {

    @Bean
    fun internalTokenInterceptor(@Value("\${clients.identity-service.internal-token}") token: String): RequestInterceptor {
        return RequestInterceptor { it.header(INTERNAL_TOKEN_HEADER, token) }
    }

    companion object {
        const val INTERNAL_TOKEN_HEADER = "X-Internal-Token"
    }
}
