package io.correcta.cohort.models.response

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortState
import io.swagger.v3.oas.annotations.media.Schema

data class CohortSummaryResponse(
  val id: String,
  val name: String,
  val slug: String,
  @field:Schema(description = "Short code that never changes; the cohort's URL is \"{slug}-{publicCode}\".")
  val publicCode: String,
  val status: CohortState,
) {

  companion object {

    fun fromCohort(cohort: Cohort): CohortSummaryResponse = CohortSummaryResponse(
      id = cohort.id!!,
      name = cohort.title,
      slug = cohort.slug,
      publicCode = cohort.publicCode,
      status = cohort.state,
    )
  }
}