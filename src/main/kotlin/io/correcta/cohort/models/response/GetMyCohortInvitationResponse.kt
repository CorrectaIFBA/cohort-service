package io.correcta.cohort.models.response

import io.correcta.cohort.entities.CohortMembershipInvitation
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse
import io.swagger.v3.oas.annotations.media.Schema

/** Pending invitation addressed to the authenticated profile. */
data class GetMyCohortInvitationResponse(
  @field:Schema(description = "Invitation ID.")
  val id: String,

  @field:Schema(description = "Cohort the profile is invited to.")
  val cohort: CohortSummaryResponse,

  @field:Schema(description = "Role the profile will have in the cohort.", example = "STUDENT")
  val role: CohortRole,

  @field:Schema(description = "Display name of the faculty member who sent the invitation, when available.")
  val invitedByName: String?,

  @field:Schema(description = "When the invitation was sent.")
  val invitedAt: String,

  @field:Schema(description = "When the invitation expires, or null when it does not.")
  val expiresAt: String?,
) {
  companion object {
    fun fromInvitation(invitation: CohortMembershipInvitation, inviter: InstitutionalProfileResponse?): GetMyCohortInvitationResponse = GetMyCohortInvitationResponse(
      id = invitation.id!!,
      cohort = CohortSummaryResponse.fromCohort(invitation.cohort),
      role = invitation.role,
      invitedByName = inviter?.displayName,
      invitedAt = invitation.invitedAt!!.toString(),
      expiresAt = invitation.expiresAt?.toString(),
    )
  }
}