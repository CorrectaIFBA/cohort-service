package io.correcta.boilerplate.controllers

import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.models.request.CreateCohortInvitationRequest
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.response.GetCohortInvitationResponse
import io.correcta.boilerplate.models.response.GetCohortMembershipResponse
import io.correcta.boilerplate.models.response.GetInvitationCandidateResponse
import io.correcta.boilerplate.models.response.PagedResponse
import io.correcta.boilerplate.models.response.error.ErrorResponse
import io.correcta.boilerplate.models.response.error.ValidationErrorResponse
import io.correcta.boilerplate.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/api/v1/cohorts/{cohortId}/invitations")
@Tag(name = "Cohort invitations", description = "Direct membership invitations targeting a specific profile.")
interface CohortInvitationsController {

    @Operation(
        summary = "Send invitation",
        description = "Invites an eligible profile to the cohort. The role must match the profile type.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Invitation successfully sent."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload or role incompatible with the profile type.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can send invitations.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort or profile not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort not active, profile not eligible, already a member or already invited.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "503",
                description = "Identity-service is unavailable.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    fun sendInvitation(
        @PathVariable cohortId: String,
        @Valid @RequestBody body: CreateCohortInvitationRequest
    ): GetCohortInvitationResponse

    @Operation(
        summary = "List invitations",
        description = "Returns a page of the cohort invitations enriched with identity details.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Invitations successfully retrieved."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid pagination parameters.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can list invitations.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not active.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "503",
                description = "Identity-service is unavailable.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping
    @IsLoggedUser
    fun getInvitations(
        @PathVariable cohortId: String,
        @Valid @ParameterObject pagination: PaginationRequest
    ): PagedResponse<GetCohortInvitationResponse>

    @Operation(
        summary = "Search invitation candidates",
        description = "Searches eligible profiles by name, login or e-mail (at least 2 characters) and marks each as available, already a member or already invited. Returns at most 10 profiles.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Matching profiles, possibly none."),
            ApiResponse(
                responseCode = "400",
                description = "Search term shorter than 2 characters.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can search candidates.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not active.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "503",
                description = "Identity-service is unavailable.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/candidates")
    @IsLoggedUser
    fun searchCandidates(
        @PathVariable cohortId: String,
        @RequestParam("q") search: String,
        @RequestParam("role", required = false) role: CohortRole?
    ): List<GetInvitationCandidateResponse>

    @Operation(
        summary = "Revoke invitation",
        description = "Revokes the pending invitation of a profile.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Invitation successfully revoked."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can revoke invitations.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort or pending invitation not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not active.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsLoggedUser
    fun revokeInvitation(@PathVariable cohortId: String, @PathVariable profileId: String)

    @Operation(
        summary = "Accept invitation",
        description = "Accepts the pending invitation of the authenticated profile. Repeating it after acceptance returns the existing membership.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Membership successfully created."),
            ApiResponse(responseCode = "200", description = "Invitation already accepted; existing membership returned."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Profile is not eligible to join cohorts.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Invitation not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not active or profile is already a member.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "503",
                description = "Identity-service is unavailable.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/accept")
    @IsLoggedUser
    fun acceptInvitation(@PathVariable cohortId: String): ResponseEntity<GetCohortMembershipResponse>

}
