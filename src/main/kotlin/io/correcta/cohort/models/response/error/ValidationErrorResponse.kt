package io.correcta.cohort.models.response.error

data class ValidationErrorResponse(
  val message: String,
  val errors: Map<String, String>,
)