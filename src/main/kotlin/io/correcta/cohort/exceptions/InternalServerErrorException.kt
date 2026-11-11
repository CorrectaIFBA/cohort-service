package io.correcta.cohort.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Maps to `500 Internal Server Error`. The message goes to the client. */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InternalServerErrorException(override val message: String = "Ocorreu um erro inesperado no servidor.") : RuntimeException(message)