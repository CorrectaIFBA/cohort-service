package io.correcta.boilerplate.models.request.clients

data class ProfileBatchLookupRequest(
    val profileIds: List<String>
)
