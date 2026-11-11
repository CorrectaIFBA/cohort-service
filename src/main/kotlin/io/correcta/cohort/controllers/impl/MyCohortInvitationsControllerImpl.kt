package io.correcta.cohort.controllers.impl

import io.correcta.cohort.controllers.MyCohortInvitationsController
import io.correcta.cohort.models.response.GetMyCohortInvitationResponse
import io.correcta.cohort.service.CohortInvitationService
import org.springframework.web.bind.annotation.RestController

/** Delegates [MyCohortInvitationsController] calls to the service layer. */
@RestController
class MyCohortInvitationsControllerImpl(
  val cohortInvitationService: CohortInvitationService,
) : MyCohortInvitationsController {

  override fun getMyInvitations(): List<GetMyCohortInvitationResponse> = cohortInvitationService.getMyInvitations()
}