package io.correcta.boilerplate.models.response

import io.correcta.boilerplate.entities.CohortMembership
import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.entities.CohortState

data class GetInternalCohortMembershipResponse(
    val cohortId: String,
    val cohortStatus: CohortState,
    val profileId: String,
    val role: CohortRole,
) {

    companion object {

        fun fromMembership(membership: CohortMembership): GetInternalCohortMembershipResponse {
            return GetInternalCohortMembershipResponse(
                cohortId = membership.cohort.id!!,
                cohortStatus = membership.cohort.state,
                profileId = membership.profileId,
                role = membership.role
            )
        }
    }
}
