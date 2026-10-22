-- ============================================================================
-- V3 — Module crm: khách hàng, công nợ, sổ cái công nợ, phiếu thu
-- Chạy trên HQ và MỌI chi nhánh.
--   customer                  : owner HQ, replicate HQ -> Branch
--   receivable_debt           : owner Branch, replicate Branch -> HQ (row filter branch_id)
--   receivable_debt_movement  : owner Branch, replicate Branch -> HQ
--   cash_receipt              : owner Branch, replicate Branch -> HQ
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

-- Số dư công nợ hiện tại theo (khách, chi nhánh).
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

-- Replicate Branch -> HQ bằng publication có row filter `WHERE (branch_id = N)`
-- (scripts/setup-replication.sh). PostgreSQL chỉ cho UPDATE/DELETE đi qua row filter khi mọi
-- cột trong filter thuộc REPLICA IDENTITY, nên dùng unique key (customer_id, branch_id) thay cho id.
ALTER TABLE receivable_debt REPLICA IDENTITY USING INDEX uk_debt_customer_branch;

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

-- Phiếu thu tiền khách hàng.
--  * Lập phiếu = giảm công nợ: receivable_debt_movement PAYMENT, ref_type 'CASH_RECEIPT', ref_id = cash_receipt.id.
--  * Hủy phiếu = tăng lại công nợ đúng số tiền: ADJUSTMENT, ref_type 'CASH_RECEIPT_CANCEL'. Không sửa, không xóa.
--  * debt_before / debt_after: snapshot công nợ lúc lập phiếu (đọc sau khi khóa dòng công nợ).
--  * request_key: khóa lập phiếu (xem sales_invoice.request_key ở V5).
CREATE TABLE cash_receipt (
    id              UUID          PRIMARY KEY,
    receipt_code    VARCHAR(100)  NOT NULL UNIQUE,
    branch_id       BIGINT        NOT NULL,
    customer_id     UUID          NOT NULL,
    receipt_date    DATE          NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    debt_before     NUMERIC(19,4) NOT NULL,
    debt_after      NUMERIC(19,4) NOT NULL,
    note            TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'POSTED',
    cancelled_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    cancelled_by    UUID,
    cancel_reason   TEXT,
    request_key     VARCHAR(255),
    version         INTEGER       NOT NULL,
    is_deleted      BOOLEAN       NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT ck_cash_receipt_amount CHECK (amount > 0),
    CONSTRAINT ck_cash_receipt_debt   CHECK (debt_after = debt_before - amount),
    CONSTRAINT ck_cash_receipt_status CHECK (status IN ('POSTED', 'CANCELLED')),
    CONSTRAINT uk_cash_receipt_request_key UNIQUE (request_key),
    CONSTRAINT fk_cash_receipt_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);
CREATE INDEX idx_cash_receipt_branch_created ON cash_receipt (branch_id, created_at DESC);
CREATE INDEX idx_cash_receipt_customer       ON cash_receipt (customer_id, branch_id);
