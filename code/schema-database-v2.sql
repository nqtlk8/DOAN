-- ============================================================================
-- DATABASE SCHEMA — Website & ERP Cửa hàng VLXD & TTNT (v2 - Simplified)
-- PostgreSQL 15+ 
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- cho gen_random_uuid()

-- ============================================================================
-- MODULE: IDENTITY (HQ — master data)
-- ============================================================================

CREATE TABLE branch (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    internal_url    VARCHAR(255),
    address         VARCHAR(500),
    phone           VARCHAR(20),
    opening_hours   VARCHAR(100),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_account (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(20),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,  
    name            VARCHAR(200) NOT NULL
);

CREATE TABLE permission (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(255)
);

CREATE TABLE role_permission (
    role_id         SMALLINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id   SMALLINT NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_branch_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT   NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role_id         SMALLINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    branch_id       BIGINT   REFERENCES branch(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_user_branch_role ON user_branch_role (user_id, role_id, COALESCE(branch_id, 0));

CREATE TABLE idempotency_record (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    request_hash    VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- MODULE: CATALOG (HQ — master data, replicate READ-ONLY xuống branch)
-- ============================================================================

CREATE TABLE category (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT REFERENCES category(id) ON DELETE RESTRICT,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    internal_url    VARCHAR(255),
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_category_parent ON category (parent_id);

CREATE TABLE unit_of_measure (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE, 
    name            VARCHAR(100) NOT NULL
);

CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
    sku             VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(300) NOT NULL,
    base_unit       VARCHAR(20)  NOT NULL DEFAULT 'UNIT',
    description     TEXT,
    image_url       VARCHAR(500),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    attributes      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_product_category ON product (category_id);
CREATE INDEX ix_product_name_trgm ON product USING gin (name gin_trgm_ops);
CREATE INDEX ix_product_attributes ON product USING gin (attributes);

CREATE TABLE price_list (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT      NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    branch_id       BIGINT      NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
    price           NUMERIC(18,2) NOT NULL CHECK (price >= 0),
    effective_date  DATE        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_price_list_lookup ON price_list (product_id, branch_id, effective_date DESC);

-- ============================================================================
-- MODULE: CRM (customer = HQ master; receivable_debt, customer_product_price = BRANCH-LOCAL)
-- ============================================================================

CREATE TABLE customer (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL UNIQUE,
    full_name       VARCHAR(200) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(200),
    address         VARCHAR(500),
    customer_type   VARCHAR(20)  NOT NULL DEFAULT 'RETAIL' CHECK (customer_type IN ('RETAIL','CONSTRUCTION')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_customer_phone ON customer (phone);

CREATE TABLE receivable_debt (
    customer_id     BIGINT NOT NULL,
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, branch_id)
);

CREATE TABLE customer_product_price (
    customer_id     BIGINT NOT NULL,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    last_price      NUMERIC(18,2) NOT NULL,
    last_invoice_id UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, product_id, branch_id)
);

-- ============================================================================
-- MODULE: INVENTORY (BRANCH-LOCAL)
-- ============================================================================

CREATE TABLE stock_on_hand (
    product_id      BIGINT NOT NULL REFERENCES product(id),
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    quantity        NUMERIC(18,3) NOT NULL DEFAULT 0,
    avg_cost        NUMERIC(18,4) NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0, 
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, branch_id)
);

CREATE TABLE inbound_receipt (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_no         VARCHAR(30) NOT NULL,
    branch_id          BIGINT      NOT NULL REFERENCES branch(id),
    supplier_name      VARCHAR(200),
    status             VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    note               TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at       TIMESTAMPTZ,
    confirmed_by       UUID
);
CREATE UNIQUE INDEX ux_inbound_branch_no ON inbound_receipt (branch_id, receipt_no);

CREATE TABLE inbound_receipt_line (
    id              BIGSERIAL PRIMARY KEY,
    receipt_id      UUID   NOT NULL REFERENCES inbound_receipt(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    quantity        NUMERIC(18,3) NOT NULL CHECK (quantity > 0),
    unit_cost       NUMERIC(18,4) NOT NULL CHECK (unit_cost >= 0)
);

-- ============================================================================
-- MODULE: SALES (BRANCH-LOCAL)
-- ============================================================================

CREATE TABLE sales_invoice (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_no      VARCHAR(30)  NOT NULL,
    customer_id     BIGINT       NOT NULL REFERENCES customer(id),
    branch_id       BIGINT       NOT NULL REFERENCES branch(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    previous_debt   NUMERIC(18,2),
    total_amount    NUMERIC(18,2),
    amount_paid     NUMERIC(18,2) NOT NULL DEFAULT 0,
    remaining_debt  NUMERIC(18,2),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    confirmed_at    TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_invoice_branch_no ON sales_invoice (branch_id, invoice_no);
CREATE INDEX ix_invoice_customer ON sales_invoice (customer_id, branch_id);

CREATE TABLE sales_invoice_line (
    id                      BIGSERIAL PRIMARY KEY,
    invoice_id              UUID   NOT NULL REFERENCES sales_invoice(id) ON DELETE CASCADE,
    product_id              BIGINT NOT NULL REFERENCES product(id),
    quantity                NUMERIC(18,3) NOT NULL CHECK (quantity > 0),
    default_unit_price      NUMERIC(18,2) NOT NULL,
    unit_price              NUMERIC(18,2) NOT NULL,
    is_price_overridden     BOOLEAN NOT NULL DEFAULT FALSE,
    unit_cost_snapshot      NUMERIC(18,4)
);
CREATE INDEX ix_invoice_line_invoice ON sales_invoice_line (invoice_id);
CREATE INDEX ix_invoice_line_product ON sales_invoice_line (product_id);

CREATE TABLE goods_return (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_no           VARCHAR(30) NOT NULL,
    customer_id         BIGINT      NOT NULL REFERENCES customer(id),
    branch_id           BIGINT      NOT NULL REFERENCES branch(id),
    original_invoice_id UUID        REFERENCES sales_invoice(id),
    total_return_amount NUMERIC(18,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_return_branch_no ON goods_return (branch_id, return_no);

CREATE TABLE goods_return_line (
    id              BIGSERIAL PRIMARY KEY,
    return_id       UUID   NOT NULL REFERENCES goods_return(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    quantity        NUMERIC(18,3) NOT NULL CHECK (quantity > 0),
    return_price    NUMERIC(18,2) NOT NULL CHECK (return_price >= 0)
);

-- ============================================================================
-- MODULE: COMMON
-- ============================================================================

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    changed_by      BIGINT REFERENCES user_account(id),
    branch_id       BIGINT REFERENCES branch(id),
    before_data     JSONB,
    after_data      JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entity ON audit_log (entity_name, entity_id);
CREATE INDEX ix_audit_created ON audit_log (created_at);

-- ============================================================================
-- MODULE: ANALYTICS
-- ============================================================================

CREATE TABLE dim_date (
    date_key        INTEGER PRIMARY KEY,
    full_date       DATE NOT NULL,
    year            SMALLINT NOT NULL,
    month           SMALLINT NOT NULL,
    day             SMALLINT NOT NULL
);

CREATE TABLE fact_sales (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      UUID   NOT NULL,
    date_key        INTEGER NOT NULL REFERENCES dim_date(date_key),
    product_id      BIGINT NOT NULL,
    branch_id       BIGINT NOT NULL,
    customer_id     BIGINT NOT NULL,
    quantity        NUMERIC(18,3) NOT NULL,
    unit_price      NUMERIC(18,2) NOT NULL,
    unit_cost       NUMERIC(18,4) NOT NULL,
    revenue         NUMERIC(18,2) NOT NULL,
    gross_profit    NUMERIC(18,2) NOT NULL
);
CREATE INDEX ix_fact_sales_date_branch ON fact_sales (date_key, branch_id);
CREATE INDEX ix_fact_sales_product ON fact_sales (product_id);

-- ============================================================================
-- REPLICATION & GRANTS
-- ============================================================================
-- CREATE PUBLICATION pub_branch_1_master FOR TABLE product, category, price_list, customer;
-- CREATE PUBLICATION pub_to_hq FOR TABLE sales_invoice, sales_invoice_line, inbound_receipt, inbound_receipt_line, goods_return, goods_return_line, stock_on_hand, receivable_debt;

-- Bảng master (READ-ONLY tại Branch)
-- REVOKE ALL ON product, category, price_list, customer FROM app_user;
-- GRANT SELECT ON product, category, price_list, customer TO app_user;

-- Bảng branch-local (đầy đủ quyền DML)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON sales_invoice, sales_invoice_line, goods_return, goods_return_line, stock_on_hand, inbound_receipt, inbound_receipt_line, receivable_debt, customer_product_price, idempotency_record, audit_log TO app_user;
