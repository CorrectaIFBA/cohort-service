package io.correcta.cohort.config

import feign.RequestTemplate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConfigTest {

  @Test
  fun `sends the internal token to identity-service`() {
    val template = RequestTemplate()

    IdentityClientConfig().internalTokenInterceptor("secret").apply(template)

    assertEquals(listOf("secret"), template.headers()[IdentityClientConfig.INTERNAL_TOKEN_HEADER]?.toList())
  }

  @Test
  fun `declares the bearer security scheme`() {
    val openApi = SwaggerConfig().customOpenAPI()

    assertEquals("bearer", openApi.components.securitySchemes["bearerAuth"]!!.scheme)
  }
}