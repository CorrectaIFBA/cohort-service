package io.correcta.boilerplate.controllers.impl

import io.correcta.boilerplate.controllers.CohortInvitationsController
import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.models.request.CreateCohortInvitationRequest
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.response.GetCohortInvitationResponse
import io.correcta.boilerplate.models.response.GetCohortMembershipResponse
import io.correcta.boilerplate.models.response.GetInvitationCandidateResponse
import io.correcta.boilerplate.models.response.PagedResponse
import io.correcta.boilerplate.service.CohortInvitationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortInvitationsControllerImpl(
    val cohortInvitationService: CohortInvitationService
) : CohortInvitationsController {

    override fun sendInvitation(cohortId: String, body: CreateCohortInvitationRequest): GetCohortInvitationResponse {
        return cohortInvitationService.sendInvitation(cohortId, body)
    }

    override fun getInvitations(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortInvitationResponse> {
        return cohortInvitationService.getInvitations(cohortId, pagination)
    }

    override fun searchCandidates(cohortId: String, search: String, role: CohortRole?): List<GetInvitationCandidateResponse> {
        return cohortInvitationService.searchCandidates(cohortId, search, role)
    }

    override fun revokeInvitation(cohortId: String, profileId: String) {
        cohortInvitationService.revokeInvitation(cohortId, profileId)
    }

    override fun acceptInvitation(cohortId: String): ResponseEntity<GetCohortMembershipResponse> {
        val (membership, created) = cohortInvitationService.acceptInvitation(cohortId)
        return ResponseEntity.status(if (created) HttpStatus.CREATED else HttpStatus.OK).body(membership)
    }
}
