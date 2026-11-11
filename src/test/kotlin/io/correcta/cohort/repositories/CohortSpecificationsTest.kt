package io.correcta.cohort.repositories

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.repositories.specifications.CohortSpecifications
import io.correcta.cohort.repositories.specifications.SpecificationExt.toContainsPattern
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CohortSpecificationsTest {

  private val root = mock<Root<Cohort>>(defaultAnswer = RETURNS_DEEP_STUBS)
  private val query = mock<CriteriaQuery<*>>(defaultAnswer = RETURNS_DEEP_STUBS)
  private val builder = mock<CriteriaBuilder>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `skips absent filters`() {
    assertNull(CohortSpecifications.hasMember(null))
    assertNull(CohortSpecifications.hasMember(" "))
    assertNull(CohortSpecifications.matchesSearch(null))
    assertNull(CohortSpecifications.matchesSearch(""))
    assertNull(CohortSpecifications.ownedBy(null))
    assertNull(CohortSpecifications.hasState(null))
    assertNull(CohortSpecifications.createdFrom(null))
    assertNull(CohortSpecifications.createdUntil(null))
  }

  @Test
  fun `builds predicates for present filters`() {
    val now = Instant.now()

    apply(CohortSpecifications.visibleTo("profile"))
    apply(CohortSpecifications.hasMember("profile"))
    apply(CohortSpecifications.matchesSearch("Turma"))
    apply(CohortSpecifications.ownedBy("owner"))
    apply(CohortSpecifications.hasState(CohortState.ACTIVE))
    apply(CohortSpecifications.createdFrom(now))
    apply(CohortSpecifications.createdUntil(now))

    verify(builder).equal(any(), eq("owner"))
    verify(builder).equal(any(), eq(CohortState.ACTIVE))
    verify(builder).greaterThanOrEqualTo(any(), eq(now))
    verify(builder).lessThanOrEqualTo(any(), eq(now))
    verify(builder, org.mockito.kotlin.times(2)).like(anyOrNull(), eq("%turma%"), eq('\\'))
  }

  @Test
  fun `escapes LIKE wildcards`() {
    assertEquals("%50\\%\\_a\\\\b%", " 50%_A\\b ".toContainsPattern())
  }

  private fun apply(specification: Specification<Cohort>?) {
    assertNotNull(specification).toPredicate(root, query, builder)
  }
}