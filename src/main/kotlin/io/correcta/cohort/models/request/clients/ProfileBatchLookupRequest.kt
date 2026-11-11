package io.correcta.cohort.models.request.clients

data class ProfileBatchLookupRequest(
  val profileIds: List<String>,
)