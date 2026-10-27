package io.correcta.boilerplate.models.response

import io.correcta.boilerplate.entities.CohortMembership
import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.models.response.clients.InstitutionalProfileResponse

data class GetCohortMemberResponse(
    val profileId: String,
    val displayName: String?,
    val profileType: String?,
    val role: CohortRole,
    val joinedAt: String,
) {

    companion object {

        fun fromMembership(membership: CohortMembership, profile: InstitutionalProfileResponse?): GetCohortMemberResponse {
            return GetCohortMemberResponse(
                profileId = membership.profileId,
                displayName = profile?.displayName,
                profileType = profile?.type,
                role = membership.role,
                joinedAt = membership.joinedAt!!.toString()
            )
        }
    }
}
