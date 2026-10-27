package io.correcta.boilerplate.controllers.impl

import io.correcta.boilerplate.controllers.CohortsController
import io.correcta.boilerplate.models.request.ChangeCohortStatusRequest
import io.correcta.boilerplate.models.request.CohortFilterRequest
import io.correcta.boilerplate.models.request.CreateCohortRequest
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.request.UpdateCohortRequest
import io.correcta.boilerplate.models.response.GetCohortResponse
import io.correcta.boilerplate.models.response.PagedResponse
import io.correcta.boilerplate.service.CohortService
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortsControllerImpl(
    val cohortService: CohortService
) : CohortsController {

    override fun getCohorts(filter: CohortFilterRequest, pagination: PaginationRequest): PagedResponse<GetCohortResponse> {
        return cohortService.getCohorts(filter, pagination)
    }

    override fun getCohort(cohortId: String, slug: String): GetCohortResponse {
        return cohortService.getCohort(cohortId, slug)
    }

    override fun createCohort(body: CreateCohortRequest): GetCohortResponse {
        return cohortService.createCohort(body)
    }

    override fun updateCohort(cohortId: String, body: UpdateCohortRequest): GetCohortResponse {
        return cohortService.updateCohort(cohortId, body)
    }

    override fun changeCohortStatus(cohortId: String, body: ChangeCohortStatusRequest): GetCohortResponse {
        return cohortService.changeCohortStatus(cohortId, body)
    }

    override fun deleteCohort(cohortId: String) {
        cohortService.deleteCohort(cohortId)
    }
}
