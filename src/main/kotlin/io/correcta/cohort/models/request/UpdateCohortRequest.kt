package io.correcta.cohort.models.request

import io.correcta.cohort.models.request.CreateCohortRequest.Companion.SLUG_PATTERN
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Body of the cohort update endpoint. */
data class UpdateCohortRequest(
  @field:Schema(description = "Cohort name.", example = "Introdução à Ciência da Computação — 2026")
  @field:NotBlank(message = "O nome é obrigatório")
  @field:Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
  val name: String,

  @field:Schema(description = "Human-readable unique identifier.", example = "intro-ciencia-computacao-2026")
  @field:NotBlank(message = "O slug é obrigatório")
  @field:Size(max = 160, message = "O slug deve ter no máximo 160 caracteres")
  @field:Pattern(regexp = SLUG_PATTERN, message = "O slug deve conter apenas letras minúsculas, números e hífens")
  val slug: String,

  @field:Schema(description = "Optional cohort description. Null clears it.")
  @field:Size(max = 1024, message = "A descrição deve ter no máximo 1024 caracteres")
  val description: String? = null,
)