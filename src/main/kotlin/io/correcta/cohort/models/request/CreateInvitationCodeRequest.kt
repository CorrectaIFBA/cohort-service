package io.correcta.cohort.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import java.time.Instant

/** Body of the invitation code generation endpoint. */
data class CreateInvitationCodeRequest(
  @field:Schema(description = "Optional expiration (UTC ISO-8601). Defaults to the maximum lifetime.")
  @field:Future(message = "A data de expiração deve estar no futuro")
  val expiresAt: Instant? = null,
)