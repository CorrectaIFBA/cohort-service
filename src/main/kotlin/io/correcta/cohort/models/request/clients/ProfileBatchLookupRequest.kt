package io.correcta.cohort.models.request.clients

/** Body of the identity-service batch profile lookup. */
data class ProfileBatchLookupRequest(
  val profileIds: List<String>,
)