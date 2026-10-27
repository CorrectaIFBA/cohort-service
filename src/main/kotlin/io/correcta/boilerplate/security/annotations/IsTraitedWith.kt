package io.correcta.boilerplate.security.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsTraitedWith(val trait: String)
