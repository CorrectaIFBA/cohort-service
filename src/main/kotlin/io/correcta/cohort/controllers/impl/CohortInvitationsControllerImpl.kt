package io.correcta.cohort.controllers.impl

import io.correcta.cohort.controllers.CohortInvitationsController
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.models.request.CreateCohortInvitationRequest
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.response.GetCohortInvitationResponse
import io.correcta.cohort.models.response.GetCohortMembershipResponse
import io.correcta.cohort.models.response.GetInvitationCandidateResponse
import io.correcta.cohort.models.response.PagedResponse
import io.correcta.cohort.service.CohortInvitationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/** Delegates [CohortInvitationsController] calls to the service layer. */
@RestController
class CohortInvitationsControllerImpl(
  val cohortInvitationService: CohortInvitationService,
) : CohortInvitationsController {

  override fun sendInvitation(cohortId: String, body: CreateCohortInvitationRequest): GetCohortInvitationResponse = cohortInvitationService.sendInvitation(cohortId, body)

  override fun getInvitations(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortInvitationResponse> = cohortInvitationService.getInvitations(cohortId, pagination)

  override fun searchCandidates(cohortId: String, search: String, role: CohortRole?): List<GetInvitationCandidateResponse> = cohortInvitationService.searchCandidates(cohortId, search, role)

  override fun revokeInvitation(cohortId: String, profileId: String) {
    cohortInvitationService.revokeInvitation(cohortId, profileId)
  }

  override fun acceptInvitation(cohortId: String): ResponseEntity<GetCohortMembershipResponse> {
    val (membership, created) = cohortInvitationService.acceptInvitation(cohortId)
    return ResponseEntity.status(if (created) HttpStatus.CREATED else HttpStatus.OK).body(membership)
  }
}