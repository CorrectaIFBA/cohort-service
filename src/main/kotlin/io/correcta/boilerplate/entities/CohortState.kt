package io.correcta.boilerplate.entities

enum class CohortState {
    DRAFT,
    ACTIVE,
    ARCHIVED;

    fun canTransitionTo(target: CohortState): Boolean {
        return when (this) {
            DRAFT -> target == ACTIVE || target == ARCHIVED
            ACTIVE -> target == ARCHIVED
            ARCHIVED -> target == ACTIVE
        }
    }
}
