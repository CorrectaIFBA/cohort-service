package io.correcta.cohort.security.annotations

/** Requires the caller profile type to be [profileType]. Other callers get `403`. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsProfileType(val profileType: String)