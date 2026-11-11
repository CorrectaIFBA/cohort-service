package io.correcta.cohort.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(
  name = "cohort_invitation_codes",
  uniqueConstraints = [
    UniqueConstraint(
      name = "uk_cohort_invitation_code",
      columnNames = ["code"],
    ),
  ],
  indexes = [
    Index(
      name = "idx_cohort_invitation_cohort",
      columnList = "cohort_id",
    ),
    Index(
      name = "idx_cohort_invitation_state_expires",
      columnList = "state, expires_at",
    ),
  ],
)
@EntityListeners(AuditingEntityListener::class)
class CohortInvitationCode(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cohort_id", nullable = false)
  var cohort: Cohort,

  @Column(nullable = false, updatable = false, length = 16)
  var code: String,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  var state: CohortInvitationCodeState = CohortInvitationCodeState.ACTIVE,

  @Column(nullable = false, updatable = false)
  var createdByProfileId: String,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @Column(name = "expires_at", nullable = false)
  var expiresAt: Instant,

  var revokedAt: Instant? = null,
) {

  fun effectiveState(now: Instant = Instant.now()): CohortInvitationCodeState {
    val expired = !expiresAt.isAfter(now)
    return if (state == CohortInvitationCodeState.ACTIVE && expired) CohortInvitationCodeState.EXPIRED else state
  }
}