package io.correcta.cohort.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Maps to `400 Bad Request`. The message goes to the client. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(override val message: String = "A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios.") : RuntimeException(message)