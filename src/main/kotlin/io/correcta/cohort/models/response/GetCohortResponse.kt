package io.correcta.cohort.models.response

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortState
import io.swagger.v3.oas.annotations.media.Schema

/** Full cohort view. */
data class GetCohortResponse(
  val id: String,
  val ownerProfileId: String,
  val name: String,
  val slug: String,
  @field:Schema(description = "Short code that never changes; the cohort's URL is \"{slug}-{publicCode}\".")
  val publicCode: String,
  val status: CohortState,
  val description: String?,
  val createdAt: String,
) {

  companion object {

    fun fromCohort(cohort: Cohort): GetCohortResponse = GetCohortResponse(
      id = cohort.id!!,
      ownerProfileId = cohort.ownerProfileId,
      name = cohort.title,
      slug = cohort.slug,
      publicCode = cohort.publicCode,
      status = cohort.state,
      description = cohort.description,
      createdAt = cohort.createdAt!!.toString(),
    )
  }
}