package io.correcta.cohort.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Maps to `409 Conflict`. The message goes to the client. */
@ResponseStatus(HttpStatus.CONFLICT)
class ConflictException(override val message: String = "A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.") : RuntimeException(message)