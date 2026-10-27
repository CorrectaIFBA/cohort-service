package io.correcta.boilerplate.clients

import io.correcta.boilerplate.config.IdentityClientConfig
import io.correcta.boilerplate.models.request.clients.ProfileBatchLookupRequest
import io.correcta.boilerplate.models.response.clients.InstitutionalProfileResponse
import io.correcta.boilerplate.models.response.clients.ProfileBatchLookupResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "identity-service",
    url = "\${clients.identity-service.url}",
    configuration = [IdentityClientConfig::class]
)
interface IdentityClient {

    @GetMapping("/api/v1/profiles/{profileId}")
    fun getProfile(
        @PathVariable profileId: String
    ): InstitutionalProfileResponse

    @GetMapping("/api/v1/profiles/search")
    fun searchProfiles(
        @RequestParam("q") search: String,
        @RequestParam("type", required = false) type: String?,
        @RequestParam("size") size: Int
    ): List<InstitutionalProfileResponse>

    @PostMapping("/api/v1/profiles/batch-lookup")
    fun getProfiles(
        @RequestBody request: ProfileBatchLookupRequest
    ): ProfileBatchLookupResponse

}
