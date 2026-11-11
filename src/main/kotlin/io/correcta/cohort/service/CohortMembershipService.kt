package io.correcta.cohort.service

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.MembershipInvitationState
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.response.GetCohortMemberResponse
import io.correcta.cohort.models.response.GetInternalCohortMembershipResponse
import io.correcta.cohort.models.response.PagedResponse
import io.correcta.cohort.repositories.CohortMembershipInvitationRepository
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.utils.PaginationExt.toPageRequest
import io.correcta.cohort.utils.PaginationExt.toPagedResponse
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Cohort membership listing, lookup and removal. */
@Service
class CohortMembershipService(
  val cohortRepository: CohortRepository,
  val cohortMembershipRepository: CohortMembershipRepository,
  val cohortMembershipInvitationRepository: CohortMembershipInvitationRepository,
  val cohortAccessService: CohortAccessService,
  val identityProfileService: IdentityProfileService,
) {

  /** Returns a page of current members. */
  @Transactional(readOnly = true)
  fun getMembers(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> {
    cohortAccessService.requireReadableActiveCohort(cohortId)

    return cohortMembershipRepository
      .findByCohortIdAndLeftAtIsNull(cohortId, pagination.toPageRequest(ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD))
      .toEnrichedResponse()
  }

  /** Returns a page of current faculty members. */
  @Transactional(readOnly = true)
  fun getFaculty(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> {
    cohortAccessService.requireReadableActiveCohort(cohortId)

    return cohortMembershipRepository
      .findByCohortIdAndRoleAndLeftAtIsNull(
        cohortId,
        CohortRole.FACULTY,
        pagination.toPageRequest(ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD),
      )
      .toEnrichedResponse()
  }

  /** Returns the current membership of [profileId] for internal callers. No access check. */
  @Transactional(readOnly = true)
  fun getCurrentMembership(cohortId: String, profileId: String): GetInternalCohortMembershipResponse {
    val membership = cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId, profileId)
      ?: throw NotFoundException("Membro não encontrado.")

    return GetInternalCohortMembershipResponse.fromMembership(membership)
  }

  /** Removes a member and revokes its pending invitations. The owner cannot be removed. */
  @Transactional
  fun removeMember(cohortId: String, profileId: String) {
    cohortAccessService.requireActiveFaculty(cohortId)
    val cohort = cohortRepository.findLockedById(cohortId)!!

    val membership = cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId, profileId)
      ?: throw NotFoundException("Membro não encontrado.")

    if (membership.profileId == cohort.ownerProfileId) {
      throw ConflictException("O responsável pela turma não pode ser removido.")
    }

    val now = Instant.now()

    membership.leftAt = now

    cohortMembershipInvitationRepository
      .findByCohortIdAndProfileIdAndState(cohortId, profileId, MembershipInvitationState.PENDING)
      .forEach {
        it.state = MembershipInvitationState.REVOKED
        it.revokedAt = now
      }
  }

  /** Creates the membership, or reactivates the membership of a removed member. */
  fun addMember(cohort: Cohort, profileId: String, role: CohortRole): CohortMembership {
    val membership = cohortMembershipRepository.findByCohortIdAndProfileId(cohort.id!!, profileId)
      ?: return cohortMembershipRepository.saveAndFlush(CohortMembership(cohort = cohort, profileId = profileId, role = role))

    if (membership.leftAt == null) throw ConflictException("O perfil já é membro da turma.")

    membership.role = role
    membership.joinedAt = Instant.now()
    membership.leftAt = null

    return membership
  }

  private fun Page<CohortMembership>.toEnrichedResponse(): PagedResponse<GetCohortMemberResponse> {
    val profiles = identityProfileService.findProfiles(content.map { it.profileId })
    return toPagedResponse { GetCohortMemberResponse.fromMembership(it, profiles[it.profileId]) }
  }

  companion object {
    private const val DEFAULT_SORT_FIELD = "joinedAt"
    private val ALLOWED_SORT_FIELDS = setOf("joinedAt")
  }
}