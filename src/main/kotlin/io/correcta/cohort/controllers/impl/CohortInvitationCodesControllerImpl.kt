package io.correcta.cohort.controllers.impl

import io.correcta.cohort.controllers.CohortInvitationCodesController
import io.correcta.cohort.models.request.CreateInvitationCodeRequest
import io.correcta.cohort.models.response.GetCohortMembershipResponse
import io.correcta.cohort.models.response.GetInvitationCodeResponse
import io.correcta.cohort.service.CohortInvitationCodeService
import org.springframework.web.bind.annotation.RestController

/** Delegates [CohortInvitationCodesController] calls to the service layer. */
@RestController
class CohortInvitationCodesControllerImpl(
  val cohortInvitationCodeService: CohortInvitationCodeService,
) : CohortInvitationCodesController {

  override fun generateInvitationCode(cohortId: String, body: CreateInvitationCodeRequest?): GetInvitationCodeResponse = cohortInvitationCodeService.generateInvitationCode(cohortId, body ?: CreateInvitationCodeRequest())

  override fun getInvitationCode(cohortId: String): GetInvitationCodeResponse = cohortInvitationCodeService.getInvitationCode(cohortId)

  override fun revokeInvitationCode(cohortId: String) {
    cohortInvitationCodeService.revokeInvitationCode(cohortId)
  }

  override fun joinCohort(code: String): GetCohortMembershipResponse = cohortInvitationCodeService.joinCohort(code)
}