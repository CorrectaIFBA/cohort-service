package io.correcta.boilerplate.utils

import io.correcta.boilerplate.exceptions.BadRequestException
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.response.PagedResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

object PaginationExt {

    fun PaginationRequest.toPageRequest(allowedSortFields: Set<String>, defaultSortField: String): PageRequest {
        val field = sortBy ?: defaultSortField

        if (field !in allowedSortFields) {
            throw BadRequestException(
                "Não é possível ordenar por '$field'. Campos permitidos: ${allowedSortFields.joinToString(", ")}"
            )
        }

        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), field))
    }

    fun <E, T> Page<E>.toPagedResponse(mapper: (E) -> T): PagedResponse<T> {
        val order = sort.first()

        return PagedResponse(
            content = content.map(mapper),
            page = number,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            sortBy = order.property,
            sortDirection = order.direction.name.lowercase()
        )
    }

}
