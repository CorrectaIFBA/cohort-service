package io.correcta.boilerplate.controllers.impl

import io.correcta.boilerplate.controllers.CohortInvitationCodesController
import io.correcta.boilerplate.models.request.CreateInvitationCodeRequest
import io.correcta.boilerplate.models.response.GetCohortMembershipResponse
import io.correcta.boilerplate.models.response.GetInvitationCodeResponse
import io.correcta.boilerplate.service.CohortInvitationCodeService
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortInvitationCodesControllerImpl(
    val cohortInvitationCodeService: CohortInvitationCodeService
) : CohortInvitationCodesController {

    override fun generateInvitationCode(cohortId: String, body: CreateInvitationCodeRequest?): GetInvitationCodeResponse {
        return cohortInvitationCodeService.generateInvitationCode(cohortId, body ?: CreateInvitationCodeRequest())
    }

    override fun getInvitationCode(cohortId: String): GetInvitationCodeResponse {
        return cohortInvitationCodeService.getInvitationCode(cohortId)
    }

    override fun revokeInvitationCode(cohortId: String) {
        cohortInvitationCodeService.revokeInvitationCode(cohortId)
    }

    override fun joinCohort(code: String): GetCohortMembershipResponse {
        return cohortInvitationCodeService.joinCohort(code)
    }
}
