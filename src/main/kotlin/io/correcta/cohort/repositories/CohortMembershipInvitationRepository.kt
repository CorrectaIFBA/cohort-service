package io.correcta.cohort.repositories

import io.correcta.cohort.entities.CohortMembershipInvitation
import io.correcta.cohort.entities.MembershipInvitationState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** Persistence of [CohortMembershipInvitation]. */
@Repository
interface CohortMembershipInvitationRepository : JpaRepository<CohortMembershipInvitation, String> {
  fun findByCohortIdAndProfileIdAndState(
    cohortId: String,
    profileId: String,
    state: MembershipInvitationState,
  ): List<CohortMembershipInvitation>

  fun existsByCohortIdAndProfileIdAndState(cohortId: String, profileId: String, state: MembershipInvitationState): Boolean

  fun findByCohortIdAndProfileIdInAndState(
    cohortId: String,
    profileIds: Collection<String>,
    state: MembershipInvitationState,
  ): List<CohortMembershipInvitation>

  @EntityGraph(attributePaths = ["cohort"])
  fun findByProfileIdAndStateOrderByInvitedAtDesc(
    profileId: String,
    state: MembershipInvitationState,
  ): List<CohortMembershipInvitation>

  fun findByCohortId(cohortId: String, pageable: Pageable): Page<CohortMembershipInvitation>
}