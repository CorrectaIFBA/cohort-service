package io.correcta.boilerplate.repositories

import io.correcta.boilerplate.entities.Cohort
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CohortRepository : JpaRepository<Cohort, String>, JpaSpecificationExecutor<Cohort> {
    fun existsBySlug(slug: String): Boolean

    fun existsBySlugAndIdNot(slug: String, id: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cohort c where c.id = :id")
    fun findLockedById(id: String): Cohort?
}
