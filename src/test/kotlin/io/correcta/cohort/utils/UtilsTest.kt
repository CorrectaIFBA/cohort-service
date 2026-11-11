package io.correcta.cohort.utils

import io.correcta.cohort.exceptions.BadRequestException
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.support.Fixtures
import io.correcta.cohort.utils.ExpirationExt.withinLifetime
import io.correcta.cohort.utils.PaginationExt.toPageRequest
import io.correcta.cohort.utils.PaginationExt.toPagedResponse
import io.correcta.cohort.utils.SlugExt.toSlug
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.Sort
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTest {

  @Test
  fun `defaults the expiration to the maximum lifetime`() {
    val before = Instant.now().plus(Duration.ofDays(30))
    val expiration = null.withinLifetime(Duration.ofDays(30))

    assertTrue(!expiration.isBefore(before))
  }

  @Test
  fun `keeps an expiration inside the lifetime`() {
    val expiration = Fixtures.future()

    assertEquals(expiration, expiration.withinLifetime(Duration.ofDays(30)))
  }

  @Test
  fun `rejects an expiration past the lifetime`() {
    val exception = assertThrows<BadRequestException> {
      Instant.now().plus(Duration.ofDays(31)).withinLifetime(Duration.ofDays(30))
    }

    assertEquals("A data de expiração deve ser de no máximo 30 dias a partir de agora.", exception.message)
  }

  @Test
  fun `builds slugs without accents`() {
    assertEquals("introducao-a-computacao", "  Introdução à Computação!  ".toSlug())
    assertEquals("turma", "!!!".toSlug())
    assertEquals(150, "a".repeat(200).toSlug().length)
  }

  @Test
  fun `uses the default sort field`() {
    val request = PaginationRequest(page = 1, size = 5, sortDirection = "ASC").toPageRequest(setOf("createdAt"), "createdAt")

    assertEquals(1, request.pageNumber)
    assertEquals(5, request.pageSize)
    assertEquals(Sort.Direction.ASC, request.sort.getOrderFor("createdAt")!!.direction)
  }

  @Test
  fun `uses an allowed sort field`() {
    val request = PaginationRequest(sortBy = "slug").toPageRequest(setOf("createdAt", "slug"), "createdAt")

    assertEquals(Sort.Direction.DESC, request.sort.getOrderFor("slug")!!.direction)
  }

  @Test
  fun `rejects other sort fields`() {
    val exception = assertThrows<BadRequestException> {
      PaginationRequest(sortBy = "title").toPageRequest(setOf("createdAt", "slug"), "createdAt")
    }

    assertEquals("Não é possível ordenar por 'title'. Campos permitidos: createdAt, slug", exception.message)
  }

  @Test
  fun `maps a page`() {
    val response = Fixtures.pageOf(1, 2, sortField = "slug").toPagedResponse { it * 10 }

    assertEquals(listOf(10, 20), response.content)
    assertEquals(0, response.page)
    assertEquals(20, response.size)
    assertEquals(2, response.totalElements)
    assertEquals(1, response.totalPages)
    assertEquals("slug", response.sortBy)
    assertEquals("desc", response.sortDirection)
  }
}