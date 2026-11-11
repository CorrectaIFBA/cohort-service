package io.correcta.cohort.security

import io.correcta.cohort.exceptions.UnauthorizedException
import io.correcta.cohort.security.annotations.IsLoggedUser
import io.correcta.cohort.security.annotations.IsProfileType
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.security.components.aspects.SecurityAspect
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authorization.AuthorizationDeniedException
import java.lang.reflect.Method

class SecurityAspectTest {

  interface Marker

  interface OtherMarker

  interface SecuredApi : Marker {
    @IsLoggedUser
    fun logged()

    @IsLoggedUser
    @IsProfileType("FACULTY")
    fun faculty()

    fun open()
  }

  class SecuredApiImpl :
    SecuredApi,
    OtherMarker {
    override fun logged() = Unit

    override fun faculty() = Unit

    override fun open() = Unit
  }

  class Overloaded {
    @Suppress("UNUSED_PARAMETER")
    fun logged(value: String) = Unit
  }

  private val scopeEvaluator = mock<ScopeEvaluatorComponent>()
  private val aspect = SecurityAspect(scopeEvaluator)

  @Test
  fun `finds annotations declared on the interface of the implementation`() {
    whenever(scopeEvaluator.isLoggedUser()).doReturn(false)

    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint(implMethod("logged"), SecuredApiImpl())) }
  }

  @Test
  fun `finds annotations on the interface method without a target`() {
    whenever(scopeEvaluator.isLoggedUser()).doReturn(true)

    assertDoesNotThrow { aspect.authorize(joinPoint(apiMethod("logged"), null)) }
    verify(scopeEvaluator, never()).isProfileType("FACULTY")
  }

  @Test
  fun `ignores targets without a matching method`() {
    whenever(scopeEvaluator.isLoggedUser()).doReturn(false)

    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint(apiMethod("logged"), Overloaded())) }
  }

  @Test
  fun `requires the profile type`() {
    whenever(scopeEvaluator.isLoggedUser()).doReturn(true)
    whenever(scopeEvaluator.isProfileType("FACULTY")).doReturn(false)

    assertThrows<AuthorizationDeniedException> { aspect.authorize(joinPoint(implMethod("faculty"), SecuredApiImpl())) }
  }

  @Test
  fun `allows the matching profile type`() {
    whenever(scopeEvaluator.isLoggedUser()).doReturn(true)
    whenever(scopeEvaluator.isProfileType("FACULTY")).doReturn(true)

    assertDoesNotThrow { aspect.authorize(joinPoint(implMethod("faculty"), SecuredApiImpl())) }
  }

  @Test
  fun `allows methods without annotations`() {
    assertDoesNotThrow { aspect.authorize(joinPoint(implMethod("open"), SecuredApiImpl())) }
    verify(scopeEvaluator, never()).isLoggedUser()
  }

  @Test
  fun `allows join points that are not methods`() {
    val joinPoint = mock<JoinPoint>()
    whenever(joinPoint.signature).doReturn(mock<Signature>())

    assertDoesNotThrow { aspect.authorize(joinPoint) }
  }

  private fun implMethod(name: String): Method = SecuredApiImpl::class.java.getMethod(name)

  private fun apiMethod(name: String): Method = SecuredApi::class.java.getMethod(name)

  private fun joinPoint(method: Method, target: Any?): JoinPoint {
    val signature = mock<MethodSignature>()
    whenever(signature.method).doReturn(method)
    val joinPoint = mock<JoinPoint>()
    whenever(joinPoint.signature).doReturn(signature)
    whenever(joinPoint.target).doReturn(target)
    return joinPoint
  }
}