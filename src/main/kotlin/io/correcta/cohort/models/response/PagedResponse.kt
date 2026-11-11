package io.correcta.cohort.models.response

/** One page of a listing with its sort. */
data class PagedResponse<T>(
  val content: List<T>,
  val page: Int,
  val size: Int,
  val totalElements: Long,
  val totalPages: Int,
  val sortBy: String,
  val sortDirection: String,
)