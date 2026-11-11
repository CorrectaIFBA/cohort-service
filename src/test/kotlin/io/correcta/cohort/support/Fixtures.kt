package io.correcta.cohort.support

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortInvitationCode
import io.correcta.cohort.entities.CohortInvitationCodeState
import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortMembershipInvitation
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.entities.MembershipInvitationState
import io.correcta.cohort.models.response.clients.InstitutionalProfileResponse
import io.correcta.cohort.security.models.ProfilePrincipal
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Duration
import java.time.Instant

/** Builders of domain objects for unit tests. */
object Fixtures {

  val CREATED_AT: Instant = Instant.parse("2026-01-01T00:00:00Z")

  fun future(): Instant = Instant.now().plus(Duration.ofDays(1))

  fun past(): Instant = Instant.now().minus(Duration.ofDays(1))

  fun cohort(
    id: String = "cohort-1",
    ownerProfileId: String = "owner",
    state: CohortState = CohortState.ACTIVE,
    slug: String = "turma",
  ): Cohort = Cohort(
    id = id,
    ownerProfileId = ownerProfileId,
    slug = slug,
    publicCode = "abc123",
    title = "Turma",
    state = state,
    createdAt = CREATED_AT,
    updatedAt = CREATED_AT,
  )

  fun membership(
    cohort: Cohort = cohort(),
    profileId: String = "student",
    role: CohortRole = CohortRole.STUDENT,
    leftAt: Instant? = null,
  ): CohortMembership = CohortMembership(
    id = "membership-$profileId",
    cohort = cohort,
    profileId = profileId,
    role = role,
    joinedAt = CREATED_AT,
    leftAt = leftAt,
  )

  fun invitation(
    cohort: Cohort = cohort(),
    profileId: String = "student",
    state: MembershipInvitationState = MembershipInvitationState.PENDING,
    expiresAt: Instant? = future(),
    role: CohortRole = CohortRole.STUDENT,
  ): CohortMembershipInvitation = CohortMembershipInvitation(
    id = "invitation-$profileId",
    cohort = cohort,
    profileId = profileId,
    role = role,
    invitedByProfileId = "owner",
    state = state,
    invitedAt = CREATED_AT,
    expiresAt = expiresAt,
  )

  fun invitationCode(
    cohort: Cohort = cohort(),
    state: CohortInvitationCodeState = CohortInvitationCodeState.ACTIVE,
    expiresAt: Instant = future(),
  ): CohortInvitationCode = CohortInvitationCode(
    id = "code-1",
    cohort = cohort,
    code = "ABCD2345",
    state = state,
    createdByProfileId = "owner",
    createdAt = CREATED_AT,
    expiresAt = expiresAt,
  )

  fun profile(
    profileId: String = "student",
    type: String = "STUDENT",
    state: String = "ACTIVE",
    profileActive: Boolean = true,
    credentialActive: Boolean = true,
  ): InstitutionalProfileResponse = InstitutionalProfileResponse(
    profileId = profileId,
    type = type,
    state = state,
    profileActive = profileActive,
    credentialActive = credentialActive,
    displayName = "Perfil $profileId",
    email = "$profileId@example.edu",
    login = profileId,
  )

  fun principal(profileId: String = "owner", profileType: String? = "FACULTY"): ProfilePrincipal = ProfilePrincipal(
    profileId = profileId,
    personId = "person-$profileId",
    profileType = profileType,
    login = profileId,
    email = "$profileId@example.edu",
  )

  fun <T> pageOf(vararg content: T, sortField: String = "createdAt"): Page<T> = PageImpl(content.toList(), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, sortField)), content.size.toLong())
}