CREATE SEQUENCE user_account_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE user_account (
    user_id        bigint       PRIMARY KEY,
    email          varchar(254) NOT NULL,
    password_hash  varchar(100) NOT NULL,
    role           varchar(20)  NOT NULL,
    alias          varchar(40),
    status         varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    consent_at     timestamptz  NOT NULL,
    created_at     timestamptz  NOT NULL,
    updated_at     timestamptz  NOT NULL,
    deactivated_at timestamptz,
    deleted_at     timestamptz,
    version        bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_account_role CHECK (role IN ('CUSTOMER', 'ENTREPRENEUR', 'ADMINISTRATOR')),
    CONSTRAINT ck_user_account_status CHECK (status IN ('ACTIVE', 'DEACTIVATED', 'SUSPENDED', 'DELETED')),
    CONSTRAINT ck_user_account_email_lower CHECK (email = lower(email)),
    CONSTRAINT ck_user_account_deactivated CHECK ((status = 'DEACTIVATED') = (deactivated_at IS NOT NULL)),
    CONSTRAINT ck_user_account_deleted CHECK ((status = 'DELETED') = (deleted_at IS NOT NULL))
);

CREATE UNIQUE INDEX uk_user_account_email ON user_account (lower(email));
CREATE INDEX ix_user_account_role_status ON user_account (role, status);

CREATE TABLE entrepreneur (
    user_id       bigint      PRIMARY KEY REFERENCES user_account (user_id),
    age_range_id  bigint      NOT NULL REFERENCES age_range (age_range_id),
    gender_id     bigint      NOT NULL REFERENCES gender (gender_id),
    rut           varchar(10),
    first_name    varchar(60),
    last_name     varchar(60),
    anonymized_at timestamptz,
    CONSTRAINT uk_entrepreneur_rut UNIQUE (rut),
    CONSTRAINT ck_entrepreneur_rut_format CHECK (rut ~ '^[0-9]{7,8}-[0-9K]$'),
    -- Personal data is present until the profile is anonymized, and gone afterwards.
    CONSTRAINT ck_entrepreneur_identity CHECK (
        (anonymized_at IS NULL AND rut IS NOT NULL AND first_name IS NOT NULL AND last_name IS NOT NULL)
        OR (anonymized_at IS NOT NULL AND rut IS NULL AND first_name IS NULL AND last_name IS NULL))
);

CREATE INDEX ix_entrepreneur_age_range ON entrepreneur (age_range_id);
CREATE INDEX ix_entrepreneur_gender ON entrepreneur (gender_id);

CREATE SEQUENCE refresh_token_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE refresh_token (
    token_id   bigint      PRIMARY KEY,
    user_id    bigint      NOT NULL REFERENCES user_account (user_id),
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_token_user_active ON refresh_token (user_id) WHERE revoked_at IS NULL;

CREATE SEQUENCE password_reset_code_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE password_reset_code (
    code_id         bigint      PRIMARY KEY,
    user_id         bigint      NOT NULL REFERENCES user_account (user_id),
    code_hash       varchar(64) NOT NULL,
    expires_at      timestamptz NOT NULL,
    used_at         timestamptz,
    failed_attempts integer     NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL,
    CONSTRAINT ck_password_reset_code_attempts CHECK (failed_attempts BETWEEN 0 AND 5)
);

CREATE INDEX ix_password_reset_code_user_open ON password_reset_code (user_id, created_at DESC) WHERE used_at IS NULL;
