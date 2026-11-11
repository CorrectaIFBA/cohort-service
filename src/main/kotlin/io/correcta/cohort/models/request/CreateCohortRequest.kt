package io.correcta.cohort.models.request

import io.correcta.cohort.entities.CohortState
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Body of the cohort creation endpoint. */
data class CreateCohortRequest(
  @field:Schema(description = "Cohort name.", example = "Introdução à Ciência da Computação")
  @field:NotBlank(message = "O nome é obrigatório")
  @field:Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
  val name: String,

  @field:Schema(description = "Human-readable unique identifier. Generated from the name when omitted.", example = "intro-ciencia-computacao")
  @field:Size(max = 160, message = "O slug deve ter no máximo 160 caracteres")
  @field:Pattern(regexp = SLUG_PATTERN, message = "O slug deve conter apenas letras minúsculas, números e hífens")
  val slug: String? = null,

  @field:Schema(description = "Optional cohort description.")
  @field:Size(max = 1024, message = "A descrição deve ter no máximo 1024 caracteres")
  val description: String? = null,

  @field:Schema(description = "Initial status. Only DRAFT is accepted.", defaultValue = "DRAFT")
  val status: CohortState = CohortState.DRAFT,
) {
  companion object {
    const val SLUG_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$"
  }
}