package io.correcta.cohort.models.request

import io.correcta.cohort.entities.CohortState
import io.swagger.v3.oas.annotations.media.Schema

data class ChangeCohortStatusRequest(
  @field:Schema(description = "Target status.", example = "ACTIVE")
  val status: CohortState,
)