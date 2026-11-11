package io.correcta.cohort.service

import io.correcta.cohort.entities.CohortMembershipInvitation
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.entities.MembershipInvitationState
import io.correcta.cohort.exceptions.BadRequestException
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.ForbiddenException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.mailer.TransactionalMailProducer
import io.correcta.cohort.models.request.CreateCohortInvitationRequest
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.response.InvitationCandidateStatus
import io.correcta.cohort.repositories.CohortMembershipInvitationRepository
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CohortInvitationServiceTest {

  private val cohortRepository = mock<CohortRepository>()
  private val cohortMembershipRepository = mock<CohortMembershipRepository>()
  private val cohortMembershipInvitationRepository = mock<CohortMembershipInvitationRepository>()
  private val cohortAccessService = mock<CohortAccessService>()
  private val cohortMembershipService = mock<CohortMembershipService>()
  private val identityProfileService = mock<IdentityProfileService>()
  private val scopeEvaluator = mock<ScopeEvaluatorComponent>()
  private val transactionalMailProducer = mock<TransactionalMailProducer>()
  private val service = CohortInvitationService(
    cohortRepository,
    cohortMembershipRepository,
    cohortMembershipInvitationRepository,
    cohortAccessService,
    cohortMembershipService,
    identityProfileService,
    scopeEvaluator,
    transactionalMailProducer,
  )

  private val cohort = Fixtures.cohort()
  private val request = CreateCohortInvitationRequest("student", CohortRole.STUDENT)

  @BeforeEach
  fun setUp() {
    whenever(cohortRepository.findLockedById("cohort-1")).doReturn(cohort)
    whenever(cohortAccessService.requireActiveFaculty("cohort-1")).doReturn(Fixtures.membership(cohort, "owner", CohortRole.FACULTY))
    whenever(scopeEvaluator.authenticatedProfile()).doReturn(Fixtures.principal("student", "STUDENT"))
    whenever(cohortMembershipInvitationRepository.save(any<CohortMembershipInvitation>())).thenAnswer {
      it.getArgument<CohortMembershipInvitation>(0).apply {
        id = "invitation-1"
        invitedAt = Fixtures.CREATED_AT
      }
    }
  }

  @Test
  fun `sends an invitation and queues the e-mail`() {
    val expired = Fixtures.invitation(cohort, expiresAt = Fixtures.past())
    pending(expired)
    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile())
    whenever(identityProfileService.findProfile("owner")).doReturn(Fixtures.profile("owner", "FACULTY"))

    val response = service.sendInvitation("cohort-1", request)

    assertEquals(MembershipInvitationState.EXPIRED, expired.state)
    assertEquals(MembershipInvitationState.PENDING, response.status)
    assertEquals("Perfil student", response.displayName)
    verify(transactionalMailProducer).sendCohortInvitationEmail(any(), eq(Fixtures.profile()), eq("Perfil owner"))
  }

  @Test
  fun `sends the e-mail without the inviter name when the inviter is unknown`() {
    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile())

    service.sendInvitation("cohort-1", request)

    verify(transactionalMailProducer).sendCohortInvitationEmail(any(), any(), eq(null))
  }

  @Test
  fun `rejects invalid invitations`() {
    assertThrows<NotFoundException> { service.sendInvitation("cohort-1", request) }

    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile(credentialActive = false))
    assertThrows<ConflictException> { service.sendInvitation("cohort-1", request) }

    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile())
    assertThrows<BadRequestException> { service.sendInvitation("cohort-1", request.copy(role = CohortRole.FACULTY)) }

    pending(Fixtures.invitation(cohort))
    assertThrows<ConflictException> { service.sendInvitation("cohort-1", request) }

    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", "student")).doReturn(Fixtures.membership(cohort))
    assertThrows<ConflictException> { service.sendInvitation("cohort-1", request) }

    verify(cohortMembershipInvitationRepository, never()).save(any<CohortMembershipInvitation>())
  }

  @Test
  fun `lists my pending invitations to active cohorts`() {
    val valid = Fixtures.invitation(cohort)
    whenever(cohortMembershipInvitationRepository.findByProfileIdAndStateOrderByInvitedAtDesc("student", MembershipInvitationState.PENDING))
      .doReturn(
        listOf(
          valid,
          Fixtures.invitation(cohort, expiresAt = Fixtures.past()),
          Fixtures.invitation(Fixtures.cohort(id = "draft", state = CohortState.DRAFT)),
        ),
      )
    whenever(identityProfileService.findProfiles(listOf("owner"))).doReturn(mapOf("owner" to Fixtures.profile("owner")))

    val response = service.getMyInvitations()

    assertEquals(listOf("invitation-student"), response.map { it.id })
    assertEquals("Perfil owner", response.single().invitedByName)
  }

  @Test
  fun `skips the profile lookup without invitations`() {
    assertEquals(emptyList(), service.getMyInvitations())
    verify(identityProfileService, never()).findProfiles(any())
  }

  @Test
  fun `lists the cohort invitations`() {
    whenever(cohortMembershipInvitationRepository.findByCohortId(eq("cohort-1"), any()))
      .doReturn(Fixtures.pageOf(Fixtures.invitation(cohort), sortField = "invitedAt"))
    whenever(identityProfileService.findProfiles(listOf("student"))).doReturn(mapOf("student" to Fixtures.profile()))

    val response = service.getInvitations("cohort-1", PaginationRequest())

    assertEquals("Perfil student", response.content.single().displayName)
  }

  @Test
  fun `classifies invitation candidates`() {
    whenever(identityProfileService.searchProfiles("ana", "STUDENT", 10)).doReturn(
      listOf(Fixtures.profile("member"), Fixtures.profile("invited"), Fixtures.profile("expired"), Fixtures.profile("free"), Fixtures.profile("staff", "STAFF")),
    )
    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdInAndLeftAtIsNull(eq("cohort-1"), any()))
      .doReturn(listOf(Fixtures.membership(cohort, "member")))
    whenever(cohortMembershipInvitationRepository.findByCohortIdAndProfileIdInAndState(eq("cohort-1"), any(), eq(MembershipInvitationState.PENDING)))
      .doReturn(listOf(Fixtures.invitation(cohort, "invited"), Fixtures.invitation(cohort, "expired", expiresAt = Fixtures.past())))

    val candidates = service.searchCandidates("cohort-1", "  ana ", CohortRole.STUDENT)

    assertEquals(
      listOf(
        "member" to InvitationCandidateStatus.MEMBER,
        "invited" to InvitationCandidateStatus.INVITED,
        "expired" to InvitationCandidateStatus.AVAILABLE,
        "free" to InvitationCandidateStatus.AVAILABLE,
      ),
      candidates.map { it.profileId to it.status },
    )
  }

  @Test
  fun `returns no candidates without eligible profiles`() {
    whenever(identityProfileService.searchProfiles(eq("ana"), anyOrNull(), eq(10))).doReturn(listOf(Fixtures.profile("staff", "STAFF")))

    assertEquals(emptyList(), service.searchCandidates("cohort-1", "ana", null))
    verify(cohortMembershipRepository, never()).findByCohortIdAndProfileIdInAndLeftAtIsNull(any(), any())
  }

  @Test
  fun `rejects short candidate searches`() {
    assertThrows<BadRequestException> { service.searchCandidates("cohort-1", " a ", null) }
  }

  @Test
  fun `revokes a pending invitation`() {
    val invitation = Fixtures.invitation(cohort)
    pending(invitation)

    service.revokeInvitation("cohort-1", "student")

    assertEquals(MembershipInvitationState.REVOKED, invitation.state)
    assertNotNull(invitation.revokedAt)
    assertThrows<NotFoundException> { service.revokeInvitation("cohort-1", "other") }
  }

  @Test
  fun `accepts a pending invitation`() {
    val invitation = Fixtures.invitation(cohort)
    pending(invitation)
    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile())
    whenever(cohortMembershipService.addMember(cohort, "student", CohortRole.STUDENT)).doReturn(Fixtures.membership(cohort))

    val (membership, created) = service.acceptInvitation("cohort-1")

    assertTrue(created)
    assertEquals("student", membership.profileId)
    assertEquals(MembershipInvitationState.ACCEPTED, invitation.state)
    assertNotNull(invitation.acceptedAt)
  }

  @Test
  fun `accepts an already accepted invitation again`() {
    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", "student")).doReturn(Fixtures.membership(cohort))
    whenever(cohortMembershipInvitationRepository.existsByCohortIdAndProfileIdAndState("cohort-1", "student", MembershipInvitationState.ACCEPTED))
      .doReturn(true)

    val (_, created) = service.acceptInvitation("cohort-1")

    assertFalse(created)
  }

  @Test
  fun `rejects accepting without a pending invitation`() {
    assertThrows<NotFoundException> { service.acceptInvitation("missing") }
    assertThrows<NotFoundException> { service.acceptInvitation("cohort-1") }

    whenever(cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull("cohort-1", "student")).doReturn(Fixtures.membership(cohort))
    assertThrows<NotFoundException> { service.acceptInvitation("cohort-1") }
  }

  @Test
  fun `rejects accepting into inactive cohorts or with ineligible profiles`() {
    pending(Fixtures.invitation(cohort))
    assertThrows<ForbiddenException> { service.acceptInvitation("cohort-1") }

    whenever(identityProfileService.findProfile("student")).doReturn(Fixtures.profile(state = "SUSPENDED"))
    assertThrows<ForbiddenException> { service.acceptInvitation("cohort-1") }

    cohort.state = CohortState.ARCHIVED
    assertThrows<ConflictException> { service.acceptInvitation("cohort-1") }
  }

  private fun pending(vararg invitations: CohortMembershipInvitation) {
    whenever(cohortMembershipInvitationRepository.findByCohortIdAndProfileIdAndState(eq("cohort-1"), eq(invitations.first().profileId), eq(MembershipInvitationState.PENDING)))
      .doReturn(invitations.toList())
  }
}