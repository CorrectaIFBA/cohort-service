package io.correcta.cohort.models.request

import io.correcta.cohort.entities.CohortRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import java.time.Instant

/** Body of the direct invitation endpoint. */
data class CreateCohortInvitationRequest(
  @field:Schema(description = "Identity-service institutional profile ID.")
  @field:NotBlank(message = "O perfil é obrigatório")
  val profileId: String,

  @field:Schema(description = "Cohort role. Must match the profile type.", example = "STUDENT")
  val role: CohortRole,

  @field:Schema(description = "Optional expiration (UTC ISO-8601). Defaults to the maximum lifetime.")
  @field:Future(message = "A data de expiração deve estar no futuro")
  val expiresAt: Instant? = null,
)