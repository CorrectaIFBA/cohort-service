package io.correcta.cohort.models.response

import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState

data class GetInternalCohortMembershipResponse(
  val cohortId: String,
  val cohortStatus: CohortState,
  val profileId: String,
  val role: CohortRole,
) {

  companion object {

    fun fromMembership(membership: CohortMembership): GetInternalCohortMembershipResponse = GetInternalCohortMembershipResponse(
      cohortId = membership.cohort.id!!,
      cohortStatus = membership.cohort.state,
      profileId = membership.profileId,
      role = membership.role,
    )
  }
}