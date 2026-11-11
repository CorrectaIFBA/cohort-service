package io.correcta.cohort.models.response.clients

/** Result of the identity-service batch profile lookup. */
data class ProfileBatchLookupResponse(
  val profiles: List<InstitutionalProfileResponse>,
  val notFoundProfileIds: List<String>,
)