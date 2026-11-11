package io.correcta.cohort.security.components

import io.correcta.cohort.exceptions.InternalServerErrorException
import io.correcta.cohort.security.models.ProfilePrincipal
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component("scopeEvaluator")
class ScopeEvaluatorComponent {

  fun isLoggedUser(): Boolean = currentProfile() != null

  fun isProfileType(profileType: String): Boolean = currentProfile()?.profileType.equals(profileType, ignoreCase = true)

  fun currentProfile(): ProfilePrincipal? = currentAuthentication()?.principal as? ProfilePrincipal

  fun authenticatedProfile(): ProfilePrincipal = currentProfile()
    ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")

  private fun currentAuthentication(): Authentication? = SecurityContextHolder.getContext().authentication
    ?.takeIf { it.isAuthenticated }
}