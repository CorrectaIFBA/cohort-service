package io.correcta.boilerplate.repositories

import io.correcta.boilerplate.entities.CohortInvitationCode
import io.correcta.boilerplate.entities.CohortInvitationCodeState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CohortInvitationCodeRepository : JpaRepository<CohortInvitationCode, String> {
    fun findByCohortIdAndState(cohortId: String, state: CohortInvitationCodeState): List<CohortInvitationCode>

    fun findByCode(code: String): CohortInvitationCode?
}
