package io.correcta.cohort.models.response

import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse

/** Current cohort member. Profile fields are null when identity-service does not return the profile. */
data class GetCohortMemberResponse(
  val profileId: String,
  val displayName: String?,
  val profileType: String?,
  val role: CohortRole,
  val joinedAt: String,
) {

  companion object {

    fun fromMembership(membership: CohortMembership, profile: InstitutionalProfileResponse?): GetCohortMemberResponse = GetCohortMemberResponse(
      profileId = membership.profileId,
      displayName = profile?.displayName,
      profileType = profile?.type,
      role = membership.role,
      joinedAt = membership.joinedAt!!.toString(),
    )
  }
}