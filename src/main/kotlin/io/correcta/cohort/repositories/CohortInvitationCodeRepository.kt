package io.correcta.cohort.repositories

import io.correcta.cohort.entities.CohortInvitationCode
import io.correcta.cohort.entities.CohortInvitationCodeState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CohortInvitationCodeRepository : JpaRepository<CohortInvitationCode, String> {
  fun findByCohortIdAndState(cohortId: String, state: CohortInvitationCodeState): List<CohortInvitationCode>

  fun findByCode(code: String): CohortInvitationCode?
}