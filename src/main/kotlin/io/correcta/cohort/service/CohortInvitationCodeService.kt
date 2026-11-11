package io.correcta.cohort.service

import io.correcta.cohort.entities.CohortInvitationCode
import io.correcta.cohort.entities.CohortInvitationCodeState
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.ForbiddenException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.models.request.CreateInvitationCodeRequest
import io.correcta.cohort.models.response.GetCohortMembershipResponse
import io.correcta.cohort.models.response.GetInvitationCodeResponse
import io.correcta.cohort.repositories.CohortInvitationCodeRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.utils.ExpirationExt.withinLifetime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

@Service
class CohortInvitationCodeService(
  val cohortRepository: CohortRepository,
  val cohortInvitationCodeRepository: CohortInvitationCodeRepository,
  val cohortAccessService: CohortAccessService,
  val cohortMembershipService: CohortMembershipService,
  val identityProfileService: IdentityProfileService,
  val scopeEvaluator: ScopeEvaluatorComponent,
) {

  private val secureRandom = SecureRandom()

  @Transactional
  fun generateInvitationCode(cohortId: String, request: CreateInvitationCodeRequest): GetInvitationCodeResponse {
    val faculty = cohortAccessService.requireActiveFaculty(cohortId)
    val cohort = cohortRepository.findLockedById(cohortId)!!
    val expiresAt = request.expiresAt.withinLifetime(MAX_LIFETIME)
    val now = Instant.now()

    cohortInvitationCodeRepository.findByCohortIdAndState(cohortId, CohortInvitationCodeState.ACTIVE).forEach {
      if (it.effectiveState(now) == CohortInvitationCodeState.EXPIRED) {
        it.state = CohortInvitationCodeState.EXPIRED
      } else {
        it.state = CohortInvitationCodeState.REVOKED
        it.revokedAt = now
      }
    }

    val invitationCode = cohortInvitationCodeRepository.save(
      CohortInvitationCode(
        cohort = cohort,
        code = generateCode(),
        createdByProfileId = faculty.profileId,
        expiresAt = expiresAt,
      ),
    )

    return GetInvitationCodeResponse.fromInvitationCode(invitationCode)
  }

  @Transactional(readOnly = true)
  fun getInvitationCode(cohortId: String): GetInvitationCodeResponse {
    cohortAccessService.requireActiveFaculty(cohortId)
    return GetInvitationCodeResponse.fromInvitationCode(findValidCode(cohortId))
  }

  @Transactional
  fun revokeInvitationCode(cohortId: String) {
    cohortAccessService.requireActiveFaculty(cohortId)
    val invitationCode = findValidCode(cohortId)

    invitationCode.state = CohortInvitationCodeState.REVOKED
    invitationCode.revokedAt = Instant.now()
  }

  @Transactional
  fun joinCohort(code: String): GetCohortMembershipResponse {
    val profileId = scopeEvaluator.authenticatedProfile().profileId

    val invitationCode = cohortInvitationCodeRepository.findByCode(normalizeCode(code))
      ?.takeIf { it.effectiveState() == CohortInvitationCodeState.ACTIVE }
      ?: throw NotFoundException("Código de convite inválido ou expirado.")

    val cohort = cohortRepository.findLockedById(invitationCode.cohort.id!!)!!
    if (cohort.state != CohortState.ACTIVE) throw ConflictException("A turma não está ativa.")

    val profile = identityProfileService.findProfile(profileId)?.takeIf { it.isEligible() }
      ?: throw ForbiddenException("Seu perfil não está elegível para participar de turmas.")

    val membership = cohortMembershipService.addMember(cohort, profileId, CohortRole.valueOf(profile.type))

    return GetCohortMembershipResponse.fromMembership(membership)
  }

  private fun findValidCode(cohortId: String): CohortInvitationCode = cohortInvitationCodeRepository.findByCohortIdAndState(cohortId, CohortInvitationCodeState.ACTIVE)
    .firstOrNull { it.effectiveState() == CohortInvitationCodeState.ACTIVE }
    ?: throw NotFoundException("Nenhum código de convite válido encontrado para esta turma.")

  private fun generateCode(): String = generateSequence {
    (1..CODE_LENGTH).map { CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)] }.joinToString("")
  }.first { cohortInvitationCodeRepository.findByCode(it) == null }

  private fun normalizeCode(code: String): String = code.uppercase().filter { it.isLetterOrDigit() }

  companion object {
    private const val CODE_LENGTH = 8
    private const val CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val MAX_LIFETIME = Duration.ofDays(30)
  }
}