package io.correcta.cohort.repositories

import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Persistence of [CohortMembership].
 *
 * Methods that end in `LeftAtIsNull` return current members only.
 */
@Repository
interface CohortMembershipRepository : JpaRepository<CohortMembership, String> {
  fun findByCohortIdAndProfileId(cohortId: String, profileId: String): CohortMembership?

  fun findByCohortIdAndProfileIdAndLeftAtIsNull(cohortId: String, profileId: String): CohortMembership?

  fun findByCohortIdAndLeftAtIsNull(cohortId: String, pageable: Pageable): Page<CohortMembership>

  fun findByCohortIdAndRoleAndLeftAtIsNull(cohortId: String, role: CohortRole, pageable: Pageable): Page<CohortMembership>

  fun findByCohortIdAndProfileIdInAndLeftAtIsNull(cohortId: String, profileIds: Collection<String>): List<CohortMembership>

  fun deleteByCohortId(cohortId: String)
}