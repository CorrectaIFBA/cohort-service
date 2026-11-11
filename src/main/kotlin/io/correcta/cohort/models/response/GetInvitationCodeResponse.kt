package io.correcta.cohort.models.response

import io.correcta.cohort.entities.CohortInvitationCode
import io.correcta.cohort.entities.CohortInvitationCodeState

/** Invitation code with its effective state. */
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

    fun fromInvitationCode(invitationCode: CohortInvitationCode): GetInvitationCodeResponse = GetInvitationCodeResponse(
      id = invitationCode.id!!,
      code = invitationCode.code,
      state = invitationCode.effectiveState(),
      createdByProfileId = invitationCode.createdByProfileId,
      createdAt = invitationCode.createdAt!!.toString(),
      expiresAt = invitationCode.expiresAt.toString(),
      cohort = CohortSummaryResponse.fromCohort(invitationCode.cohort),
    )
  }
}