package io.correcta.boilerplate.models.response

import io.correcta.boilerplate.entities.CohortMembershipInvitation
import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.entities.MembershipInvitationState
import io.correcta.boilerplate.models.response.clients.InstitutionalProfileResponse

data class GetCohortInvitationResponse(
    val id: String,
    val cohortId: String,
    val profileId: String,
    val displayName: String?,
    val login: String?,
    val email: String?,
    val profileType: String?,
    val role: CohortRole,
    val status: MembershipInvitationState,
    val invitedAt: String,
    val expiresAt: String?,
) {

    companion object {

        fun fromInvitation(invitation: CohortMembershipInvitation, profile: InstitutionalProfileResponse?): GetCohortInvitationResponse {
            return GetCohortInvitationResponse(
                id = invitation.id!!,
                cohortId = invitation.cohort.id!!,
                profileId = invitation.profileId,
                displayName = profile?.displayName,
                login = profile?.login,
                email = profile?.email,
                profileType = profile?.type,
                role = invitation.role,
                status = invitation.effectiveState(),
                invitedAt = invitation.invitedAt!!.toString(),
                expiresAt = invitation.expiresAt?.toString()
            )
        }
    }
}
