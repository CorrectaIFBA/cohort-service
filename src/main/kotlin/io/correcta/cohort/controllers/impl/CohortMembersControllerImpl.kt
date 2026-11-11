package io.correcta.cohort.controllers.impl

import io.correcta.cohort.controllers.CohortMembersController
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.response.GetCohortMemberResponse
import io.correcta.cohort.models.response.PagedResponse
import io.correcta.cohort.service.CohortMembershipService
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortMembersControllerImpl(
  val cohortMembershipService: CohortMembershipService,
) : CohortMembersController {

  override fun getMembers(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> = cohortMembershipService.getMembers(cohortId, pagination)

  override fun getFaculty(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> = cohortMembershipService.getFaculty(cohortId, pagination)

  override fun removeMember(cohortId: String, profileId: String) {
    cohortMembershipService.removeMember(cohortId, profileId)
  }
}