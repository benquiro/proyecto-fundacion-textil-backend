CREATE SEQUENCE product_seq START WITH 1000 INCREMENT BY 50;

CREATE TABLE product (
    product_id    bigint        PRIMARY KEY,
    business_id   bigint        NOT NULL REFERENCES business (business_id),
    title         varchar(100)  NOT NULL,
    description   varchar(1000) NOT NULL,
    -- Reference price in CLP (no decimals).
    price         bigint,
    -- Structured attributes the app used to pack into the description.
    size          varchar(10),
    condition     varchar(20),
    material      varchar(100),
    offering_type varchar(20),
    status        varchar(10)   NOT NULL DEFAULT 'ACTIVE',
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    deleted_at    timestamptz,
    version       bigint        NOT NULL DEFAULT 0,
    CONSTRAINT ck_product_status CHECK (status IN ('ACTIVE', 'HIDDEN')),
    CONSTRAINT ck_product_price CHECK (price >= 0),
    CONSTRAINT ck_product_size CHECK (size IN ('XS', 'S', 'M', 'L', 'XL', 'ONE_SIZE')),
    CONSTRAINT ck_product_condition CHECK (condition IN ('NEW', 'GOOD', 'WITH_DETAILS')),
    CONSTRAINT ck_product_offering_type CHECK (offering_type IN ('MADE_TO_ORDER', 'REPAIR')),
    CONSTRAINT ck_product_title CHECK (btrim(title) <> ''),
    CONSTRAINT ck_product_description CHECK (btrim(description) <> '')
);

CREATE INDEX ix_product_business ON product (business_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE SEQUENCE product_photo_seq START WITH 1000 INCREMENT BY 50;

-- The order constraint is deferred so a replaced gallery can be written before the old rows go.
CREATE TABLE product_photo (
    photo_id   bigint       PRIMARY KEY,
    product_id bigint       NOT NULL REFERENCES product (product_id) ON DELETE CASCADE,
    url        varchar(500) NOT NULL,
    sort_order integer      NOT NULL,
    created_at timestamptz  NOT NULL,
    CONSTRAINT uk_product_photo_order UNIQUE (product_id, sort_order) DEFERRABLE INITIALLY DEFERRED,
    -- 1 is the main photo.
    CONSTRAINT ck_product_photo_order CHECK (sort_order BETWEEN 1 AND 5)
);
