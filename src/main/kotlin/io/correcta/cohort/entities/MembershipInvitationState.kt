package io.correcta.cohort.entities

/** Lifecycle states of a [CohortMembershipInvitation]. */
enum class MembershipInvitationState {
  PENDING,
  ACCEPTED,
  REJECTED,
  REVOKED,
  EXPIRED,
}