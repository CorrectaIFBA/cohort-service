package io.correcta.cohort.security.components

import io.correcta.cohort.exceptions.InternalServerErrorException
import io.correcta.cohort.security.models.ProfilePrincipal
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/** Reads the caller [ProfilePrincipal] from the security context. */
@Component("scopeEvaluator")
class ScopeEvaluatorComponent {

  /** Tells if the request has an authenticated profile. */
  fun isLoggedUser(): Boolean = currentProfile() != null

  /** Tells if the caller profile type is [profileType], case-insensitive. */
  fun isProfileType(profileType: String): Boolean = currentProfile()?.profileType.equals(profileType, ignoreCase = true)

  /** Returns the caller profile, or null for anonymous requests. */
  fun currentProfile(): ProfilePrincipal? = currentAuthentication()?.principal as? ProfilePrincipal

  /** Returns the caller profile. Use only behind [io.correcta.cohort.security.annotations.IsLoggedUser]. */
  fun authenticatedProfile(): ProfilePrincipal = currentProfile()
    ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")

  private fun currentAuthentication(): Authentication? = SecurityContextHolder.getContext().authentication
    ?.takeIf { it.isAuthenticated }
}