-- ============================================================================
-- V4 — Module inventory: tồn kho, sổ kho, lô giá vốn FIFO, phiếu nhập, ngưỡng cảnh báo
-- Chạy trên HQ và MỌI chi nhánh.
--   stock_on_hand            : owner Branch, replicate Branch -> HQ (row filter branch_id)
--   stock_movement, cost_layer, inbound_receipt(_line) : owner Branch, replicate Branch -> HQ
--   inventory_alert_config   : owner HQ, replicate HQ -> Branch
-- ============================================================================

-- Snapshot tồn hiện tại theo (sản phẩm, chi nhánh). Unique key này cũng là REPLICA IDENTITY (V8).
CREATE TABLE stock_on_hand (
    id          UUID          PRIMARY KEY,
    product_id  BIGINT        NOT NULL,
    branch_id   BIGINT        NOT NULL,
    quantity    NUMERIC(19,4) NOT NULL,
    version     INTEGER       NOT NULL,
    is_deleted  BOOLEAN       NOT NULL,
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by  UUID,
    updated_by  UUID,
    CONSTRAINT uk_stock_product_branch UNIQUE (product_id, branch_id)
);

CREATE TABLE stock_movement (
    id              UUID          PRIMARY KEY,
    product_id      BIGINT        NOT NULL,
    branch_id       BIGINT        NOT NULL,
    movement_type   VARCHAR(20)   NOT NULL,
    quantity        NUMERIC(18,3) NOT NULL,   -- dương = vào, âm = ra
    ref_type        VARCHAR(50)   NOT NULL,   -- 'inbound_receipt' | 'sales_invoice' | 'goods_return'
    ref_id          VARCHAR(100)  NOT NULL,   -- UUID chứng từ dạng chuỗi
    ref_line_id     UUID,
    performed_by    UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_movement_type CHECK (movement_type IN ('INBOUND', 'SALE', 'RETURN', 'ADJUSTMENT')),
    CONSTRAINT fk_stock_movement_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_stock_movement_branch  FOREIGN KEY (branch_id)  REFERENCES branch (id)
);
CREATE INDEX ix_stock_movement_product_branch ON stock_movement (product_id, branch_id, created_at DESC);
CREATE INDEX ix_stock_movement_ref ON stock_movement (ref_type, ref_id);
COMMENT ON TABLE stock_movement IS
    'Nguồn sự thật về số lượng. Append-only. SUM(quantity) = stock_on_hand.quantity.';

CREATE TABLE cost_layer (
    id                   UUID          PRIMARY KEY,
    product_id           BIGINT        NOT NULL,
    branch_id            BIGINT        NOT NULL,
    unit_cost            NUMERIC(18,4) NOT NULL,
    initial_qty          NUMERIC(18,3) NOT NULL,
    remaining_qty        NUMERIC(18,3) NOT NULL,
    inbound_movement_id  UUID,
    cost_basis           VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
    version              BIGINT        NOT NULL DEFAULT 0,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_cost_layer_unit_cost     CHECK (unit_cost >= 0),
    CONSTRAINT ck_cost_layer_initial_qty   CHECK (initial_qty > 0),
    CONSTRAINT ck_cost_layer_remaining_qty CHECK (remaining_qty >= 0),
    CONSTRAINT ck_cost_layer_cost_basis    CHECK (cost_basis IN ('NORMAL', 'RETURN', 'NO_LAYER')),
    CONSTRAINT fk_cost_layer_product  FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_cost_layer_branch   FOREIGN KEY (branch_id)  REFERENCES branch (id),
    CONSTRAINT fk_cost_layer_movement FOREIGN KEY (inbound_movement_id) REFERENCES stock_movement (id)
);
CREATE INDEX ix_cost_layer_fifo ON cost_layer (product_id, branch_id, created_at ASC) WHERE remaining_qty > 0;
COMMENT ON TABLE cost_layer IS
    'Nguồn tính giá vốn FIFO. Mỗi INBOUND tạo 1 entry. remaining_qty giảm khi bán.';

-- supplier_id tham chiếu supplier (dùng chung) — không đặt FK vì supplier thuộc module catalog.
CREATE TABLE inbound_receipt (
    id              UUID         PRIMARY KEY,
    receipt_code    VARCHAR(255) NOT NULL UNIQUE,
    branch_id       BIGINT       NOT NULL,
    supplier_id     UUID,
    status          VARCHAR(255) NOT NULL,
    note            VARCHAR(255),
    confirmed_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    confirmed_by    UUID,
    version         INTEGER      NOT NULL,
    is_deleted      BOOLEAN      NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT ck_inbound_receipt_status CHECK (status IN ('DRAFT', 'CONFIRMED'))
);

CREATE TABLE inbound_receipt_line (
    id                  UUID          PRIMARY KEY,
    receipt_id          UUID          NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            NUMERIC(19,4) NOT NULL,
    unit_cost           NUMERIC(19,4) NOT NULL,
    unit_of_measure     VARCHAR(50)   NOT NULL,
    version             INTEGER       NOT NULL,
    is_deleted          BOOLEAN       NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT fk_inbound_receipt_line_receipt FOREIGN KEY (receipt_id) REFERENCES inbound_receipt (id)
);

-- Ngưỡng tồn thấp theo (sản phẩm, chi nhánh). Nhập tại HQ, replicate xuống chi nhánh.
CREATE TABLE inventory_alert_config (
    id                      BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    product_id              BIGINT        NOT NULL,
    branch_id               BIGINT        NOT NULL,
    min_quantity_threshold  NUMERIC(38,2) NOT NULL,
    email_recipients        VARCHAR(255),
    is_active               BOOLEAN       NOT NULL DEFAULT true,
    created_at              TIMESTAMP(6) WITH TIME ZONE,
    updated_at              TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_alert_config_product_branch UNIQUE (product_id, branch_id),
    CONSTRAINT ck_alert_threshold_non_negative CHECK (min_quantity_threshold >= 0)
);
