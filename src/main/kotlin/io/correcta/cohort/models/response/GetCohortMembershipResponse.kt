package io.correcta.cohort.models.response

import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole

data class GetCohortMembershipResponse(
  val cohortId: String,
  val profileId: String,
  val role: CohortRole,
  val joinedAt: String,
) {

  companion object {

    fun fromMembership(membership: CohortMembership): GetCohortMembershipResponse = GetCohortMembershipResponse(
      cohortId = membership.cohort.id!!,
      profileId = membership.profileId,
      role = membership.role,
      joinedAt = membership.joinedAt!!.toString(),
    )
  }
}