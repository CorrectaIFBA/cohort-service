package io.correcta.boilerplate.models.response

import io.correcta.boilerplate.entities.Cohort
import io.correcta.boilerplate.entities.CohortState

data class GetCohortResponse(
    val id: String,
    val ownerProfileId: String,
    val name: String,
    val slug: String,
    val status: CohortState,
    val description: String?,
    val createdAt: String,
) {

    companion object {

        fun fromCohort(cohort: Cohort): GetCohortResponse {
            return GetCohortResponse(
                id = cohort.id!!,
                ownerProfileId = cohort.ownerProfileId,
                name = cohort.title,
                slug = cohort.slug,
                status = cohort.state,
                description = cohort.description,
                createdAt = cohort.createdAt!!.toString()
            )
        }
    }
}
