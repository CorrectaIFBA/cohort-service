package io.correcta.cohort.service

import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.ForbiddenException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals

class CohortAccessServiceTest {

  private val cohortRepository = mock<CohortRepository>()
  private val cohortMembershipRepository = mock<CohortMembershipRepository>()
  private val scopeEvaluator = mock<ScopeEvaluatorComponent>()
  private val service = CohortAccessService(cohortRepository, cohortMembershipRepository, scopeEvaluator)

  private val cohort = Fixtures.cohort(ownerProfileId = "owner")

  @BeforeEach
  fun setUp() {
    whenever(cohortRepository.findById("cohort-1")).doReturn(Optional.of(cohort))
    whenever(cohortRepository.findByPublicCode("abc123")).doReturn(cohort)
  }

  @Test
  fun `hides missing cohorts`() {
    caller("owner")
    whenever(cohortRepository.findById("missing")).doReturn(Optional.empty())

    assertThrows<NotFoundException> { service.findVisibleCohort("missing") }
    assertThrows<NotFoundException> { service.findVisibleCohortByPublicCode("missing") }
  }

  @Test
  fun `shows the cohort to staff, the owner and current members`() {
    caller("staff", staff = true)
    assertEquals(cohort, service.findVisibleCohort("cohort-1"))

    caller("owner")
    assertEquals(cohort, service.findVisibleCohortByPublicCode("abc123"))

    caller("student")
    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", "student"))
      .doReturn(Fixtures.membership(cohort))
    assertEquals(cohort, service.findVisibleCohort("cohort-1"))
  }

  @Test
  fun `hides the cohort from other profiles`() {
    caller("stranger")

    assertThrows<NotFoundException> { service.findVisibleCohort("cohort-1") }
    assertThrows<NotFoundException> { service.findVisibleCohortByPublicCode("abc123") }
  }

  @Test
  fun `returns owned cohorts only to the owner`() {
    caller("owner")
    assertEquals(cohort, service.findOwnedCohort("cohort-1"))

    caller("staff", staff = true)
    assertThrows<ForbiddenException> { service.findOwnedCohort("cohort-1") }
  }

  @Test
  fun `requires a current membership in an active cohort`() {
    caller("student")
    assertThrows<NotFoundException> { service.requireActiveMember("cohort-1") }

    member("student", Fixtures.membership(Fixtures.cohort(state = CohortState.DRAFT)))
    assertThrows<ConflictException> { service.requireActiveMember("cohort-1") }

    val membership = Fixtures.membership(cohort)
    member("student", membership)
    assertEquals(membership, service.requireActiveMember("cohort-1"))
  }

  @Test
  fun `requires faculty members`() {
    caller("student")
    member("student", Fixtures.membership(cohort))
    assertThrows<ForbiddenException> { service.requireActiveFaculty("cohort-1") }

    val faculty = Fixtures.membership(cohort, "student", CohortRole.FACULTY)
    member("student", faculty)
    assertEquals(faculty, service.requireActiveFaculty("cohort-1"))
  }

  @Test
  fun `lets members read active cohorts`() {
    caller("student")
    member("student", Fixtures.membership(cohort))

    assertDoesNotThrow { service.requireReadableActiveCohort("cohort-1") }
  }

  @Test
  fun `lets staff read any active cohort`() {
    caller("staff", staff = true)
    assertDoesNotThrow { service.requireReadableActiveCohort("cohort-1") }

    whenever(cohortRepository.findById("missing")).doReturn(Optional.empty())
    assertThrows<NotFoundException> { service.requireReadableActiveCohort("missing") }

    cohort.state = CohortState.ARCHIVED
    assertThrows<ConflictException> { service.requireReadableActiveCohort("cohort-1") }
  }

  private fun caller(profileId: String, staff: Boolean = false) {
    whenever(scopeEvaluator.authenticatedProfile()).doReturn(Fixtures.principal(profileId))
    whenever(scopeEvaluator.isProfileType("STAFF")).doReturn(staff)
  }

  private fun member(profileId: String, membership: io.correcta.cohort.entities.CohortMembership) {
    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", profileId)).doReturn(membership)
  }
}