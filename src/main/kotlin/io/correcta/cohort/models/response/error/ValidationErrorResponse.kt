package io.correcta.cohort.models.response.error

/** Error body of a failed bean validation, with one message per field. */
data class ValidationErrorResponse(
  val message: String,
  val errors: Map<String, String>,
)