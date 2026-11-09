CREATE SEQUENCE business_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE business (
    business_id  bigint       PRIMARY KEY,
    user_id      bigint       NOT NULL REFERENCES entrepreneur (user_id),
    commune_id   bigint       NOT NULL REFERENCES commune (commune_id),
    name         varchar(100) NOT NULL,
    description  varchar(500) NOT NULL,
    photo_url    varchar(500),
    status       varchar(20)  NOT NULL DEFAULT 'PENDING',
    review_round integer      NOT NULL DEFAULT 1,
    approved_at  timestamptz,
    hidden_at    timestamptz,
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    deleted_at   timestamptz,
    version      bigint       NOT NULL DEFAULT 0,
    -- One business per entrepreneur.
    CONSTRAINT uk_business_user UNIQUE (user_id),
    CONSTRAINT ck_business_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_business_approved CHECK ((status = 'APPROVED') = (approved_at IS NOT NULL)),
    CONSTRAINT ck_business_review_round CHECK (review_round >= 1),
    CONSTRAINT ck_business_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_business_description CHECK (btrim(description) <> '')
);

CREATE INDEX ix_business_commune ON business (commune_id);
-- Public directory: the rows V_CATALOG can return.
CREATE INDEX ix_business_directory ON business (commune_id)
    WHERE status = 'APPROVED' AND deleted_at IS NULL AND hidden_at IS NULL;
-- Peer review queue, oldest change first.
CREATE INDEX ix_business_review_queue ON business (updated_at)
    WHERE status = 'PENDING' AND deleted_at IS NULL;
-- Accent and case insensitive partial-word search (LIKE '%term%').
CREATE INDEX ix_business_search ON business
    USING gin (immutable_unaccent(lower(name || ' ' || description)) gin_trgm_ops);

-- Constraints are deferred so a reordered or replaced list can be written row by row.
CREATE TABLE business_service (
    business_id bigint  NOT NULL REFERENCES business (business_id) ON DELETE CASCADE,
    service_id  bigint  NOT NULL REFERENCES service (service_id),
    sort_order  integer NOT NULL,
    CONSTRAINT pk_business_service PRIMARY KEY (business_id, service_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uk_business_service_order UNIQUE (business_id, sort_order) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_business_service_order CHECK (sort_order BETWEEN 1 AND 2)
);

CREATE INDEX ix_business_service_service ON business_service (service_id);

CREATE SEQUENCE business_review_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE business_review (
    review_id        bigint      PRIMARY KEY,
    business_id      bigint      NOT NULL REFERENCES business (business_id),
    reviewer_id      bigint      NOT NULL REFERENCES user_account (user_id),
    review_round     integer     NOT NULL,
    decision         varchar(10) NOT NULL,
    rejection_reason varchar(30),
    comment          varchar(500),
    reviewed_at      timestamptz NOT NULL,
    -- One vote per reviewer and round; the leading columns also serve "votes of this round".
    CONSTRAINT uk_business_review_vote UNIQUE (business_id, review_round, reviewer_id),
    CONSTRAINT ck_business_review_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ck_business_review_reason CHECK (rejection_reason IN
        ('INCOMPLETE_DATA', 'UNCLEAR_DESCRIPTION', 'INAPPROPRIATE_IMAGES', 'NOT_CIRCULAR', 'OTHER')),
    CONSTRAINT ck_business_review_rejection CHECK ((decision = 'REJECTED') = (rejection_reason IS NOT NULL)),
    CONSTRAINT ck_business_review_rejection_comment CHECK (decision <> 'REJECTED' OR (comment IS NOT NULL AND btrim(comment) <> '')),
    CONSTRAINT ck_business_review_round CHECK (review_round >= 1)
);

CREATE INDEX ix_business_review_reviewer ON business_review (reviewer_id);

-- Public directory. Never projects RUT, names or email of the owner.
CREATE VIEW v_catalog AS
SELECT b.business_id,
       b.name,
       b.description,
       b.photo_url,
       b.commune_id,
       c.name AS commune_name,
       c.region_id,
       r.name AS region_name,
       b.approved_at,
       immutable_unaccent(lower(b.name || ' ' || b.description)) AS search_text
FROM business b
         JOIN user_account u ON u.user_id = b.user_id
         JOIN commune c ON c.commune_id = b.commune_id
         JOIN region r ON r.region_id = c.region_id
WHERE b.status = 'APPROVED'
  AND b.deleted_at IS NULL
  AND b.hidden_at IS NULL
  AND u.status = 'ACTIVE';
