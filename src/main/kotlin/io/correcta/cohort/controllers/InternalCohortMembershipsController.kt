package io.correcta.cohort.controllers

import io.correcta.cohort.models.response.GetInternalCohortMembershipResponse
import io.correcta.cohort.models.response.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

/** Internal endpoints for other services. Do not expose them through the gateway. Endpoint details are in the OpenAPI annotations. */
@RequestMapping("/api/v1/internal/cohorts/{cohortId}")
@Tag(
  name = "Internal Cohort Memberships",
  description = "Service-to-service lookups called directly inside the cluster, without the gateway or authentication.",
)
interface InternalCohortMembershipsController {

  @Operation(
    summary = "Get current membership",
    description = "Returns the current membership of a profile in a cohort, with the cohort status.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Membership successfully retrieved."),
      ApiResponse(
        responseCode = "404",
        description = "Cohort not found or profile is not a current member.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping("/members/{profileId}")
  fun getCurrentMembership(
    @PathVariable cohortId: String,
    @PathVariable profileId: String,
  ): GetInternalCohortMembershipResponse
}