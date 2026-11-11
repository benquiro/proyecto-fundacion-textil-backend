CREATE SEQUENCE outreach_content_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE outreach_content (
    content_id     bigint        PRIMARY KEY,
    author_id      bigint        NOT NULL REFERENCES user_account (user_id),
    type           varchar(10)   NOT NULL,
    title          varchar(150),
    body           varchar(5000),
    image_url      varchar(500),
    event_at       timestamptz,
    event_location varchar(200),
    created_at     timestamptz   NOT NULL,
    updated_at     timestamptz   NOT NULL,
    deleted_at     timestamptz,
    version        bigint        NOT NULL DEFAULT 0,
    CONSTRAINT ck_outreach_content_type CHECK (type IN ('TEXT', 'IMAGE', 'ARTICLE', 'EVENT')),
    -- Fields each type needs; event fields exist only on events.
    CONSTRAINT ck_outreach_content_text CHECK (type <> 'TEXT' OR body IS NOT NULL),
    CONSTRAINT ck_outreach_content_image CHECK (type <> 'IMAGE' OR image_url IS NOT NULL),
    CONSTRAINT ck_outreach_content_article CHECK (type <> 'ARTICLE' OR (title IS NOT NULL AND body IS NOT NULL)),
    CONSTRAINT ck_outreach_content_event CHECK (
        (type = 'EVENT' AND title IS NOT NULL AND event_at IS NOT NULL)
        OR (type <> 'EVENT' AND event_at IS NULL AND event_location IS NULL))
);

-- Home feed: newest non-deleted first.
CREATE INDEX ix_outreach_content_feed ON outreach_content (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_outreach_content_author ON outreach_content (author_id);

-- Simulated object storage: files live on local disk, the row keeps their metadata.
CREATE TABLE media_file (
    file_id      uuid         PRIMARY KEY,
    owner_id     bigint       NOT NULL REFERENCES user_account (user_id),
    content_type varchar(20)  NOT NULL,
    size_bytes   bigint       NOT NULL,
    storage_path varchar(300) NOT NULL,
    created_at   timestamptz  NOT NULL,
    CONSTRAINT uk_media_file_path UNIQUE (storage_path),
    CONSTRAINT ck_media_file_type CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT ck_media_file_size CHECK (size_bytes > 0 AND size_bytes <= 5242880)
);

CREATE INDEX ix_media_file_owner ON media_file (owner_id);

CREATE SEQUENCE analytics_event_seq START WITH 1000 INCREMENT BY 50;

-- Append-only usage events that feed the admin metrics (searches, views, contact clicks).
CREATE TABLE analytics_event (
    event_id      bigint       PRIMARY KEY,
    type          varchar(20)  NOT NULL,
    actor_user_id bigint       REFERENCES user_account (user_id),
    business_id   bigint       REFERENCES business (business_id),
    product_id    bigint       REFERENCES product (product_id),
    region_id     bigint       REFERENCES region (region_id),
    commune_id    bigint       REFERENCES commune (commune_id),
    service_id    bigint       REFERENCES service (service_id),
    search_term   varchar(100),
    occurred_at   timestamptz  NOT NULL,
    CONSTRAINT ck_analytics_event_type CHECK (type IN ('DIRECTORY_SEARCH', 'BUSINESS_VIEW', 'PRODUCT_VIEW', 'CONTACT_CLICK')),
    CONSTRAINT ck_analytics_event_target CHECK (
        (type = 'DIRECTORY_SEARCH' AND business_id IS NULL AND product_id IS NULL)
        OR (type IN ('BUSINESS_VIEW', 'CONTACT_CLICK') AND business_id IS NOT NULL AND product_id IS NULL)
        OR (type = 'PRODUCT_VIEW' AND business_id IS NOT NULL AND product_id IS NOT NULL))
);

CREATE INDEX ix_analytics_event_type_time ON analytics_event (type, occurred_at);
-- "Most visited businesses" and per-business views.
CREATE INDEX ix_analytics_event_business ON analytics_event (business_id, type, occurred_at)
    WHERE business_id IS NOT NULL;
