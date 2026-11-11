package io.correcta.cohort.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Maps to `503 Service Unavailable`. The message goes to the client. */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class ServiceUnavailableException(override val message: String = "Um serviço necessário está indisponível no momento. Tente novamente mais tarde.") : RuntimeException(message)