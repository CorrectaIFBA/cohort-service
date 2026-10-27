package io.correcta.boilerplate.entities

import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "cohorts",
    indexes = [
        Index(name = "idx_cohort_owner_profile", columnList = "owner_profile_id"),
        Index(name = "idx_cohort_slug", columnList = "slug")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cohort_slug", columnNames = ["slug"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Cohort(
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    /**
     * Identity-service profile ID of the cohort owner.
     */
    @Column(name = "owner_profile_id", nullable = false, updatable = false)
    var ownerProfileId: String,

    @Column(nullable = false, length = 160)
    var slug: String,

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
    var updatedAt: Instant? = null
)