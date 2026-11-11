package io.correcta.cohort.security

import io.correcta.cohort.exceptions.InternalServerErrorException
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.security.filter.HeaderAuthenticationFilter
import io.correcta.cohort.security.models.IdentityHeaders
import io.correcta.cohort.security.models.ProfilePrincipal
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurityTest {

  private val scopeEvaluator = ScopeEvaluatorComponent()

  @AfterEach
  fun clearContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `reads the principal from the identity headers`() {
    val request = MockHttpServletRequest("GET", "/api/v1/cohorts")
    request.addHeader(IdentityHeaders.PROFILE_ID, " profile-1 ")
    request.addHeader(IdentityHeaders.PERSON_ID, "person-1")
    request.addHeader(IdentityHeaders.PROFILE_TYPE, "FACULTY")
    request.addHeader(IdentityHeaders.LOGIN, "login")
    request.addHeader(IdentityHeaders.EMAIL, " ")
    val chain = MockFilterChain()

    HeaderAuthenticationFilter().doFilter(request, MockHttpServletResponse(), chain)

    assertNotNull(chain.request)
    assertEquals(
      ProfilePrincipal("profile-1", "person-1", "FACULTY", "login", null),
      SecurityContextHolder.getContext().authentication.principal,
    )
  }

  @Test
  fun `leaves requests without a profile anonymous`() {
    val request = MockHttpServletRequest("GET", "/api/v1/cohorts")
    request.addHeader(IdentityHeaders.PROFILE_ID, "   ")

    HeaderAuthenticationFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

    assertNull(SecurityContextHolder.getContext().authentication)
  }

  @Test
  fun `skips preflight requests`() {
    val request = MockHttpServletRequest("OPTIONS", "/api/v1/cohorts")
    request.addHeader(IdentityHeaders.PROFILE_ID, "profile-1")
    val chain = MockFilterChain()

    HeaderAuthenticationFilter().doFilter(request, MockHttpServletResponse(), chain)

    assertNotNull(chain.request)
    assertNull(SecurityContextHolder.getContext().authentication)
  }

  @Test
  fun `evaluates the authenticated profile`() {
    val principal = Fixtures.principal(profileType = "faculty")
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())

    assertTrue(scopeEvaluator.isLoggedUser())
    assertTrue(scopeEvaluator.isProfileType("FACULTY"))
    assertFalse(scopeEvaluator.isProfileType("STAFF"))
    assertEquals(principal, scopeEvaluator.authenticatedProfile())
    assertEquals("person-owner", principal.personId)
    assertEquals("owner", principal.login)
    assertEquals("owner@example.edu", principal.email)
  }

  @Test
  fun `treats missing, unauthenticated and foreign principals as anonymous`() {
    assertAnonymous()

    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(Fixtures.principal(), null)
    assertAnonymous()

    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("user", null, emptyList())
    assertAnonymous()
  }

  private fun assertAnonymous() {
    assertFalse(scopeEvaluator.isLoggedUser())
    assertFalse(scopeEvaluator.isProfileType("FACULTY"))
    assertNull(scopeEvaluator.currentProfile())
    assertThrows<InternalServerErrorException> { scopeEvaluator.authenticatedProfile() }
  }
}