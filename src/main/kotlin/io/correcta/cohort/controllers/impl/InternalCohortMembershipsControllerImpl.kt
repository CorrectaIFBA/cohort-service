package io.correcta.cohort.controllers.impl

import io.correcta.cohort.controllers.InternalCohortMembershipsController
import io.correcta.cohort.models.response.GetInternalCohortMembershipResponse
import io.correcta.cohort.service.CohortMembershipService
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalCohortMembershipsControllerImpl(
  val cohortMembershipService: CohortMembershipService,
) : InternalCohortMembershipsController {

  override fun getCurrentMembership(cohortId: String, profileId: String): GetInternalCohortMembershipResponse = cohortMembershipService.getCurrentMembership(cohortId, profileId)
}