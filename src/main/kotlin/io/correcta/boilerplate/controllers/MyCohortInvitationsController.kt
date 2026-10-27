package io.correcta.boilerplate.controllers

import io.correcta.boilerplate.models.response.GetMyCohortInvitationResponse
import io.correcta.boilerplate.models.response.error.ErrorResponse
import io.correcta.boilerplate.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/v1/cohort-invitations")
@Tag(name = "My cohort invitations", description = "Invitations addressed to the authenticated profile.")
interface MyCohortInvitationsController {

    @Operation(
        summary = "List my pending invitations",
        description = "Returns the pending, unexpired invitations of the authenticated profile to active cohorts, newest first. Accept one with POST /api/v1/cohorts/{cohortId}/invitations/accept.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Pending invitations, possibly none."),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
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
    fun getMyInvitations(): List<GetMyCohortInvitationResponse>
}
