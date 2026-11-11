package io.correcta.cohort.clients

import io.correcta.cohort.config.IdentityClientConfig
import io.correcta.cohort.models.request.clients.ProfileBatchLookupRequest
import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse
import io.correcta.cohort.models.response.clients.ProfileBatchLookupResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

/** Feign client for the identity-service profile API. Every call carries the internal token. */
@FeignClient(
  name = "identity-service",
  url = "\${clients.identity-service.url}",
  configuration = [IdentityClientConfig::class],
)
interface IdentityClient {

  /** Returns one institutional profile by ID. */
  @GetMapping("/api/v1/profiles/{profileId}")
  fun getProfile(
    @PathVariable profileId: String,
  ): InstitutionalProfileResponse

  /** Searches profiles by name, login or e-mail, optionally restricted to a profile [type]. */
  @GetMapping("/api/v1/profiles/search")
  fun searchProfiles(
    @RequestParam("q") search: String,
    @RequestParam("type", required = false) type: String?,
    @RequestParam("size") size: Int,
  ): List<InstitutionalProfileResponse>

  /** Returns the profiles found for the given IDs. */
  @PostMapping("/api/v1/profiles/batch-lookup")
  fun getProfiles(
    @RequestBody request: ProfileBatchLookupRequest,
  ): ProfileBatchLookupResponse
}