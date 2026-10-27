package io.correcta.boilerplate.controllers.impl

import io.correcta.boilerplate.controllers.CohortMembersController
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.response.GetCohortMemberResponse
import io.correcta.boilerplate.models.response.PagedResponse
import io.correcta.boilerplate.service.CohortMembershipService
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortMembersControllerImpl(
    val cohortMembershipService: CohortMembershipService
) : CohortMembersController {

    override fun getMembers(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> {
        return cohortMembershipService.getMembers(cohortId, pagination)
    }

    override fun getFaculty(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> {
        return cohortMembershipService.getFaculty(cohortId, pagination)
    }

    override fun removeMember(cohortId: String, profileId: String) {
        cohortMembershipService.removeMember(cohortId, profileId)
    }
}
