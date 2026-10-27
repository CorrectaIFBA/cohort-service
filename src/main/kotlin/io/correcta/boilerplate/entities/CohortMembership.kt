package io.correcta.boilerplate.entities

import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "cohort_memberships",
    indexes = [
        Index(
            name = "idx_cohort_membership_profile",
            columnList = "profile_id"
        ),
        Index(
            name = "idx_cohort_membership_cohort_role",
            columnList = "cohort_id, role"
        )
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_cohort_membership_cohort_profile",
            columnNames = ["cohort_id", "profile_id"]
        )
    ]
)
@EntityListeners(AuditingEntityListener::class)
class CohortMembership(
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    var cohort: Cohort,

    @Column(name = "profile_id", nullable = false, updatable = false)
    var profileId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: CohortRole,

    @CreatedDate
    @Column(nullable = false)
    var joinedAt: Instant? = null,

    var leftAt: Instant? = null
)
