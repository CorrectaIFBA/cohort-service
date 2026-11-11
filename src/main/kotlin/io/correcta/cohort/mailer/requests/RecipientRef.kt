package io.correcta.cohort.mailer.requests

/** E-mail recipient. */
data class RecipientRef(val email: String, val name: String? = null)