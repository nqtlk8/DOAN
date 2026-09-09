-- ================================================================
-- V11: Inventory Refactor — Stock Movement + Cost Layer
-- Bỏ: avg_cost, unit_conversion, outbound_receipt, stock_transfer
-- ================================================================

-- 1. Xóa bảng legacy (theo thứ tự FK)
DROP TABLE IF EXISTS stock_transfer_line CASCADE;
DROP TABLE IF EXISTS stock_transfer CASCADE;
DROP TABLE IF EXISTS outbound_receipt_line CASCADE;
DROP TABLE IF EXISTS outbound_receipt CASCADE;
DROP TABLE IF EXISTS unit_conversion CASCADE;

-- 2. Xóa cột avg_cost khỏi stock_on_hand
ALTER TABLE stock_on_hand DROP COLUMN IF EXISTS avg_cost;

-- 3. Xóa customer_type
ALTER TABLE customer DROP COLUMN IF EXISTS customer_type;

-- 4. Thêm bảng stock_movement
CREATE TABLE stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES product(id),
    branch_id       BIGINT        NOT NULL REFERENCES branch(id),
    movement_type   VARCHAR(20)   NOT NULL
                    CHECK (movement_type IN ('INBOUND','SALE','RETURN','ADJUSTMENT')),
    quantity        NUMERIC(18,3) NOT NULL,   -- dương = vào, âm = ra
    ref_type        VARCHAR(50)   NOT NULL,   -- 'inbound_receipt','sales_invoice','goods_return'
    ref_id          VARCHAR(100)  NOT NULL,   -- UUID dạng string
    ref_line_id     UUID,
    performed_by    UUID,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_stock_movement_product_branch
    ON stock_movement (product_id, branch_id, created_at DESC);
CREATE INDEX ix_stock_movement_ref
    ON stock_movement (ref_type, ref_id);

-- 5. Thêm bảng cost_layer
CREATE TABLE cost_layer (
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT        NOT NULL REFERENCES product(id),
    branch_id            BIGINT        NOT NULL REFERENCES branch(id),
    unit_cost            NUMERIC(18,4) NOT NULL CHECK (unit_cost >= 0),
    initial_qty          NUMERIC(18,3) NOT NULL CHECK (initial_qty > 0),
    remaining_qty        NUMERIC(18,3) NOT NULL CHECK (remaining_qty >= 0),
    inbound_movement_id  BIGINT REFERENCES stock_movement(id),
    cost_basis           VARCHAR(20)   NOT NULL DEFAULT 'NORMAL'
                         CHECK (cost_basis IN ('NORMAL','RETURN','NO_LAYER')),
    version              BIGINT        NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_cost_layer_fifo
    ON cost_layer (product_id, branch_id, created_at ASC)
    WHERE remaining_qty > 0;

-- 6. Thêm cost_basis vào sales_invoice_line
ALTER TABLE sales_invoice_line
    ADD COLUMN IF NOT EXISTS cost_basis VARCHAR(20) DEFAULT 'NORMAL';

COMMENT ON TABLE stock_movement IS
    'Nguồn sự thật về số lượng. Append-only. SUM(quantity) = stock_on_hand.quantity.';
COMMENT ON TABLE cost_layer IS
    'Nguồn tính giá vốn FIFO. Mỗi INBOUND tạo 1 entry. remaining_qty giảm khi bán.';
