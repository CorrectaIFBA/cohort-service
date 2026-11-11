package io.correcta.cohort.entities

/** Lifecycle states of a [CohortInvitationCode]. */
enum class CohortInvitationCodeState {
  ACTIVE,
  EXPIRED,
  REVOKED,
}