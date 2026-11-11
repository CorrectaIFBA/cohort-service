package io.correcta.cohort.models.response

import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse
import io.swagger.v3.oas.annotations.media.Schema

data class GetInvitationCandidateResponse(
  @field:Schema(description = "Institutional profile ID.")
  val profileId: String,

  @field:Schema(description = "Display name of the profile owner.")
  val displayName: String,

  @field:Schema(description = "Login of the profile.")
  val login: String?,

  @field:Schema(description = "Institutional e-mail of the profile.")
  val email: String,

  @field:Schema(description = "Profile type, which is also the cohort role it would get.", example = "STUDENT")
  val profileType: String,

  @field:Schema(description = "Whether the profile can be invited, is already a member or already has a pending invitation.")
  val status: InvitationCandidateStatus,
) {
  companion object {
    fun fromProfile(profile: InstitutionalProfileResponse, status: InvitationCandidateStatus): GetInvitationCandidateResponse = GetInvitationCandidateResponse(
      profileId = profile.profileId,
      displayName = profile.displayName,
      login = profile.login,
      email = profile.email,
      profileType = profile.type,
      status = status,
    )
  }
}

enum class InvitationCandidateStatus {
  AVAILABLE,
  MEMBER,
  INVITED,
}