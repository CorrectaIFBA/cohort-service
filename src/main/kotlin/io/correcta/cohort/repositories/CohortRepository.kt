package io.correcta.cohort.repositories

import io.correcta.cohort.entities.Cohort
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CohortRepository :
  JpaRepository<Cohort, String>,
  JpaSpecificationExecutor<Cohort> {
  fun existsByPublicCode(publicCode: String): Boolean

  fun findByPublicCode(publicCode: String): Cohort?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Cohort c where c.id = :id")
  fun findLockedById(id: String): Cohort?
}