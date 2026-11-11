package io.correcta.cohort.service

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.ForbiddenException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import org.springframework.stereotype.Service

@Service
class CohortAccessService(
  val cohortRepository: CohortRepository,
  val cohortMembershipRepository: CohortMembershipRepository,
  val scopeEvaluator: ScopeEvaluatorComponent,
) {

  fun findVisibleCohort(cohortId: String): Cohort {
    val cohort = cohortRepository.findById(cohortId).orElse(null) ?: throw cohortNotFound()
    if (!isVisible(cohort)) throw cohortNotFound()

    return cohort
  }

  fun findVisibleCohortByPublicCode(publicCode: String): Cohort {
    val cohort = cohortRepository.findByPublicCode(publicCode) ?: throw cohortNotFound()
    if (!isVisible(cohort)) throw cohortNotFound()

    return cohort
  }

  fun findOwnedCohort(cohortId: String): Cohort {
    val cohort = findVisibleCohort(cohortId)

    if (cohort.ownerProfileId != scopeEvaluator.authenticatedProfile().profileId) {
      throw ForbiddenException("Apenas o responsável pela turma pode realizar esta operação.")
    }

    return cohort
  }

  fun requireActiveMember(cohortId: String): CohortMembership {
    val membership = cohortMembershipRepository
      .findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId, scopeEvaluator.authenticatedProfile().profileId)
      ?: throw cohortNotFound()

    if (membership.cohort.state != CohortState.ACTIVE) {
      throw ConflictException("A turma não está ativa.")
    }

    return membership
  }

  fun requireReadableActiveCohort(cohortId: String) {
    if (!isStaff()) {
      requireActiveMember(cohortId)
      return
    }

    val cohort = cohortRepository.findById(cohortId).orElse(null) ?: throw cohortNotFound()
    if (cohort.state != CohortState.ACTIVE) throw ConflictException("A turma não está ativa.")
  }

  fun requireActiveFaculty(cohortId: String): CohortMembership {
    val membership = requireActiveMember(cohortId)

    if (membership.role != CohortRole.FACULTY) {
      throw ForbiddenException("Apenas docentes da turma podem realizar esta operação.")
    }

    return membership
  }

  private fun isVisible(cohort: Cohort): Boolean {
    val profileId = scopeEvaluator.authenticatedProfile().profileId

    return isStaff() ||
      cohort.ownerProfileId == profileId ||
      cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull(cohort.id!!, profileId) != null
  }

  fun isStaff(): Boolean = scopeEvaluator.isProfileType("STAFF")

  private fun cohortNotFound(): NotFoundException = NotFoundException("Turma não encontrada.")
}