-- ============================================================================
-- V3 — Module crm: khách hàng, công nợ, sổ cái công nợ
-- Chạy trên HQ và MỌI chi nhánh.
--   customer                  : owner HQ, replicate HQ -> Branch
--   receivable_debt           : owner Branch, replicate Branch -> HQ (row filter branch_id)
--   receivable_debt_movement  : owner Branch, replicate Branch -> HQ
-- ============================================================================

-- branch_id NULL = khách dùng chung mọi chi nhánh; có giá trị = khách riêng của chi nhánh đó.
CREATE TABLE customer (
    id              UUID         PRIMARY KEY,
    customer_code   VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(200),
    address         TEXT,
    tax_code        VARCHAR(20),
    branch_id       BIGINT,
    version         INTEGER      NOT NULL,
    is_deleted      BOOLEAN      NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by      UUID,
    updated_by      UUID
);

-- Số dư công nợ hiện tại theo (khách, chi nhánh). Unique key này cũng là REPLICA IDENTITY (V8).
CREATE TABLE receivable_debt (
    id              UUID          PRIMARY KEY,
    customer_id     UUID          NOT NULL,
    branch_id       BIGINT        NOT NULL,
    total_debt      NUMERIC(19,4) NOT NULL,
    version         INTEGER       NOT NULL,
    is_deleted      BOOLEAN       NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT uk_debt_customer_branch UNIQUE (customer_id, branch_id),
    CONSTRAINT fk_receivable_debt_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

-- Sổ cái công nợ (append-only). amount có dấu: + tăng nợ (INVOICE, OPENING_BALANCE), - giảm nợ (PAYMENT, RETURN).
-- balance_after = balance_before + amount. idempotency_key chống ghi trùng một nghiệp vụ.
CREATE TABLE receivable_debt_movement (
    id                  UUID          PRIMARY KEY,
    branch_id           BIGINT        NOT NULL,
    customer_id         UUID          NOT NULL,
    movement_type       VARCHAR(50)   NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    balance_before      NUMERIC(19,4) NOT NULL,
    balance_after       NUMERIC(19,4) NOT NULL,
    ref_type            VARCHAR(50)   NOT NULL,
    ref_id              VARCHAR(100)  NOT NULL,
    idempotency_key     VARCHAR(255),
    note                TEXT,
    created_by          UUID,
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_debt_movement_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_debt_movement_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);
CREATE INDEX idx_debt_movement_customer_branch ON receivable_debt_movement (customer_id, branch_id);
CREATE INDEX idx_debt_movement_ref ON receivable_debt_movement (ref_type, ref_id);
