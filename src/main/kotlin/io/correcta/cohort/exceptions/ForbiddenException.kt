package io.correcta.cohort.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Maps to `403 Forbidden`. The message goes to the client. */
@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(override val message: String = "Você não tem permissão para acessar este recurso.") : RuntimeException(message)