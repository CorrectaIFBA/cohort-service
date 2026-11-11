package io.correcta.cohort.controllers.impl

import io.correcta.cohort.controllers.CohortsController
import io.correcta.cohort.models.request.ChangeCohortStatusRequest
import io.correcta.cohort.models.request.CohortFilterRequest
import io.correcta.cohort.models.request.CreateCohortRequest
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.request.UpdateCohortRequest
import io.correcta.cohort.models.response.GetCohortResponse
import io.correcta.cohort.models.response.PagedResponse
import io.correcta.cohort.service.CohortService
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortsControllerImpl(
  val cohortService: CohortService,
) : CohortsController {

  override fun getCohorts(filter: CohortFilterRequest, pagination: PaginationRequest): PagedResponse<GetCohortResponse> = cohortService.getCohorts(filter, pagination)

  override fun getCohort(cohortId: String, slug: String): GetCohortResponse = cohortService.getCohort(cohortId, slug)

  override fun getCohortByPublicCode(publicCode: String): GetCohortResponse = cohortService.getCohortByPublicCode(publicCode)

  override fun createCohort(body: CreateCohortRequest): GetCohortResponse = cohortService.createCohort(body)

  override fun updateCohort(cohortId: String, body: UpdateCohortRequest): GetCohortResponse = cohortService.updateCohort(cohortId, body)

  override fun changeCohortStatus(cohortId: String, body: ChangeCohortStatusRequest): GetCohortResponse = cohortService.changeCohortStatus(cohortId, body)

  override fun deleteCohort(cohortId: String) {
    cohortService.deleteCohort(cohortId)
  }
}