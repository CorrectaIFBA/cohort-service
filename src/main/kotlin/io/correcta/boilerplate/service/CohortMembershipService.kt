package io.correcta.boilerplate.service

import io.correcta.boilerplate.entities.Cohort
import io.correcta.boilerplate.entities.CohortMembership
import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.exceptions.ConflictException
import io.correcta.boilerplate.exceptions.NotFoundException
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.response.GetCohortMemberResponse
import io.correcta.boilerplate.models.response.GetInternalCohortMembershipResponse
import io.correcta.boilerplate.models.response.PagedResponse
import io.correcta.boilerplate.repositories.CohortMembershipRepository
import io.correcta.boilerplate.repositories.CohortRepository
import io.correcta.boilerplate.utils.PaginationExt.toPageRequest
import io.correcta.boilerplate.utils.PaginationExt.toPagedResponse
import java.time.Instant
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CohortMembershipService(
    val cohortRepository: CohortRepository,
    val cohortMembershipRepository: CohortMembershipRepository,
    val cohortAccessService: CohortAccessService,
    val identityProfileService: IdentityProfileService,
) {

    @Transactional(readOnly = true)
    fun getMembers(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> {
        cohortAccessService.requireReadableActiveCohort(cohortId)

        return cohortMembershipRepository
            .findByCohortIdAndLeftAtIsNull(cohortId, pagination.toPageRequest(ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD))
            .toEnrichedResponse()
    }

    @Transactional(readOnly = true)
    fun getFaculty(cohortId: String, pagination: PaginationRequest): PagedResponse<GetCohortMemberResponse> {
        cohortAccessService.requireReadableActiveCohort(cohortId)

        return cohortMembershipRepository
            .findByCohortIdAndRoleAndLeftAtIsNull(
                cohortId,
                CohortRole.FACULTY,
                pagination.toPageRequest(ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD)
            )
            .toEnrichedResponse()
    }

    @Transactional(readOnly = true)
    fun getCurrentMembership(cohortId: String, profileId: String): GetInternalCohortMembershipResponse {
        val membership = cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId, profileId)
            ?: throw NotFoundException("Membro não encontrado.")

        return GetInternalCohortMembershipResponse.fromMembership(membership)
    }

    @Transactional
    fun removeMember(cohortId: String, profileId: String) {
        cohortAccessService.requireActiveFaculty(cohortId)
        val cohort = cohortRepository.findLockedById(cohortId)!!

        val membership = cohortMembershipRepository.findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId, profileId)
            ?: throw NotFoundException("Membro não encontrado.")

        if (membership.profileId == cohort.ownerProfileId) {
            throw ConflictException("O responsável pela turma não pode ser removido.")
        }

        membership.leftAt = Instant.now()
    }

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
