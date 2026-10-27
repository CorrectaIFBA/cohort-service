package io.correcta.boilerplate.controllers

import io.correcta.boilerplate.models.request.ChangeCohortStatusRequest
import io.correcta.boilerplate.models.request.CohortFilterRequest
import io.correcta.boilerplate.models.request.CreateCohortRequest
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.request.UpdateCohortRequest
import io.correcta.boilerplate.models.response.GetCohortResponse
import io.correcta.boilerplate.models.response.PagedResponse
import io.correcta.boilerplate.models.response.error.ErrorResponse
import io.correcta.boilerplate.models.response.error.ValidationErrorResponse
import io.correcta.boilerplate.security.annotations.IsLoggedUser
import io.correcta.boilerplate.security.annotations.IsProfileType
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/api/v1/cohorts")
@Tag(name = "Cohorts", description = "Cohort creation, discovery and lifecycle.")
interface CohortsController {

    @Operation(
        summary = "List cohorts",
        description = "Returns a page of cohorts the authenticated profile owns or is a current member of.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Cohorts successfully retrieved."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid pagination or filter parameters.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping
    @IsLoggedUser
    fun getCohorts(
        @Valid @ParameterObject filter: CohortFilterRequest,
        @Valid @ParameterObject pagination: PaginationRequest
    ): PagedResponse<GetCohortResponse>

    @Operation(
        summary = "Get cohort",
        description = "Returns one cohort by ID and current slug. Only the owner and current members can read it.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Cohort successfully retrieved."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort not found, slug mismatch or not visible to the authenticated profile.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/{cohortId}/{slug}")
    @IsLoggedUser
    fun getCohort(@PathVariable cohortId: String, @PathVariable slug: String): GetCohortResponse

    @Operation(
        summary = "Create cohort",
        description = "Creates a DRAFT cohort owned by the authenticated FACULTY profile, which also becomes a FACULTY member.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Cohort successfully created."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload or initial status other than DRAFT.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only FACULTY profiles can create cohorts.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Slug already in use.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    @IsProfileType("FACULTY")
    fun createCohort(@Valid @RequestBody body: CreateCohortRequest): GetCohortResponse

    @Operation(
        summary = "Update cohort metadata",
        description = "Updates name, slug and description. Only the owner can update it, and archived cohorts are read-only.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Cohort successfully updated."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only the cohort owner can update it.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Slug already in use or cohort archived.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PutMapping("/{cohortId}")
    @IsLoggedUser
    @IsProfileType("FACULTY")
    fun updateCohort(@PathVariable cohortId: String, @Valid @RequestBody body: UpdateCohortRequest): GetCohortResponse

    @Operation(
        summary = "Change cohort status",
        description = "Moves the cohort through DRAFT → ACTIVE → ARCHIVED. Archived cohorts can be reactivated.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Cohort status successfully changed."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only the cohort owner can change its status.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Invalid status transition.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PatchMapping("/{cohortId}/status")
    @IsLoggedUser
    @IsProfileType("FACULTY")
    fun changeCohortStatus(@PathVariable cohortId: String, @Valid @RequestBody body: ChangeCohortStatusRequest): GetCohortResponse

    @Operation(
        summary = "Delete cohort",
        description = "Deletes a DRAFT cohort and its memberships. Only the owner can delete it.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Cohort successfully deleted."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only the cohort owner can delete it.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cohort not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Cohort is not in DRAFT.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @DeleteMapping("/{cohortId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsLoggedUser
    @IsProfileType("FACULTY")
    fun deleteCohort(@PathVariable cohortId: String)

}
