package io.correcta.boilerplate.controllers.impl

import io.correcta.boilerplate.controllers.InternalCohortMembershipsController
import io.correcta.boilerplate.models.response.GetInternalCohortMembershipResponse
import io.correcta.boilerplate.service.CohortMembershipService
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalCohortMembershipsControllerImpl(
    val cohortMembershipService: CohortMembershipService
) : InternalCohortMembershipsController {

    override fun getCurrentMembership(cohortId: String, profileId: String): GetInternalCohortMembershipResponse {
        return cohortMembershipService.getCurrentMembership(cohortId, profileId)
    }
}
