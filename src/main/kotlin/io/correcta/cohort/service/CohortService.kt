package io.correcta.cohort.service

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.exceptions.BadRequestException
import io.correcta.cohort.exceptions.ConflictException
import io.correcta.cohort.exceptions.NotFoundException
import io.correcta.cohort.models.request.ChangeCohortStatusRequest
import io.correcta.cohort.models.request.CohortFilterRequest
import io.correcta.cohort.models.request.CreateCohortRequest
import io.correcta.cohort.models.request.PaginationRequest
import io.correcta.cohort.models.request.UpdateCohortRequest
import io.correcta.cohort.models.response.GetCohortResponse
import io.correcta.cohort.models.response.PagedResponse
import io.correcta.cohort.repositories.CohortMembershipRepository
import io.correcta.cohort.repositories.CohortRepository
import io.correcta.cohort.repositories.specifications.CohortSpecifications
import io.correcta.cohort.security.components.ScopeEvaluatorComponent
import io.correcta.cohort.utils.PaginationExt.toPageRequest
import io.correcta.cohort.utils.PaginationExt.toPagedResponse
import io.correcta.cohort.utils.SlugExt.toSlug
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant

/** Cohort creation, listing and lifecycle. */
@Service
class CohortService(
  val cohortRepository: CohortRepository,
  val cohortMembershipRepository: CohortMembershipRepository,
  val cohortAccessService: CohortAccessService,
  val scopeEvaluator: ScopeEvaluatorComponent,
) {

  private val secureRandom = SecureRandom()

  /** Returns a page of cohorts. Staff see every cohort, other profiles see visible cohorts only. */
  @Transactional(readOnly = true)
  fun getCohorts(filter: CohortFilterRequest, pagination: PaginationRequest): PagedResponse<GetCohortResponse> {
    val profileId = scopeEvaluator.authenticatedProfile().profileId

    if (filter.createdAfter != null && filter.createdBefore != null && filter.createdAfter.isAfter(filter.createdBefore)) {
      throw BadRequestException("O parâmetro 'createdAfter' deve ser anterior ou igual a 'createdBefore'.")
    }

    val specification = Specification.allOf(
      listOfNotNull(
        CohortSpecifications.visibleTo(profileId).takeUnless { cohortAccessService.isStaff() },
        CohortSpecifications.matchesSearch(filter.search),
        CohortSpecifications.hasMember(filter.memberProfileId),
        CohortSpecifications.ownedBy(profileId.takeIf { filter.mine }),
        CohortSpecifications.hasState(filter.status),
        CohortSpecifications.createdFrom(filter.createdAfter),
        CohortSpecifications.createdUntil(filter.createdBefore),
      ),
    )

    return cohortRepository
      .findAll(specification, pagination.toPageRequest(ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD))
      .toPagedResponse(GetCohortResponse::fromCohort)
  }

  /** Returns a visible cohort by ID. [slug] must be the current slug. */
  @Transactional(readOnly = true)
  fun getCohort(cohortId: String, slug: String): GetCohortResponse {
    val cohort = cohortAccessService.findVisibleCohort(cohortId)
    if (cohort.slug != slug) throw NotFoundException("Turma não encontrada.")

    return GetCohortResponse.fromCohort(cohort)
  }

  /** Returns a visible cohort by public code. */
  @Transactional(readOnly = true)
  fun getCohortByPublicCode(publicCode: String): GetCohortResponse = GetCohortResponse.fromCohort(cohortAccessService.findVisibleCohortByPublicCode(publicCode))

  /** Creates a DRAFT cohort and adds the caller as its owner and faculty member. */
  @Transactional
  fun createCohort(request: CreateCohortRequest): GetCohortResponse {
    val profileId = scopeEvaluator.authenticatedProfile().profileId

    if (request.status != CohortState.DRAFT) {
      throw BadRequestException("Turmas só podem ser criadas com o status DRAFT.")
    }

    val name = request.name.trim()
    val slug = request.slug?.also { ensureSlugAllowed(it) } ?: generateSlug(name)

    val cohort = cohortRepository.save(
      Cohort(
        ownerProfileId = profileId,
        slug = slug,
        publicCode = generatePublicCode(),
        title = name,
        description = request.description?.trim(),
      ),
    )

    cohortMembershipRepository.save(
      CohortMembership(cohort = cohort, profileId = profileId, role = CohortRole.FACULTY),
    )

    return GetCohortResponse.fromCohort(cohort)
  }

  /** Updates name, slug and description. Archived cohorts are read-only. */
  @Transactional
  fun updateCohort(cohortId: String, request: UpdateCohortRequest): GetCohortResponse {
    val cohort = cohortAccessService.findOwnedCohort(cohortId)

    if (cohort.state == CohortState.ARCHIVED) {
      throw ConflictException("Turmas arquivadas não podem ser editadas.")
    }

    if (request.slug != cohort.slug) ensureSlugAllowed(request.slug)

    cohort.title = request.name.trim()
    cohort.slug = request.slug
    cohort.description = request.description?.trim()

    return GetCohortResponse.fromCohort(cohortRepository.saveAndFlush(cohort))
  }

  /** Moves the cohort to a new state allowed by [CohortState.canTransitionTo]. */
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

  /** Deletes a DRAFT cohort and its memberships. */
  @Transactional
  fun deleteCohort(cohortId: String) {
    val cohort = cohortAccessService.findOwnedCohort(cohortId)

    if (cohort.state != CohortState.DRAFT) {
      throw ConflictException("Apenas turmas em rascunho podem ser excluídas.")
    }

    cohortMembershipRepository.deleteByCohortId(cohortId)
    cohortRepository.delete(cohort)
  }

  private fun ensureSlugAllowed(slug: String) {
    if (slug in RESERVED_SLUGS) throw BadRequestException("O slug '$slug' é reservado.")
  }

  private fun generateSlug(name: String): String {
    val base = name.toSlug()

    return if (base in RESERVED_SLUGS) "$base-turma" else base
  }

  private fun generatePublicCode(): String = generateSequence {
    (1..PUBLIC_CODE_LENGTH).map { PUBLIC_CODE_ALPHABET[secureRandom.nextInt(PUBLIC_CODE_ALPHABET.length)] }.joinToString("")
  }.first { !cohortRepository.existsByPublicCode(it) }

  companion object {
    private const val DEFAULT_SORT_FIELD = "createdAt"
    private val ALLOWED_SORT_FIELDS = setOf("createdAt", "updatedAt", "slug")
    private val RESERVED_SLUGS = setOf("members", "faculty", "invitations", "invitation-code")
    private const val PUBLIC_CODE_LENGTH = 6
    private const val PUBLIC_CODE_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
  }
}