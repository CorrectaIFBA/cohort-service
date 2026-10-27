package io.correcta.boilerplate.mailer.requests

data class SendTransactionalMailRequest(
    val id: String,
    val to: List<RecipientRef>,
    val subject: String,
    val template: TemplateRef,
    val data: Map<String, Any?>,
)
