package io.correcta.cohort.models

import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.entities.MembershipInvitationState
import io.correcta.cohort.mailer.requests.RecipientRef
import io.correcta.cohort.models.request.ChangeCohortStatusRequest
import io.correcta.cohort.models.request.CohortFilterRequest
import io.correcta.cohort.models.request.CreateCohortInvitationRequest
import io.correcta.cohort.models.request.CreateCohortRequest
import io.correcta.cohort.models.request.CreateInvitationCodeRequest
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.request.UpdateCohortRequest
import io.correcta.cohort.models.request.clients.ProfileBatchLookupRequest
import io.correcta.cohort.models.response.GetCohortInvitationResponse
import io.correcta.cohort.models.response.GetCohortMemberResponse
import io.correcta.cohort.models.response.GetMyCohortInvitationResponse
import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse
import io.correcta.cohort.models.response.clients.ProfileBatchLookupResponse
import io.correcta.cohort.models.response.error.ValidationErrorResponse
import io.correcta.cohort.support.Fixtures
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelsTest {

  @Test
  fun `keeps request fields`() {
    val now = Instant.now()

    assertEquals(CohortState.ACTIVE, ChangeCohortStatusRequest(CohortState.ACTIVE).status)

    val filter = CohortFilterRequest(CohortState.DRAFT, now, now, true, "search", "member")
    assertEquals(CohortState.DRAFT, filter.status)
    assertEquals(now, filter.createdAfter)
    assertEquals(now, filter.createdBefore)
    assertTrue(filter.mine)
    assertEquals("search", filter.search)
    assertEquals("member", filter.memberProfileId)

    val invitation = CreateCohortInvitationRequest("profile", CohortRole.STUDENT, now)
    assertEquals("profile", invitation.profileId)
    assertEquals(CohortRole.STUDENT, invitation.role)
    assertEquals(now, invitation.expiresAt)

    val create = CreateCohortRequest("name", "slug", "description")
    assertEquals("name", create.name)
    assertEquals("slug", create.slug)
    assertEquals("description", create.description)
    assertEquals(CohortState.DRAFT, create.status)

    assertNull(CreateInvitationCodeRequest().expiresAt)

    val pagination = PaginationRequest()
    assertEquals(0, pagination.page)
    assertEquals(20, pagination.size)
    assertNull(pagination.sortBy)
    assertEquals("desc", pagination.sortDirection)

    val update = UpdateCohortRequest("name", "slug")
    assertEquals("name", update.name)
    assertEquals("slug", update.slug)
    assertNull(update.description)

    assertEquals(listOf("a"), ProfileBatchLookupRequest(listOf("a")).profileIds)
  }

  @Test
  fun `keeps client and error response fields`() {
    val profile = Fixtures.profile()
    assertEquals("ACTIVE", profile.state)
    assertTrue(profile.profileActive)
    assertTrue(profile.credentialActive)

    assertEquals(setOf("STUDENT", "FACULTY"), InstitutionalProfileResponse.COHORT_PROFILE_TYPES)
    assertNull(RecipientRef("a@example.edu").name)

    assertEquals(listOf("missing"), ProfileBatchLookupResponse(listOf(profile), listOf("missing")).notFoundProfileIds)

    val validation = ValidationErrorResponse("message", mapOf("field" to "error"))
    assertEquals("message", validation.message)
    assertEquals(mapOf("field" to "error"), validation.errors)
  }

  @Test
  fun `accepts only active cohort profiles`() {
    assertTrue(Fixtures.profile().isEligible())
    assertTrue(Fixtures.profile(type = "FACULTY").isEligible())
    assertFalse(Fixtures.profile(state = "SUSPENDED").isEligible())
    assertFalse(Fixtures.profile(profileActive = false).isEligible())
    assertFalse(Fixtures.profile(credentialActive = false).isEligible())
    assertFalse(Fixtures.profile(type = "STAFF").isEligible())
  }

  @Test
  fun `maps invitations with and without a profile`() {
    val withProfile = GetCohortInvitationResponse.fromInvitation(Fixtures.invitation(), Fixtures.profile())
    assertEquals("Perfil student", withProfile.displayName)
    assertEquals("student", withProfile.login)
    assertEquals("student@example.edu", withProfile.email)
    assertEquals("STUDENT", withProfile.profileType)
    assertEquals(MembershipInvitationState.PENDING, withProfile.status)

    val withoutProfile = GetCohortInvitationResponse.fromInvitation(Fixtures.invitation(expiresAt = null), null)
    assertNull(withoutProfile.displayName)
    assertNull(withoutProfile.login)
    assertNull(withoutProfile.email)
    assertNull(withoutProfile.profileType)
    assertNull(withoutProfile.expiresAt)
  }

  @Test
  fun `maps members with and without a profile`() {
    val membership = Fixtures.membership()

    assertEquals("STUDENT", GetCohortMemberResponse.fromMembership(membership, Fixtures.profile()).profileType)

    val withoutProfile = GetCohortMemberResponse.fromMembership(membership, null)
    assertNull(withoutProfile.displayName)
    assertNull(withoutProfile.profileType)
  }

  @Test
  fun `maps my invitations with and without an inviter`() {
    assertEquals("Perfil owner", GetMyCohortInvitationResponse.fromInvitation(Fixtures.invitation(), Fixtures.profile("owner")).invitedByName)

    val withoutInviter = GetMyCohortInvitationResponse.fromInvitation(Fixtures.invitation(expiresAt = null), null)
    assertNull(withoutInviter.invitedByName)
    assertNull(withoutInviter.expiresAt)
  }
}