package io.correcta.cohort.utils

import java.text.Normalizer

/** Slug helpers. */
object SlugExt {

  private const val MAX_BASE_LENGTH = 150
  private const val FALLBACK_SLUG = "turma"

  /** Converts to a lowercase ASCII slug without accents. Returns `turma` when nothing remains. */
  fun String.toSlug(): String {
    val slug = Normalizer.normalize(this, Normalizer.Form.NFD)
      .replace(Regex("\\p{M}+"), "")
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), "-")
      .take(MAX_BASE_LENGTH)
      .trim('-')

    return slug.ifEmpty { FALLBACK_SLUG }
  }
}