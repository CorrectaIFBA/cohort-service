-- Schema of every entity as of the move to Flyway.
--
-- Databases created before Flyway already hold these tables (Hibernate's
-- ddl-auto=update built them), so every statement is guarded: a fresh
-- database gets the full schema, an existing one only what it lacks.
-- Flyway baselines existing databases at version 0, so this script runs on both.

CREATE TABLE IF NOT EXISTS cohorts (
    id               VARCHAR(255)                NOT NULL,
    archived_at      TIMESTAMP(6) WITH TIME ZONE,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    description      VARCHAR(1024),
    owner_profile_id VARCHAR(255)                NOT NULL,
    slug             VARCHAR(160)                NOT NULL,
    state            VARCHAR(20)                 NOT NULL,
    title            VARCHAR(255)                NOT NULL,
    updated_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT cohorts_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cohort_slug UNIQUE (slug),
    CONSTRAINT cohorts_state_check CHECK (state IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_cohort_owner_profile ON cohorts (owner_profile_id);
CREATE INDEX IF NOT EXISTS idx_cohort_slug ON cohorts (slug);

CREATE TABLE IF NOT EXISTS cohort_memberships (
    id         VARCHAR(255)                NOT NULL,
    joined_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    left_at    TIMESTAMP(6) WITH TIME ZONE,
    profile_id VARCHAR(255)                NOT NULL,
    role       VARCHAR(20)                 NOT NULL,
    cohort_id  VARCHAR(255)                NOT NULL,
    CONSTRAINT cohort_memberships_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cohort_membership_cohort_profile UNIQUE (cohort_id, profile_id),
    CONSTRAINT cohort_memberships_role_check CHECK (role IN ('STUDENT', 'FACULTY')),
    CONSTRAINT fk_cohort_memberships_cohort FOREIGN KEY (cohort_id) REFERENCES cohorts (id)
);

CREATE INDEX IF NOT EXISTS idx_cohort_membership_cohort_role ON cohort_memberships (cohort_id, role);
CREATE INDEX IF NOT EXISTS idx_cohort_membership_profile ON cohort_memberships (profile_id);

CREATE TABLE IF NOT EXISTS cohort_membership_invitations (
    id                    VARCHAR(255)                NOT NULL,
    accepted_at           TIMESTAMP(6) WITH TIME ZONE,
    expires_at            TIMESTAMP(6) WITH TIME ZONE,
    invited_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    invited_by_profile_id VARCHAR(255)                NOT NULL,
    profile_id            VARCHAR(255)                NOT NULL,
    rejected_at           TIMESTAMP(6) WITH TIME ZONE,
    revoked_at            TIMESTAMP(6) WITH TIME ZONE,
    role                  VARCHAR(20)                 NOT NULL,
    state                 VARCHAR(20)                 NOT NULL,
    cohort_id             VARCHAR(255)                NOT NULL,
    CONSTRAINT cohort_membership_invitations_pkey PRIMARY KEY (id),
    CONSTRAINT cohort_membership_invitations_role_check CHECK (role IN ('STUDENT', 'FACULTY')),
    CONSTRAINT cohort_membership_invitations_state_check
        CHECK (state IN ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT fk_cohort_membership_invitations_cohort FOREIGN KEY (cohort_id) REFERENCES cohorts (id)
);

CREATE INDEX IF NOT EXISTS idx_cohort_membership_invitation_cohort ON cohort_membership_invitations (cohort_id);
CREATE INDEX IF NOT EXISTS idx_cohort_membership_invitation_cohort_profile ON cohort_membership_invitations (cohort_id, profile_id);
CREATE INDEX IF NOT EXISTS idx_cohort_membership_invitation_profile ON cohort_membership_invitations (profile_id);

CREATE TABLE IF NOT EXISTS cohort_invitation_codes (
    id                    VARCHAR(255)                NOT NULL,
    created_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by_profile_id VARCHAR(255)                NOT NULL,
    expires_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked_at            TIMESTAMP(6) WITH TIME ZONE,
    state                 VARCHAR(20)                 NOT NULL,
    cohort_id             VARCHAR(255)                NOT NULL,
    code                  VARCHAR(16)                 NOT NULL,
    CONSTRAINT cohort_invitation_codes_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cohort_invitation_code UNIQUE (code),
    CONSTRAINT cohort_invitation_codes_state_check CHECK (state IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT fk_cohort_invitation_codes_cohort FOREIGN KEY (cohort_id) REFERENCES cohorts (id)
);

CREATE INDEX IF NOT EXISTS idx_cohort_invitation_cohort ON cohort_invitation_codes (cohort_id);
CREATE INDEX IF NOT EXISTS idx_cohort_invitation_state_expires ON cohort_invitation_codes (state, expires_at);
