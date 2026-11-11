package io.correcta.cohort.security.models

/** Authenticated caller built from [IdentityHeaders]. */
data class ProfilePrincipal(
  val profileId: String,
  val personId: String?,
  val profileType: String?,
  val login: String?,
  val email: String?,
)