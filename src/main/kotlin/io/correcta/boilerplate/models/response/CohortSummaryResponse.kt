package io.correcta.boilerplate.models.response

import io.correcta.boilerplate.entities.Cohort
import io.correcta.boilerplate.entities.CohortState

data class CohortSummaryResponse(
    val id: String,
    val name: String,
    val slug: String,
    val status: CohortState,
) {

    companion object {

        fun fromCohort(cohort: Cohort): CohortSummaryResponse {
            return CohortSummaryResponse(
                id = cohort.id!!,
                name = cohort.title,
                slug = cohort.slug,
                status = cohort.state
            )
        }
    }
}
