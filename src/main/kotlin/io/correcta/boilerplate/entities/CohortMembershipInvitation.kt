package io.correcta.boilerplate.entities

import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "cohort_membership_invitations",
    indexes = [
        Index(
            name = "idx_cohort_membership_invitation_profile",
            columnList = "profile_id"
        ),
        Index(
            name = "idx_cohort_membership_invitation_cohort",
            columnList = "cohort_id"
        ),
        Index(
            name = "idx_cohort_membership_invitation_cohort_profile",
            columnList = "cohort_id, profile_id"
        )
    ]
)
@EntityListeners(AuditingEntityListener::class)
class CohortMembershipInvitation(
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

    @Column(nullable = false, updatable = false)
    var invitedByProfileId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var state: MembershipInvitationState = MembershipInvitationState.PENDING,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var invitedAt: Instant? = null,

    var expiresAt: Instant? = null,

    var acceptedAt: Instant? = null,

    var rejectedAt: Instant? = null,

    var revokedAt: Instant? = null
) {

    fun effectiveState(now: Instant = Instant.now()): MembershipInvitationState {
        val expired = expiresAt?.let { !it.isAfter(now) } ?: false
        return if (state == MembershipInvitationState.PENDING && expired) MembershipInvitationState.EXPIRED else state
    }
}
