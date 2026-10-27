package io.correcta.boilerplate.service

import io.correcta.boilerplate.entities.Cohort
import io.correcta.boilerplate.entities.CohortMembership
import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.entities.CohortState
import io.correcta.boilerplate.exceptions.ConflictException
import io.correcta.boilerplate.exceptions.ForbiddenException
import io.correcta.boilerplate.exceptions.NotFoundException
import io.correcta.boilerplate.repositories.CohortMembershipRepository
import io.correcta.boilerplate.repositories.CohortRepository
import io.correcta.boilerplate.security.components.ScopeEvaluatorComponent
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

    /** Current members, and staff, may read an active cohort's member lists. */
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

    fun isStaff(): Boolean {
        return scopeEvaluator.isProfileType("STAFF")
    }

    private fun cohortNotFound(): NotFoundException {
        return NotFoundException("Turma não encontrada.")
    }
}
