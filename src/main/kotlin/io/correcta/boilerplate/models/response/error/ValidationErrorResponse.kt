package io.correcta.boilerplate.models.response.error

data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, String>
)