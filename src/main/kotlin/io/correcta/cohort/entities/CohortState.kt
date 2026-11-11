package io.correcta.cohort.entities

enum class CohortState {
  DRAFT,
  ACTIVE,
  ARCHIVED,
  ;

  fun canTransitionTo(target: CohortState): Boolean = when (this) {
    DRAFT -> target == ACTIVE || target == ARCHIVED
    ACTIVE -> target == ARCHIVED
    ARCHIVED -> target == ACTIVE
  }
}