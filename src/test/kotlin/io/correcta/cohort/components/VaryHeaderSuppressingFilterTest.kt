package io.correcta.cohort.components

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals

class VaryHeaderSuppressingFilterTest {

  @Test
  fun `drops Vary headers and keeps the others`() {
    val response = MockHttpServletResponse()
    response.addHeader("Vary", "Existing")
    var names: Collection<String> = emptyList()
    val chain = FilterChain { _, wrapped ->
      wrapped as HttpServletResponse
      wrapped.addHeader("Vary", "Origin")
      wrapped.setHeader("vary", "Accept")
      wrapped.addHeader("X-Added", "1")
      wrapped.setHeader("X-Set", "2")
      names = wrapped.headerNames
    }

    VaryHeaderSuppressingFilter().doFilter(MockHttpServletRequest(), response, chain)

    assertEquals(listOf("Existing"), response.getHeaders("Vary"))
    assertEquals("1", response.getHeader("X-Added"))
    assertEquals("2", response.getHeader("X-Set"))
    assertEquals(setOf("X-Added", "X-Set"), names.toSet())
  }
}