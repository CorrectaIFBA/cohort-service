package io.correcta.cohort.mailer.requests

/** Location of the e-mail template in object storage. */
data class TemplateRef(val bucket: String, val key: String)