package io.correcta.cohort.models.request

import io.correcta.cohort.entities.CohortState
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class CohortFilterRequest(
  @field:Schema(description = "Cohort status.")
  val status: CohortState? = null,

  @field:Schema(description = "Inclusive lower creation timestamp (UTC ISO-8601).")
  val createdAfter: Instant? = null,

  @field:Schema(description = "Inclusive upper creation timestamp (UTC ISO-8601).")
  val createdBefore: Instant? = null,

  @field:Schema(description = "Restrict to cohorts owned by the authenticated profile.", defaultValue = "false")
  val mine: Boolean = false,

  @field:Schema(description = "Part of the cohort name or slug.")
  val search: String? = null,

  @field:Schema(description = "Restrict to cohorts the given profile is a current member of.")
  val memberProfileId: String? = null,
)