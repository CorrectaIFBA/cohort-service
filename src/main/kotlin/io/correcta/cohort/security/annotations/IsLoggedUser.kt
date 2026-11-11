package io.correcta.cohort.security.annotations

/** Requires the `X-User-Id` header. Requests without it get `401`. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsLoggedUser