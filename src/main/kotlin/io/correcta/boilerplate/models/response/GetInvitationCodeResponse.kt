package io.correcta.boilerplate.models.response

import io.correcta.boilerplate.entities.CohortInvitationCode
import io.correcta.boilerplate.entities.CohortInvitationCodeState

data class GetInvitationCodeResponse(
    val id: String,
    val code: String,
    val state: CohortInvitationCodeState,
    val createdByProfileId: String,
    val createdAt: String,
    val expiresAt: String,
    val cohort: CohortSummaryResponse,
) {

    companion object {

        fun fromInvitationCode(invitationCode: CohortInvitationCode): GetInvitationCodeResponse {
            return GetInvitationCodeResponse(
                id = invitationCode.id!!,
                code = invitationCode.code,
                state = invitationCode.effectiveState(),
                createdByProfileId = invitationCode.createdByProfileId,
                createdAt = invitationCode.createdAt!!.toString(),
                expiresAt = invitationCode.expiresAt.toString(),
                cohort = CohortSummaryResponse.fromCohort(invitationCode.cohort)
            )
        }
    }
}
