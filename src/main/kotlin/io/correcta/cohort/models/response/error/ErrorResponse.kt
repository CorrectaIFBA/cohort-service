package io.correcta.cohort.models.response.error

/** Error body returned by every failed request. */
data class ErrorResponse(
  val message: String,
)