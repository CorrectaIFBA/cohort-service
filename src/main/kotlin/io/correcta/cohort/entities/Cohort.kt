package io.correcta.cohort.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * A class group owned by a faculty profile.
 *
 * The cohort URL is `{slug}-{publicCode}`. The slug can change, the public code cannot.
 */
@Entity
@Table(
  name = "cohorts",
  indexes = [
    Index(name = "idx_cohort_owner_profile", columnList = "owner_profile_id"),
    Index(name = "idx_cohort_slug", columnList = "slug"),
  ],
  uniqueConstraints = [
    UniqueConstraint(name = "uk_cohort_public_code", columnNames = ["public_code"]),
  ],
)
@EntityListeners(AuditingEntityListener::class)
class Cohort(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @Column(name = "owner_profile_id", nullable = false, updatable = false)
  var ownerProfileId: String,

  @Column(nullable = false, length = 160)
  var slug: String,

  @Column(name = "public_code", nullable = false, updatable = false, length = 6)
  var publicCode: String,

  @Column(nullable = false, length = 255)
  var title: String,

  @Column(length = 1024)
  var description: String? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  var state: CohortState = CohortState.DRAFT,

  var archivedAt: Instant? = null,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,
)