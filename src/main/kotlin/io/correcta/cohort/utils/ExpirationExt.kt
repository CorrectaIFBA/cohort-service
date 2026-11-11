package io.correcta.cohort.utils

import io.correcta.cohort.exceptions.BadRequestException
import java.time.Duration
import java.time.Instant

/** Expiration helpers. */
object ExpirationExt {

  /** Returns this expiration, or now plus [maxLifetime] when null. Rejects expirations past that limit. */
  fun Instant?.withinLifetime(maxLifetime: Duration): Instant {
    val limit = Instant.now().plus(maxLifetime)

    if (this == null) return limit
    if (isAfter(limit)) {
      throw BadRequestException("A data de expiração deve ser de no máximo ${maxLifetime.toDays()} dias a partir de agora.")
    }

    return this
  }
}