package io.correcta.cohort.mailer.requests

/** Message consumed by the mailer service. [id] makes the delivery idempotent. */
data class SendTransactionalMailRequest(
  val id: String,
  val to: List<RecipientRef>,
  val subject: String,
  val template: TemplateRef,
  val data: Map<String, Any?>,
)