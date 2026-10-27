package io.correcta.boilerplate.models.response.clients

data class ProfileBatchLookupResponse(
    val profiles: List<InstitutionalProfileResponse>,
    val notFoundProfileIds: List<String>
)
