package io.correcta.boilerplate.models.response

import io.correcta.boilerplate.entities.CohortMembership
import io.correcta.boilerplate.entities.CohortRole

data class GetCohortMembershipResponse(
    val cohortId: String,
    val profileId: String,
    val role: CohortRole,
    val joinedAt: String,
) {

    companion object {

        fun fromMembership(membership: CohortMembership): GetCohortMembershipResponse {
            return GetCohortMembershipResponse(
                cohortId = membership.cohort.id!!,
                profileId = membership.profileId,
                role = membership.role,
                joinedAt = membership.joinedAt!!.toString()
            )
        }
    }
}
