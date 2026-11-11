package io.correcta.cohort.service

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.exceptions.BadRequestException
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.models.request.ChangeCohortStatusRequest
import io.correcta.cohort.models.request.CohortFilterRequest
import io.correcta.cohort.models.request.CreateCohortRequest
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.request.UpdateCohortRequest
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CohortServiceTest {

  private val cohortRepository = mock<CohortRepository>()
  private val cohortMembershipRepository = mock<CohortMembershipRepository>()
  private val cohortAccessService = mock<CohortAccessService>()
  private val scopeEvaluator = mock<ScopeEvaluatorComponent>()
  private val service = CohortService(cohortRepository, cohortMembershipRepository, cohortAccessService, scopeEvaluator)

  @BeforeEach
  fun setUp() {
    whenever(scopeEvaluator.authenticatedProfile()).doReturn(Fixtures.principal("owner"))
    whenever(cohortRepository.save(any<Cohort>())).thenAnswer {
      it.getArgument<Cohort>(0).apply {
        id = "cohort-1"
        createdAt = Fixtures.CREATED_AT
      }
    }
    whenever(cohortRepository.saveAndFlush(any<Cohort>())).thenAnswer { it.getArgument<Cohort>(0) }
    whenever(cohortRepository.findAll(any<Specification<Cohort>>(), any<Pageable>())).doReturn(Fixtures.pageOf(Fixtures.cohort()))
  }

  @Test
  fun `lists cohorts with every filter`() {
    whenever(cohortAccessService.isStaff()).doReturn(false)
    val now = Instant.now()

    val response = service.getCohorts(
      CohortFilterRequest(CohortState.ACTIVE, now, now, true, "turma", "student"),
      PaginationRequest(),
    )

    assertEquals("cohort-1", response.content.single().id)
    assertEquals("createdAt", response.sortBy)
  }

  @Test
  fun `lists cohorts without filters for staff`() {
    whenever(cohortAccessService.isStaff()).doReturn(true)

    service.getCohorts(CohortFilterRequest(createdAfter = Instant.now()), PaginationRequest())
    service.getCohorts(CohortFilterRequest(createdBefore = Instant.now()), PaginationRequest())

    verify(cohortRepository, times(2)).findAll(any<Specification<Cohort>>(), any<Pageable>())
  }

  @Test
  fun `rejects an inverted creation range`() {
    val now = Instant.now()

    assertThrows<BadRequestException> {
      service.getCohorts(CohortFilterRequest(createdAfter = now, createdBefore = now.minusSeconds(1)), PaginationRequest())
    }
  }

  @Test
  fun `reads a cohort by ID and current slug`() {
    whenever(cohortAccessService.findVisibleCohort("cohort-1")).doReturn(Fixtures.cohort(slug = "turma"))

    assertEquals("turma", service.getCohort("cohort-1", "turma").slug)
    assertThrows<NotFoundException> { service.getCohort("cohort-1", "old-slug") }
  }

  @Test
  fun `reads a cohort by public code`() {
    whenever(cohortAccessService.findVisibleCohortByPublicCode("abc123")).doReturn(Fixtures.cohort())

    assertEquals("abc123", service.getCohortByPublicCode("abc123").publicCode)
  }

  @Test
  fun `creates a draft cohort with its owner as faculty`() {
    whenever(cohortRepository.existsByPublicCode(any())).doReturn(true, false)

    val response = service.createCohort(CreateCohortRequest(name = "  Algoritmos  ", description = "  Turma A  "))

    assertEquals("Algoritmos", response.name)
    assertEquals("algoritmos", response.slug)
    assertEquals("Turma A", response.description)
    assertEquals(CohortState.DRAFT, response.status)
    assertEquals("owner", response.ownerProfileId)

    val captor = argumentCaptor<CohortMembership>()
    verify(cohortMembershipRepository).save(captor.capture())
    assertEquals("owner", captor.firstValue.profileId)
    assertEquals(CohortRole.FACULTY, captor.firstValue.role)
  }

  @Test
  fun `keeps a given slug and avoids reserved generated slugs`() {
    assertEquals("custom", service.createCohort(CreateCohortRequest(name = "Algoritmos", slug = "custom")).slug)

    val generated = service.createCohort(CreateCohortRequest(name = "Members"))
    assertEquals("members-turma", generated.slug)
    assertNull(generated.description)
  }

  @Test
  fun `rejects reserved slugs and non-draft creation`() {
    assertThrows<BadRequestException> { service.createCohort(CreateCohortRequest(name = "Turma", slug = "members")) }
    assertThrows<BadRequestException> { service.createCohort(CreateCohortRequest(name = "Turma", status = CohortState.ACTIVE)) }
  }

  @Test
  fun `updates cohort metadata`() {
    val cohort = Fixtures.cohort(slug = "turma")
    whenever(cohortAccessService.findOwnedCohort("cohort-1")).doReturn(cohort)

    val renamed = service.updateCohort("cohort-1", UpdateCohortRequest(" Novo nome ", "novo-slug", " Descrição "))
    assertEquals("Novo nome", renamed.name)
    assertEquals("novo-slug", renamed.slug)
    assertEquals("Descrição", renamed.description)

    val cleared = service.updateCohort("cohort-1", UpdateCohortRequest("Nome", "novo-slug"))
    assertNull(cleared.description)
  }

  @Test
  fun `rejects updates of archived cohorts and reserved slugs`() {
    val cohort = Fixtures.cohort()
    whenever(cohortAccessService.findOwnedCohort("cohort-1")).doReturn(cohort)

    assertThrows<BadRequestException> { service.updateCohort("cohort-1", UpdateCohortRequest("Nome", "faculty")) }

    cohort.state = CohortState.ARCHIVED
    assertThrows<ConflictException> { service.updateCohort("cohort-1", UpdateCohortRequest("Nome", "turma")) }
  }

  @Test
  fun `changes the cohort status`() {
    val cohort = Fixtures.cohort(state = CohortState.ACTIVE)
    whenever(cohortAccessService.findOwnedCohort("cohort-1")).doReturn(cohort)

    service.changeCohortStatus("cohort-1", ChangeCohortStatusRequest(CohortState.ARCHIVED))
    assertNotNull(cohort.archivedAt)

    service.changeCohortStatus("cohort-1", ChangeCohortStatusRequest(CohortState.ACTIVE))
    assertNull(cohort.archivedAt)

    assertThrows<ConflictException> { service.changeCohortStatus("cohort-1", ChangeCohortStatusRequest(CohortState.DRAFT)) }
  }

  @Test
  fun `deletes only draft cohorts`() {
    val cohort = Fixtures.cohort(state = CohortState.DRAFT)
    whenever(cohortAccessService.findOwnedCohort("cohort-1")).doReturn(cohort)

    service.deleteCohort("cohort-1")
    verify(cohortMembershipRepository).deleteByCohortId("cohort-1")
    verify(cohortRepository).delete(cohort)

    cohort.state = CohortState.ACTIVE
    assertThrows<ConflictException> { service.deleteCohort("cohort-1") }
    verify(cohortRepository, times(1)).delete(any<Cohort>())
  }
}