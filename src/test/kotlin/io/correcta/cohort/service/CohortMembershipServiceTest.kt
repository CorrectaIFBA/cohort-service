package io.correcta.cohort.service

import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.MembershipInvitationState
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.repositories.CohortMembershipInvitationRepository
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class CohortMembershipServiceTest {

  private val cohortRepository = mock<CohortRepository>()
  private val cohortMembershipRepository = mock<CohortMembershipRepository>()
  private val cohortMembershipInvitationRepository = mock<CohortMembershipInvitationRepository>()
  private val cohortAccessService = mock<CohortAccessService>()
  private val identityProfileService = mock<IdentityProfileService>()
  private val service = CohortMembershipService(
    cohortRepository,
    cohortMembershipRepository,
    cohortMembershipInvitationRepository,
    cohortAccessService,
    identityProfileService,
  )

  private val cohort = Fixtures.cohort(ownerProfileId = "owner")

  @Test
  fun `lists members with their profiles`() {
    whenever(cohortMembershipRepository.findByCohortIdAndLeftAtIsNull(eq("cohort-1"), any()))
      .doReturn(Fixtures.pageOf(Fixtures.membership(cohort, "a"), Fixtures.membership(cohort, "b"), sortField = "joinedAt"))
    whenever(identityProfileService.findProfiles(listOf("a", "b"))).doReturn(mapOf("a" to Fixtures.profile("a")))

    val response = service.getMembers("cohort-1", PaginationRequest())

    verify(cohortAccessService).requireReadableActiveCohort("cohort-1")
    assertEquals("Perfil a", response.content[0].displayName)
    assertNull(response.content[1].displayName)
  }

  @Test
  fun `lists faculty members`() {
    whenever(cohortMembershipRepository.findByCohortIdAndRoleAndLeftAtIsNull(eq("cohort-1"), eq(CohortRole.FACULTY), any()))
      .doReturn(Fixtures.pageOf(Fixtures.membership(cohort, "owner", CohortRole.FACULTY), sortField = "joinedAt"))

    val response = service.getFaculty("cohort-1", PaginationRequest())

    assertEquals(CohortRole.FACULTY, response.content.single().role)
  }

  @Test
  fun `returns the current membership to internal callers`() {
    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", "student"))
      .doReturn(Fixtures.membership(cohort))

    assertEquals(CohortRole.STUDENT, service.getCurrentMembership("cohort-1", "student").role)
    assertThrows<NotFoundException> { service.getCurrentMembership("cohort-1", "stranger") }
  }

  @Test
  fun `removes a member and revokes its pending invitations`() {
    val membership = Fixtures.membership(cohort)
    val invitation = Fixtures.invitation(cohort)
    whenever(cohortRepository.findLockedById("cohort-1")).doReturn(cohort)
    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", "student")).doReturn(membership)
    whenever(cohortMembershipInvitationRepository.findByCohortIdAndProfileIdAndState("cohort-1", "student", MembershipInvitationState.PENDING))
      .doReturn(listOf(invitation))

    service.removeMember("cohort-1", "student")

    verify(cohortAccessService).requireActiveFaculty("cohort-1")
    assertNotNull(membership.leftAt)
    assertEquals(MembershipInvitationState.REVOKED, invitation.state)
    assertEquals(membership.leftAt, invitation.revokedAt)
  }

  @Test
  fun `protects the owner and unknown members`() {
    whenever(cohortRepository.findLockedById("cohort-1")).doReturn(cohort)
    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", "owner"))
      .doReturn(Fixtures.membership(cohort, "owner", CohortRole.FACULTY))

    assertThrows<ConflictException> { service.removeMember("cohort-1", "owner") }
    assertThrows<NotFoundException> { service.removeMember("cohort-1", "stranger") }
  }

  @Test
  fun `adds a new member`() {
    whenever(cohortMembershipRepository.saveAndFlush(any<CohortMembership>())).thenAnswer { it.getArgument<CohortMembership>(0) }

    val membership = service.addMember(cohort, "student", CohortRole.STUDENT)

    assertEquals("student", membership.profileId)
    assertEquals(CohortRole.STUDENT, membership.role)
  }

  @Test
  fun `reactivates a removed member`() {
    val removed = Fixtures.membership(cohort, leftAt = Instant.now())
    whenever(cohortMembershipRepository.findByCohortIdAndProfileId("cohort-1", "student")).doReturn(removed)

    val membership = service.addMember(cohort, "student", CohortRole.FACULTY)

    assertSame(removed, membership)
    assertNull(membership.leftAt)
    assertEquals(CohortRole.FACULTY, membership.role)
  }

  @Test
  fun `rejects current members`() {
    whenever(cohortMembershipRepository.findByCohortIdAndProfileId("cohort-1", "student")).doReturn(Fixtures.membership(cohort))

    assertThrows<ConflictException> { service.addMember(cohort, "student", CohortRole.STUDENT) }
  }
}