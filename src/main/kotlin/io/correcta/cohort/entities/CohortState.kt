package io.correcta.cohort.entities

/** Lifecycle states of a [Cohort]. */
enum class CohortState {
  DRAFT,
  ACTIVE,
  ARCHIVED,
  ;

  /** Tells if the cohort can move from this state to [target]. */
  fun canTransitionTo(target: CohortState): Boolean = when (this) {
    DRAFT -> target == ACTIVE || target == ARCHIVED
    ACTIVE -> target == ARCHIVED
    ARCHIVED -> target == ACTIVE
  }
}