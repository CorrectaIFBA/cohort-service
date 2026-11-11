package io.correcta.cohort.repositories.specifications

import io.correcta.cohort.entities.Cohort
import io.correcta.cohort.entities.CohortMembership
import io.correcta.cohort.entities.CohortState
import io.correcta.cohort.repositories.specifications.SpecificationExt.LIKE_ESCAPE_CHAR
import io.correcta.cohort.repositories.specifications.SpecificationExt.toContainsPattern
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Subquery
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

object CohortSpecifications {

  fun visibleTo(profileId: String): Specification<Cohort> = Specification { root, query, builder ->
    builder.or(
      builder.equal(root.get<String>("ownerProfileId"), profileId),
      root.get<String>("id").`in`(currentCohortIds(query!!, builder, profileId)),
    )
  }

  fun hasMember(profileId: String?): Specification<Cohort>? {
    if (profileId.isNullOrBlank()) return null

    return Specification { root, query, builder ->
      root.get<String>("id").`in`(currentCohortIds(query!!, builder, profileId))
    }
  }

  fun matchesSearch(search: String?): Specification<Cohort>? {
    if (search.isNullOrBlank()) return null

    return Specification { root, _, builder ->
      val pattern = search.toContainsPattern()
      builder.or(
        builder.like(builder.lower(root.get("title")), pattern, LIKE_ESCAPE_CHAR),
        builder.like(builder.lower(root.get("slug")), pattern, LIKE_ESCAPE_CHAR),
      )
    }
  }

  private fun currentCohortIds(query: CriteriaQuery<*>, builder: CriteriaBuilder, profileId: String): Subquery<String> {
    val memberships = query.subquery(String::class.java)
    val membership = memberships.from(CohortMembership::class.java)

    return memberships
      .select(membership.get<Cohort>("cohort").get("id"))
      .where(
        builder.equal(membership.get<String>("profileId"), profileId),
        builder.isNull(membership.get<Instant>("leftAt")),
      )
  }

  fun ownedBy(ownerProfileId: String?): Specification<Cohort>? {
    if (ownerProfileId == null) return null

    return Specification { root, _, builder ->
      builder.equal(root.get<String>("ownerProfileId"), ownerProfileId)
    }
  }

  fun hasState(state: CohortState?): Specification<Cohort>? {
    if (state == null) return null

    return Specification { root, _, builder ->
      builder.equal(root.get<CohortState>("state"), state)
    }
  }

  fun createdFrom(instant: Instant?): Specification<Cohort>? {
    if (instant == null) return null

    return Specification { root, _, builder ->
      builder.greaterThanOrEqualTo(root.get("createdAt"), instant)
    }
  }

  fun createdUntil(instant: Instant?): Specification<Cohort>? {
    if (instant == null) return null

    return Specification { root, _, builder ->
      builder.lessThanOrEqualTo(root.get("createdAt"), instant)
    }
  }
}