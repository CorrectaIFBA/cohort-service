package io.correcta.cohort.service

import io.correcta.cohort.entities.CohortInvitationCode
import io.correcta.cohort.entities.CohortInvitationCodeState
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.ForbiddenException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.models.request.CreateInvitationCodeRequest
import io.correcta.cohort.repositories.CohortInvitationCodeRepository
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CohortInvitationCodeServiceTest {

  private val cohortRepository = mock<CohortRepository>()
  private val cohortInvitationCodeRepository = mock<CohortInvitationCodeRepository>()
  private val cohortMembershipRepository = mock<CohortMembershipRepository>()
  private val cohortAccessService = mock<CohortAccessService>()
  private val cohortMembershipService = mock<CohortMembershipService>()
  private val identityProfileService = mock<IdentityProfileService>()
  private val scopeEvaluator = mock<ScopeEvaluatorComponent>()
  private val service = CohortInvitationCodeService(
    cohortRepository,
    cohortInvitationCodeRepository,
    cohortMembershipRepository,
    cohortAccessService,
    cohortMembershipService,
    identityProfileService,
    scopeEvaluator,
  )

  private val cohort = Fixtures.cohort()

  @BeforeEach
  fun setUp() {
    whenever(cohortRepository.findLockedById("cohort-1")).doReturn(cohort)
    whenever(cohortAccessService.requireActiveFaculty("cohort-1")).doReturn(Fixtures.membership(cohort, "owner", CohortRole.FACULTY))
    whenever(scopeEvaluator.authenticatedProfile()).doReturn(Fixtures.principal("student", "STUDENT"))
  }

  @Test
  fun `generates a code and retires the previous ones`() {
    val expired = Fixtures.invitationCode(cohort, expiresAt = Fixtures.past())
    val active = Fixtures.invitationCode(cohort)
    whenever(cohortInvitationCodeRepository.findByCohortIdAndState("cohort-1", CohortInvitationCodeState.ACTIVE))
      .doReturn(listOf(expired, active))
    whenever(cohortInvitationCodeRepository.findByCode(any())).doReturn(active, null)
    whenever(cohortInvitationCodeRepository.save(any<CohortInvitationCode>())).thenAnswer {
      it.getArgument<CohortInvitationCode>(0).apply {
        id = "code-2"
        createdAt = Instant.now()
      }
    }

    val response = service.generateInvitationCode("cohort-1", CreateInvitationCodeRequest())

    assertEquals(CohortInvitationCodeState.EXPIRED, expired.state)
    assertEquals(CohortInvitationCodeState.REVOKED, active.state)
    assertNotNull(active.revokedAt)
    assertEquals(CohortInvitationCodeState.ACTIVE, response.state)
    assertEquals("owner", response.createdByProfileId)
    assertEquals(8, response.code.length)
  }

  @Test
  fun `returns the valid code`() {
    whenever(cohortInvitationCodeRepository.findByCohortIdAndState("cohort-1", CohortInvitationCodeState.ACTIVE))
      .doReturn(listOf(Fixtures.invitationCode(cohort, expiresAt = Fixtures.past()), Fixtures.invitationCode(cohort)))

    assertEquals("ABCD2345", service.getInvitationCode("cohort-1").code)
  }

  @Test
  fun `fails without a valid code`() {
    whenever(cohortInvitationCodeRepository.findByCohortIdAndState("cohort-1", CohortInvitationCodeState.ACTIVE))
      .doReturn(listOf(Fixtures.invitationCode(cohort, expiresAt = Fixtures.past())))

    assertThrows<NotFoundException> { service.getInvitationCode("cohort-1") }
  }

  @Test
  fun `revokes the valid code`() {
    val code = Fixtures.invitationCode(cohort)
    whenever(cohortInvitationCodeRepository.findByCohortIdAndState("cohort-1", CohortInvitationCodeState.ACTIVE)).doReturn(listOf(code))

    service.revokeInvitationCode("cohort-1")

    assertEquals(CohortInvitationCodeState.REVOKED, code.state)
    assertNotNull(code.revokedAt)
  }

  @Test
  fun `joins the cohort with the profile type as role`() {
    whenever(cohortInvitationCodeRepository.findByCode("ABCD2345")).doReturn(Fixtures.invitationCode(cohort))
    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile())
    whenever(cohortMembershipService.addMember(cohort, "student", CohortRole.STUDENT)).doReturn(Fixtures.membership(cohort))

    assertEquals(CohortRole.STUDENT, service.joinCohort("abcd-2345").role)
  }

  @Test
  fun `passes current members on to the membership check`() {
    whenever(cohortInvitationCodeRepository.findByCode("ABCD2345")).doReturn(Fixtures.invitationCode(cohort))
    whenever(cohortMembershipRepository.findByCohortIdAndProfileId("cohort-1", "student")).doReturn(Fixtures.membership(cohort))
    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile())
    whenever(cohortMembershipService.addMember(cohort, "student", CohortRole.STUDENT)).thenThrow(ConflictException())

    assertThrows<ConflictException> { service.joinCohort("ABCD2345") }
  }

  @Test
  fun `rejects unknown and expired codes`() {
    assertThrows<NotFoundException> { service.joinCohort("UNKNOWN1") }

    whenever(cohortInvitationCodeRepository.findByCode("ABCD2345")).doReturn(Fixtures.invitationCode(cohort, expiresAt = Fixtures.past()))
    assertThrows<NotFoundException> { service.joinCohort("ABCD2345") }
  }

  @Test
  fun `rejects inactive cohorts`() {
    whenever(cohortInvitationCodeRepository.findByCode("ABCD2345")).doReturn(Fixtures.invitationCode(cohort))
    cohort.state = CohortState.ARCHIVED

    assertThrows<ConflictException> { service.joinCohort("ABCD2345") }
  }

  @Test
  fun `rejects removed members`() {
    whenever(cohortInvitationCodeRepository.findByCode("ABCD2345")).doReturn(Fixtures.invitationCode(cohort))
    whenever(cohortMembershipRepository.findByCohortIdAndProfileId("cohort-1", "student"))
      .doReturn(Fixtures.membership(cohort, leftAt = Instant.now()))

    assertThrows<ForbiddenException> { service.joinCohort("ABCD2345") }
  }

  @Test
  fun `rejects unknown and ineligible profiles`() {
    whenever(cohortInvitationCodeRepository.findByCode("ABCD2345")).doReturn(Fixtures.invitationCode(cohort))
    assertThrows<ForbiddenException> { service.joinCohort("ABCD2345") }

    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile(profileActive = false))
    assertThrows<ForbiddenException> { service.joinCohort("ABCD2345") }
  }
}