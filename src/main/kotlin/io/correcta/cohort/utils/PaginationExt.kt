package io.correcta.cohort.utils

import io.correcta.cohort.exceptions.BadRequestException
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.response.PagedResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/** Conversions between pagination models and Spring Data. */
object PaginationExt {

  /** Builds a [PageRequest]. Rejects sort fields outside [allowedSortFields]. */
  fun PaginationRequest.toPageRequest(allowedSortFields: Set<String>, defaultSortField: String): PageRequest {
    val field = sortBy ?: defaultSortField

    if (field !in allowedSortFields) {
      throw BadRequestException(
        "Não é possível ordenar por '$field'. Campos permitidos: ${allowedSortFields.joinToString(", ")}",
      )
    }

    return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), field))
  }

  /** Maps the page content with [mapper]. The page must be sorted by one field. */
  fun <E, T> Page<E>.toPagedResponse(mapper: (E) -> T): PagedResponse<T> {
    val order = sort.first()

    return PagedResponse(
      content = content.map(mapper),
      page = number,
      size = size,
      totalElements = totalElements,
      totalPages = totalPages,
      sortBy = order.property,
      sortDirection = order.direction.name.lowercase(),
    )
  }
}