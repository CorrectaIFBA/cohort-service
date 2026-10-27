package io.correcta.boilerplate.controllers

import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.response.GetCohortMemberResponse
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/api/v1/cohorts/{cohortId}")
@Tag(name = "Cohort members", description = "Current cohort members and membership removal.")
interface CohortMembersController {

    @Operation(
        summary = "List members",
        description = "Returns a page of current members enriched with identity details.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Members successfully retrieved."),
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
    @GetMapping("/members")
    @IsLoggedUser
    fun getMembers(
        @PathVariable cohortId: String,
        @Valid @ParameterObject pagination: PaginationRequest
    ): PagedResponse<GetCohortMemberResponse>

    @Operation(
        summary = "List faculty",
        description = "Returns a page of current members with the FACULTY cohort role.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Faculty successfully retrieved."),
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
    @GetMapping("/faculty")
    @IsLoggedUser
    fun getFaculty(
        @PathVariable cohortId: String,
        @Valid @ParameterObject pagination: PaginationRequest
    ): PagedResponse<GetCohortMemberResponse>

    @Operation(
        summary = "Remove member",
        description = "Ends a current membership while keeping its history. The cohort owner cannot be removed.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Member successfully removed."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only cohort faculty can remove members.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort or member not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not active or target is the cohort owner.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @DeleteMapping("/members/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsLoggedUser
    fun removeMember(@PathVariable cohortId: String, @PathVariable profileId: String)

}
