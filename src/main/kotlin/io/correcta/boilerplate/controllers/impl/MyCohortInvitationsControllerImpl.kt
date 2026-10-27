package io.correcta.boilerplate.controllers.impl

import io.correcta.boilerplate.controllers.MyCohortInvitationsController
import io.correcta.boilerplate.models.response.GetMyCohortInvitationResponse
import io.correcta.boilerplate.service.CohortInvitationService
import org.springframework.web.bind.annotation.RestController

@RestController
class MyCohortInvitationsControllerImpl(
    val cohortInvitationService: CohortInvitationService
) : MyCohortInvitationsController {

    override fun getMyInvitations(): List<GetMyCohortInvitationResponse> {
        return cohortInvitationService.getMyInvitations()
    }
}
