package io.correcta.boilerplate.service

import io.correcta.boilerplate.entities.Cohort
import io.correcta.boilerplate.entities.CohortMembership
import io.correcta.boilerplate.entities.CohortRole
import io.correcta.boilerplate.entities.CohortState
import io.correcta.boilerplate.exceptions.BadRequestException
import io.correcta.boilerplate.exceptions.ConflictException
import io.correcta.boilerplate.exceptions.NotFoundException
import io.correcta.boilerplate.models.request.ChangeCohortStatusRequest
import io.correcta.boilerplate.models.request.CohortFilterRequest
import io.correcta.boilerplate.models.request.CreateCohortRequest
import io.correcta.boilerplate.models.request.PaginationRequest
import io.correcta.boilerplate.models.request.UpdateCohortRequest
import io.correcta.boilerplate.models.response.GetCohortResponse
import io.correcta.boilerplate.models.response.PagedResponse
import io.correcta.boilerplate.repositories.CohortMembershipRepository
import io.correcta.boilerplate.repositories.CohortRepository
import io.correcta.boilerplate.repositories.specifications.CohortSpecifications
import io.correcta.boilerplate.security.components.ScopeEvaluatorComponent
import io.correcta.boilerplate.utils.PaginationExt.toPageRequest
import io.correcta.boilerplate.utils.PaginationExt.toPagedResponse
import io.correcta.boilerplate.utils.SlugExt.toSlug
import java.time.Instant
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CohortService(
    val cohortRepository: CohortRepository,
    val cohortMembershipRepository: CohortMembershipRepository,
    val cohortAccessService: CohortAccessService,
    val scopeEvaluator: ScopeEvaluatorComponent,
) {

    @Transactional(readOnly = true)
    fun getCohorts(filter: CohortFilterRequest, pagination: PaginationRequest): PagedResponse<GetCohortResponse> {
        val profileId = scopeEvaluator.authenticatedProfile().profileId

        if (filter.createdAfter != null && filter.createdBefore != null && filter.createdAfter.isAfter(filter.createdBefore)) {
            throw BadRequestException("O parâmetro 'createdAfter' deve ser anterior ou igual a 'createdBefore'.")
        }

        val specification = Specification.allOf(
            listOfNotNull(
                // Staff oversee every cohort; anyone else sees what they own or belong to.
                CohortSpecifications.visibleTo(profileId).takeUnless { cohortAccessService.isStaff() },
                CohortSpecifications.matchesSearch(filter.search),
                CohortSpecifications.hasMember(filter.memberProfileId),
                CohortSpecifications.ownedBy(profileId.takeIf { filter.mine }),
                CohortSpecifications.hasState(filter.status),
                CohortSpecifications.createdFrom(filter.createdAfter),
                CohortSpecifications.createdUntil(filter.createdBefore)
            )
        )

        return cohortRepository
            .findAll(specification, pagination.toPageRequest(ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD))
            .toPagedResponse(GetCohortResponse::fromCohort)
    }

    @Transactional(readOnly = true)
    fun getCohort(cohortId: String, slug: String): GetCohortResponse {
        val cohort = cohortAccessService.findVisibleCohort(cohortId)
        if (cohort.slug != slug) throw NotFoundException("Turma não encontrada.")

        return GetCohortResponse.fromCohort(cohort)
    }

    @Transactional
    fun createCohort(request: CreateCohortRequest): GetCohortResponse {
        val profileId = scopeEvaluator.authenticatedProfile().profileId

        if (request.status != CohortState.DRAFT) {
            throw BadRequestException("Turmas só podem ser criadas com o status DRAFT.")
        }

        val name = request.name.trim()
        val slug = request.slug?.also { ensureSlugAvailable(it) } ?: generateSlug(name)

        val cohort = cohortRepository.save(
            Cohort(
                ownerProfileId = profileId,
                slug = slug,
                title = name,
                description = request.description?.trim()
            )
        )

        cohortMembershipRepository.save(
            CohortMembership(cohort = cohort, profileId = profileId, role = CohortRole.FACULTY)
        )

        return GetCohortResponse.fromCohort(cohort)
    }

    @Transactional
    fun updateCohort(cohortId: String, request: UpdateCohortRequest): GetCohortResponse {
        val cohort = cohortAccessService.findOwnedCohort(cohortId)

        if (cohort.state == CohortState.ARCHIVED) {
            throw ConflictException("Turmas arquivadas não podem ser editadas.")
        }

        if (request.slug != cohort.slug) ensureSlugAvailable(request.slug, cohortId)

        cohort.title = request.name.trim()
        cohort.slug = request.slug
        cohort.description = request.description?.trim()

        return GetCohortResponse.fromCohort(cohortRepository.saveAndFlush(cohort))
    }

    @Transactional
    fun changeCohortStatus(cohortId: String, request: ChangeCohortStatusRequest): GetCohortResponse {
        val cohort = cohortAccessService.findOwnedCohort(cohortId)

        if (!cohort.state.canTransitionTo(request.status)) {
            throw ConflictException("A turma não pode transicionar de ${cohort.state} para ${request.status}.")
        }

        cohort.state = request.status
        cohort.archivedAt = if (request.status == CohortState.ARCHIVED) Instant.now() else null

        return GetCohortResponse.fromCohort(cohortRepository.saveAndFlush(cohort))
    }

    @Transactional
    fun deleteCohort(cohortId: String) {
        val cohort = cohortAccessService.findOwnedCohort(cohortId)

        if (cohort.state != CohortState.DRAFT) {
            throw ConflictException("Apenas turmas em rascunho podem ser excluídas.")
        }

        cohortMembershipRepository.deleteByCohortId(cohortId)
        cohortRepository.delete(cohort)
    }

    private fun ensureSlugAvailable(slug: String, cohortId: String? = null) {
        if (slug in RESERVED_SLUGS) throw BadRequestException("O slug '$slug' é reservado.")

        val taken = if (cohortId == null) cohortRepository.existsBySlug(slug) else cohortRepository.existsBySlugAndIdNot(slug, cohortId)
        if (taken) throw ConflictException("Já existe uma turma com o slug '$slug'.")
    }

    private fun generateSlug(name: String): String {
        val base = name.toSlug()

        return generateSequence(1) { it + 1 }
            .map { if (it == 1) base else "$base-$it" }
            .first { it !in RESERVED_SLUGS && !cohortRepository.existsBySlug(it) }
    }

    companion object {
        private const val DEFAULT_SORT_FIELD = "createdAt"
        private val ALLOWED_SORT_FIELDS = setOf("createdAt", "updatedAt", "slug")
        private val RESERVED_SLUGS = setOf("members", "faculty", "invitations", "invitation-code")
    }
}
