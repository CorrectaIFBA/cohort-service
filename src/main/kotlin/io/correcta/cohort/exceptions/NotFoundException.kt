package io.correcta.cohort.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Maps to `404 Not Found`. The message goes to the client. */
@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(override val message: String = "O recurso solicitado não foi encontrado.") : RuntimeException(message)