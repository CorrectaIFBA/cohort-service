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
import io.correcta.cohort.models.response.GetCohortInvitationResponse
import io.correcta.cohort.models.response.GetCohortMembershipResponse
import io.correcta.cohort.models.response.GetInvitationCandidateResponse
import io.correcta.cohort.models.response.GetMyCohortInvitationResponse
import io.correcta.cohort.models.response.InvitationCandidateStatus
import io.correcta.cohort.models.response.PagedResponse
import io.correcta.cohort.repositories.CohortMembershipInvitationRepository
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.utils.ExpirationExt.withinLifetime
import io.correcta.cohort.utils.PaginationExt.toPageRequest
import io.correcta.cohort.utils.PaginationExt.toPagedResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/** Direct cohort invitations. */
@Service
class CohortInvitationService(
  val cohortRepository: CohortRepository,
  val cohortMembershipRepository: CohortMembershipRepository,
  val cohortMembershipInvitationRepository: CohortMembershipInvitationRepository,
  val cohortAccessService: CohortAccessService,
  val cohortMembershipService: CohortMembershipService,
  val identityProfileService: IdentityProfileService,
  val scopeEvaluator: ScopeEvaluatorComponent,
  val transactionalMailProducer: TransactionalMailProducer,
) {

  /** Invites an eligible profile and queues the invitation e-mail. */
  @Transactional
  fun sendInvitation(cohortId: String, request: CreateCohortInvitationRequest): GetCohortInvitationResponse {
    val faculty = cohortAccessService.requireActiveFaculty(cohortId)
    val cohort = cohortRepository.findLockedById(cohortId)!!
    val expiresAt = request.expiresAt.withinLifetime(MAX_LIFETIME)

    val profile = identityProfileService.findProfile(request.profileId)
      ?: throw NotFoundException("Perfil não encontrado.")

    if (!profile.isEligible()) {
      throw ConflictException("O perfil não está elegível para participar de turmas.")
    }

    if (profile.type != request.role.name) {
      throw BadRequestException("O papel ${request.role} não é compatível com o perfil ${profile.type}.")
    }

    if (cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId, request.profileId) != null) {
      throw ConflictException("O perfil já é membro da turma.")
    }

    if (findPendingInvitation(cohortId, request.profileId) != null) {
      throw ConflictException("Já existe um convite pendente para este perfil.")
    }

    val invitation = cohortMembershipInvitationRepository.save(
      CohortMembershipInvitation(
        cohort = cohort,
        profileId = request.profileId,
        role = request.role,
        invitedByProfileId = faculty.profileId,
        expiresAt = expiresAt,
      ),
    )

    val inviterName = identityProfileService.findProfile(faculty.profileId)?.displayName
    transactionalMailProducer.sendCohortInvitationEmail(invitation, profile, inviterName)

    return GetCohortInvitationResponse.fromInvitation(invitation, profile)
  }

  /** Returns the pending invitations of the caller to active cohorts. */
  @Transactional(readOnly = true)
  fun getMyInvitations(): List<GetMyCohortInvitationResponse> {
    val profileId = scopeEvaluator.authenticatedProfile().profileId
    val now = Instant.now()
    val invitations = cohortMembershipInvitationRepository
      .findByProfileIdAndStateOrderByInvitedAtDesc(profileId, MembershipInvitationState.PENDING)
      .filter { it.effectiveState(now) == MembershipInvitationState.PENDING && it.cohort.state == CohortState.ACTIVE }

    if (invitations.isEmpty()) return emptyList()

    val inviters = identityProfileService.findProfiles(invitations.map { it.invitedByProfileId })

    return invitations.map { GetMyCohortInvitationResponse.fromInvitation(it, inviters[it.invitedByProfileId]) }
  }

  /** Returns a page of invitations of the cohort, in any state. */
  @Transactional(readOnly = true)
  fun getInvitations(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortInvitationResponse> {
    cohortAccessService.requireActiveFaculty(cohortId)

    val page = cohortMembershipInvitationRepository
      .findByCohortId(cohortId, pagination.toPageRequest(ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD))
    val profiles = identityProfileService.findProfiles(page.content.map { it.profileId })

    return page.toPagedResponse { GetCohortInvitationResponse.fromInvitation(it, profiles[it.profileId]) }
  }

  /** Searches eligible profiles and tells for each one if it is a member, invited or available. */
  @Transactional(readOnly = true)
  fun searchCandidates(cohortId: String, search: String, role: CohortRole?): List<GetInvitationCandidateResponse> {
    cohortAccessService.requireActiveFaculty(cohortId)

    val term = search.trim()

    if (term.length < MIN_SEARCH_LENGTH) {
      throw BadRequestException("Digite pelo menos $MIN_SEARCH_LENGTH caracteres para buscar.")
    }

    val profiles = identityProfileService
      .searchProfiles(term, role?.name, MAX_CANDIDATES)
      .filter { it.isEligible() }

    if (profiles.isEmpty()) return emptyList()

    val profileIds = profiles.map { it.profileId }
    val members = cohortMembershipRepository
      .findByCohortIdAndProfileIdInAndLeftAtIsNull(cohortId, profileIds)
      .mapTo(HashSet()) { it.profileId }
    val now = Instant.now()
    val invited = cohortMembershipInvitationRepository
      .findByCohortIdAndProfileIdInAndState(cohortId, profileIds, MembershipInvitationState.PENDING)
      .filter { it.effectiveState(now) == MembershipInvitationState.PENDING }
      .mapTo(HashSet()) { it.profileId }

    return profiles.map { profile ->
      val status = when (profile.profileId) {
        in members -> InvitationCandidateStatus.MEMBER
        in invited -> InvitationCandidateStatus.INVITED
        else -> InvitationCandidateStatus.AVAILABLE
      }

      GetInvitationCandidateResponse.fromProfile(profile, status)
    }
  }

  /** Revokes the pending invitation of [profileId]. */
  @Transactional
  fun revokeInvitation(cohortId: String, profileId: String) {
    cohortAccessService.requireActiveFaculty(cohortId)

    val invitation = findPendingInvitation(cohortId, profileId)
      ?: throw NotFoundException("Convite pendente não encontrado.")

    invitation.state = MembershipInvitationState.REVOKED
    invitation.revokedAt = Instant.now()
  }

  /**
   * Accepts the caller pending invitation.
   *
   * Returns the membership and `true` when it was created now. A repeated accept returns `false`.
   */
  @Transactional
  fun acceptInvitation(cohortId: String): Pair<GetCohortMembershipResponse, Boolean> {
    val profileId = scopeEvaluator.authenticatedProfile().profileId
    val cohort = cohortRepository.findLockedById(cohortId) ?: throw invitationNotFound()

    val invitation = findPendingInvitation(cohortId, profileId)
    if (invitation == null) {
      val membership = cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId, profileId)
      val accepted = cohortMembershipInvitationRepository
        .existsByCohortIdAndProfileIdAndState(cohortId, profileId, MembershipInvitationState.ACCEPTED)

      if (membership != null && accepted) return GetCohortMembershipResponse.fromMembership(membership) to false
      throw invitationNotFound()
    }

    if (cohort.state != CohortState.ACTIVE) throw ConflictException("A turma não está ativa.")

    identityProfileService.findProfile(profileId)?.takeIf { it.isEligible() }
      ?: throw ForbiddenException("Seu perfil não está elegível para participar de turmas.")

    val membership = cohortMembershipService.addMember(cohort, profileId, invitation.role)

    invitation.state = MembershipInvitationState.ACCEPTED
    invitation.acceptedAt = Instant.now()

    return GetCohortMembershipResponse.fromMembership(membership) to true
  }

  /** Returns the pending invitation and marks expired ones as [MembershipInvitationState.EXPIRED]. */
  private fun findPendingInvitation(cohortId: String, profileId: String): CohortMembershipInvitation? {
    val now = Instant.now()
    val (pending, expired) = cohortMembershipInvitationRepository
      .findByCohortIdAndProfileIdAndState(cohortId, profileId, MembershipInvitationState.PENDING)
      .partition { it.effectiveState(now) == MembershipInvitationState.PENDING }

    expired.forEach { it.state = MembershipInvitationState.EXPIRED }

    return pending.firstOrNull()
  }

  private fun invitationNotFound(): NotFoundException = NotFoundException("Convite não encontrado.")

  companion object {
    private const val DEFAULT_SORT_FIELD = "invitedAt"
    private val ALLOWED_SORT_FIELDS = setOf("invitedAt", "expiresAt")
    private val MAX_LIFETIME = Duration.ofDays(30)
    private const val MIN_SEARCH_LENGTH = 2
    private const val MAX_CANDIDATES = 10
  }
}