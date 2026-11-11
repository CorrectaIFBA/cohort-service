package io.correcta.cohort.entities

import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntitiesTest {

  @ParameterizedTest
  @CsvSource(
    "DRAFT, DRAFT, false",
    "DRAFT, ACTIVE, true",
    "DRAFT, ARCHIVED, true",
    "ACTIVE, DRAFT, false",
    "ACTIVE, ACTIVE, false",
    "ACTIVE, ARCHIVED, true",
    "ARCHIVED, DRAFT, false",
    "ARCHIVED, ACTIVE, true",
    "ARCHIVED, ARCHIVED, false",
  )
  fun `allows only the documented cohort transitions`(from: CohortState, to: CohortState, allowed: Boolean) {
    assertEquals(allowed, from.canTransitionTo(to))
  }

  @Test
  fun `reports expired invitation codes`() {
    val now = Instant.now()

    assertEquals(CohortInvitationCodeState.ACTIVE, Fixtures.invitationCode(expiresAt = Fixtures.future()).effectiveState(now))
    assertEquals(CohortInvitationCodeState.EXPIRED, Fixtures.invitationCode(expiresAt = Fixtures.past()).effectiveState(now))
    assertEquals(CohortInvitationCodeState.EXPIRED, Fixtures.invitationCode(expiresAt = now).effectiveState(now))
    assertEquals(
      CohortInvitationCodeState.REVOKED,
      Fixtures.invitationCode(state = CohortInvitationCodeState.REVOKED, expiresAt = Fixtures.past()).effectiveState(),
    )
  }

  @Test
  fun `reports expired invitations`() {
    val now = Instant.now()

    assertEquals(MembershipInvitationState.PENDING, Fixtures.invitation(expiresAt = null).effectiveState(now))
    assertEquals(MembershipInvitationState.PENDING, Fixtures.invitation(expiresAt = Fixtures.future()).effectiveState(now))
    assertEquals(MembershipInvitationState.EXPIRED, Fixtures.invitation(expiresAt = Fixtures.past()).effectiveState(now))
    assertEquals(
      MembershipInvitationState.ACCEPTED,
      Fixtures.invitation(state = MembershipInvitationState.ACCEPTED, expiresAt = Fixtures.past()).effectiveState(),
    )
  }

  @Test
  fun `keeps every cohort field`() {
    val cohort = Cohort(ownerProfileId = "owner", slug = "slug", publicCode = "abc123", title = "Title")
    assertEquals(CohortState.DRAFT, cohort.state)
    assertNull(cohort.description)

    val now = Instant.now()
    cohort.id = "id"
    cohort.ownerProfileId = "other"
    cohort.publicCode = "xyz789"
    cohort.description = "description"
    cohort.archivedAt = now
    cohort.createdAt = now
    cohort.updatedAt = now

    assertEquals("id", cohort.id)
    assertEquals("other", cohort.ownerProfileId)
    assertEquals("xyz789", cohort.publicCode)
    assertEquals("description", cohort.description)
    assertEquals(now, cohort.archivedAt)
    assertEquals(now, cohort.createdAt)
    assertEquals(now, cohort.updatedAt)
  }

  @Test
  fun `keeps every membership field`() {
    val cohort = Fixtures.cohort()
    val membership = CohortMembership(cohort = Fixtures.cohort(id = "other"), profileId = "a", role = CohortRole.STUDENT)

    membership.id = "id"
    membership.cohort = cohort
    membership.profileId = "b"

    assertEquals("id", membership.id)
    assertEquals(cohort, membership.cohort)
    assertEquals("b", membership.profileId)
  }

  @Test
  fun `keeps every invitation field`() {
    val cohort = Fixtures.cohort()
    val now = Instant.now()
    val invitation = CohortMembershipInvitation(
      cohort = Fixtures.cohort(id = "other"),
      profileId = "a",
      role = CohortRole.STUDENT,
      invitedByProfileId = "owner",
    )
    assertEquals(MembershipInvitationState.PENDING, invitation.state)

    invitation.id = "id"
    invitation.cohort = cohort
    invitation.profileId = "b"
    invitation.role = CohortRole.FACULTY
    invitation.invitedByProfileId = "other-owner"
    invitation.invitedAt = now
    invitation.expiresAt = now
    invitation.acceptedAt = now
    invitation.rejectedAt = now
    invitation.revokedAt = now

    assertEquals("id", invitation.id)
    assertEquals(cohort, invitation.cohort)
    assertEquals("b", invitation.profileId)
    assertEquals(CohortRole.FACULTY, invitation.role)
    assertEquals("other-owner", invitation.invitedByProfileId)
    assertEquals(now, invitation.invitedAt)
    assertEquals(now, invitation.acceptedAt)
    assertEquals(now, invitation.rejectedAt)
    assertEquals(now, invitation.revokedAt)
  }

  @Test
  fun `keeps every invitation code field`() {
    val cohort = Fixtures.cohort()
    val now = Instant.now()
    val code = CohortInvitationCode(cohort = Fixtures.cohort(id = "other"), code = "A", createdByProfileId = "a", expiresAt = now)
    assertEquals(CohortInvitationCodeState.ACTIVE, code.state)

    code.id = "id"
    code.cohort = cohort
    code.code = "B"
    code.createdByProfileId = "b"
    code.createdAt = now
    code.expiresAt = now
    code.revokedAt = now

    assertEquals("id", code.id)
    assertEquals(cohort, code.cohort)
    assertEquals("B", code.code)
    assertEquals("b", code.createdByProfileId)
    assertEquals(now, code.createdAt)
    assertEquals(now, code.revokedAt)
  }
}