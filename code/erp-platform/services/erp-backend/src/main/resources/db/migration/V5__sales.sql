-- ============================================================================
-- V5 — Module order/sales: hóa đơn bán, trả hàng, giá riêng theo khách
-- Chạy trên HQ và MỌI chi nhánh.
--   sales_invoice(_line), goods_return(_line) : owner Branch, replicate Branch -> HQ
--   customer_product_price                    : chỉ đọc, chưa replicate (xem DATA_OWNERSHIP_MATRIX.md)
-- ============================================================================

CREATE TABLE sales_invoice (
    id                  UUID          PRIMARY KEY,
    invoice_code        VARCHAR(100)  NOT NULL UNIQUE,
    branch_id           BIGINT        NOT NULL,
    customer_id         UUID          NOT NULL,
    status              VARCHAR(50)   NOT NULL,
    payment_method      VARCHAR(50),
    total_amount        NUMERIC(19,4) NOT NULL,
    advance_payment     NUMERIC(19,4) DEFAULT 0,
    previous_debt       NUMERIC(19,4) NOT NULL,
    remaining_debt      NUMERIC(19,4) NOT NULL,
    note                TEXT,
    confirmed_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    confirmed_by        UUID,
    version             INTEGER       NOT NULL,
    is_deleted          BOOLEAN       NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT ck_sales_invoice_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_sales_invoice_payment_method CHECK (payment_method IN ('CASH', 'CREDIT', 'MIXED'))
);
CREATE INDEX ix_sales_invoice_confirmed ON sales_invoice (confirmed_at)
    WHERE status = 'CONFIRMED' AND is_deleted = false;

CREATE TABLE sales_invoice_line (
    id                  UUID          PRIMARY KEY,
    invoice_id          UUID          NOT NULL,
    product_id          BIGINT        NOT NULL,
    product_name        VARCHAR(255),
    quantity            NUMERIC(19,4) NOT NULL,
    unit_of_measure     VARCHAR(50)   NOT NULL,
    unit_price          NUMERIC(19,4) NOT NULL,
    unit_cost           NUMERIC(19,4),
    line_total          NUMERIC(19,4) NOT NULL,
    cost_basis          VARCHAR(20)   DEFAULT 'NORMAL',
    version             INTEGER       NOT NULL,
    is_deleted          BOOLEAN       NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT fk_sales_invoice_line_invoice FOREIGN KEY (invoice_id) REFERENCES sales_invoice (id)
);

CREATE TABLE goods_return (
    id              UUID          PRIMARY KEY,
    return_code     VARCHAR(255)  NOT NULL UNIQUE,
    branch_id       BIGINT        NOT NULL,
    customer_id     UUID          NOT NULL,
    invoice_id      UUID,
    status          VARCHAR(255)  NOT NULL,
    reason          VARCHAR(255),
    note            VARCHAR(255),
    total_amount    NUMERIC(38,2) NOT NULL,
    confirmed_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    confirmed_by    UUID,
    version         INTEGER       NOT NULL,
    is_deleted      BOOLEAN       NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT ck_goods_return_status CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT fk_goods_return_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE goods_return_line (
    id                  UUID          PRIMARY KEY,
    return_id           UUID          NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            NUMERIC(38,2) NOT NULL,
    unit_price          NUMERIC(38,2) NOT NULL,
    unit_of_measure     VARCHAR(255)  NOT NULL,
    version             INTEGER       NOT NULL,
    is_deleted          BOOLEAN       NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT fk_goods_return_line_return FOREIGN KEY (return_id) REFERENCES goods_return (id)
);

CREATE TABLE customer_product_price (
    id              UUID          PRIMARY KEY,
    customer_id     UUID          NOT NULL,
    product_id      BIGINT        NOT NULL,
    branch_id       BIGINT        NOT NULL,
    unit_price      NUMERIC(19,4) NOT NULL,
    effective_from  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    effective_to    TIMESTAMP(6) WITHOUT TIME ZONE,
    version         INTEGER       NOT NULL,
    is_deleted      BOOLEAN       NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT uk_cust_prod_branch UNIQUE (customer_id, product_id, branch_id)
);
