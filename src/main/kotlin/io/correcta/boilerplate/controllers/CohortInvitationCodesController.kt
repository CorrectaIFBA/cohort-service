package io.correcta.boilerplate.controllers

import io.correcta.boilerplate.models.request.CreateInvitationCodeRequest
import io.correcta.boilerplate.models.response.GetCohortMembershipResponse
import io.correcta.boilerplate.models.response.GetInvitationCodeResponse
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
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/api/v1")
@Tag(name = "Cohort invitation codes", description = "Reusable codes that let eligible profiles join an active cohort.")
interface CohortInvitationCodesController {

    @Operation(
        summary = "Generate invitation code",
        description = "Generates a new invitation code and revokes the current one.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Invitation code successfully generated."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid expiration.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can manage invitation codes.",
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
            )
        ]
    )
    @PostMapping("/cohorts/{cohortId}/invitation-code")
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    fun generateInvitationCode(
        @PathVariable cohortId: String,
        @Valid @RequestBody(required = false) body: CreateInvitationCodeRequest?
    ): GetInvitationCodeResponse

    @Operation(
        summary = "Get invitation code",
        description = "Returns the current valid invitation code until it expires or is revoked.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Invitation code successfully retrieved."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can manage invitation codes.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort or valid invitation code not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not active.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/cohorts/{cohortId}/invitation-code")
    @IsLoggedUser
    fun getInvitationCode(@PathVariable cohortId: String): GetInvitationCodeResponse

    @Operation(
        summary = "Revoke invitation code",
        description = "Revokes the current valid invitation code. Later join attempts with it fail.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Invitation code successfully revoked."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can manage invitation codes.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort or valid invitation code not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not active.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @DeleteMapping("/cohorts/{cohortId}/invitation-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsLoggedUser
    fun revokeInvitationCode(@PathVariable cohortId: String)

    @Operation(
        summary = "Join cohort with invitation code",
        description = "Adds the authenticated profile to the cohort. The role follows the profile type.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Membership successfully created."),
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
                description = "Invalid, expired or revoked invitation code.",
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
    @PostMapping("/cohort-invitation-codes/{code}/join")
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    fun joinCohort(@PathVariable code: String): GetCohortMembershipResponse

}
