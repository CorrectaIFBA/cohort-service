package io.correcta.cohort.repositories.specifications

/** Helpers for SQL `LIKE` predicates. */
object SpecificationExt {

  const val LIKE_ESCAPE_CHAR = '\\'

  /** Builds a lowercase `%term%` pattern with `\`, `%` and `_` escaped by [LIKE_ESCAPE_CHAR]. */
  fun String.toContainsPattern(): String {
    val escaped = trim()
      .lowercase()
      .replace("\\", "\\\\")
      .replace("%", "\\%")
      .replace("_", "\\_")

    return "%$escaped%"
  }
}