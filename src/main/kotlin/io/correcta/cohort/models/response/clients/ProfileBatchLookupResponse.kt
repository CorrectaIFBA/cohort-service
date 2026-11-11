package io.correcta.cohort.models.response.clients

data class ProfileBatchLookupResponse(
  val profiles: List<InstitutionalProfileResponse>,
  val notFoundProfileIds: List<String>,
)