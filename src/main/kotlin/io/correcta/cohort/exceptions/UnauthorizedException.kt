package io.correcta.cohort.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Maps to `401 Unauthorized`. The message goes to the client. */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(override val message: String = "É necessário estar autenticado para acessar este recurso.") : RuntimeException(message)