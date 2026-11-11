package io.correcta.cohort.models.request

import io.correcta.cohort.entities.CohortState
import io.swagger.v3.oas.annotations.media.Schema

/** Body of the cohort status change endpoint. */
data class ChangeCohortStatusRequest(
  @field:Schema(description = "Target status.", example = "ACTIVE")
  val status: CohortState,
)