-- ============================================================================
-- V5 — Module order/sales: hóa đơn bán, trả hàng, giá riêng theo khách
-- Chạy trên HQ và MỌI chi nhánh.
--   sales_invoice(_line), goods_return(_line)                : owner Branch, replicate Branch -> HQ
--   customer_product_price, customer_product_price_history   : owner Branch, replicate Branch -> HQ
--
-- request_key (sales_invoice, goods_return; inbound_receipt ở V4, cash_receipt ở V3):
-- frontend tạo một Idempotency-Key cho mỗi lần lập phiếu; IdempotencyAspect gắn key kèm phạm vi
-- người dùng vào request và service ghi vào request_key trong CÙNG transaction tạo chứng từ.
-- UNIQUE bảo đảm một lần lập phiếu chỉ ra một chứng từ, kể cả khi bản ghi idempotency_record đã
-- mất hoặc đã được dọn. NULL = chứng từ tạo không kèm Idempotency-Key.
-- ============================================================================

-- Không có luồng hủy hóa đơn: hủy hóa đơn đã xác nhận bằng phiếu trả hàng toàn bộ.
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
    request_key         VARCHAR(255),
    version             INTEGER       NOT NULL,
    is_deleted          BOOLEAN       NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT ck_sales_invoice_status CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT ck_sales_invoice_payment_method CHECK (payment_method IN ('CASH', 'CREDIT', 'MIXED')),
    CONSTRAINT uk_sales_invoice_request_key UNIQUE (request_key)
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
    total_amount    NUMERIC(19,4) NOT NULL,
    refund_amount   NUMERIC(19,4) NOT NULL DEFAULT 0,
    confirmed_at    TIMESTAMP(6) WITHOUT TIME ZONE,
    confirmed_by    UUID,
    request_key     VARCHAR(255),
    version         INTEGER       NOT NULL,
    is_deleted      BOOLEAN       NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT ck_goods_return_status CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT uk_goods_return_request_key UNIQUE (request_key),
    CONSTRAINT fk_goods_return_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);
COMMENT ON COLUMN goods_return.refund_amount IS
    'Tiền trả hàng hoàn lại cho khách (không trừ được vào công nợ vì công nợ không đủ). total_amount = phần trừ công nợ + refund_amount.';

-- unit_cost: giá vốn của hàng trả, chụp lúc xác nhận phiếu trả (GoodsReturnService.confirmReturn):
--   1. Có hóa đơn gốc     -> sales_invoice_line.unit_cost của dòng khớp sản phẩm
--   2. Không có hóa đơn   -> unit_cost lần bán gần nhất sản phẩm đó cho khách đó tại chi nhánh
--   3. Không có lần bán   -> unit_cost của lớp giá vốn gần nhất; không có nữa -> 0
-- Lớp giá vốn RETURN (cost_layer) dùng đúng giá trị này. Dashboard trừ quantity * unit_cost khỏi
-- giá vốn của kỳ xác nhận phiếu trả.
CREATE TABLE goods_return_line (
    id                  UUID          PRIMARY KEY,
    return_id           UUID          NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            NUMERIC(19,4) NOT NULL,
    unit_price          NUMERIC(19,4) NOT NULL,
    unit_cost           NUMERIC(19,4),
    unit_of_measure     VARCHAR(255)  NOT NULL,
    version             INTEGER       NOT NULL,
    is_deleted          BOOLEAN       NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT fk_goods_return_line_return FOREIGN KEY (return_id) REFERENCES goods_return (id)
);

-- Giá riêng theo khách. Quy tắc xác định giá bán (CustomerProductPriceService.resolvePrices):
--   1. Giá riêng của khách còn hiệu lực: effective_from <= now AND (effective_to IS NULL OR effective_to > now)
--   2. Nếu không có -> giá niêm yết chi nhánh (price_list)
--   3. Nếu không có -> nhân viên nhập tay
-- Khi xác nhận hóa đơn, dòng nào có unit_price khác giá đang áp dụng được ghi (upsert) vào bảng này
-- và ghi một dòng lịch sử. source_invoice_id: hóa đơn tạo/cập nhật giá gần nhất.
CREATE TABLE customer_product_price (
    id                  UUID          PRIMARY KEY,
    customer_id         UUID          NOT NULL,
    product_id          BIGINT        NOT NULL,
    branch_id           BIGINT        NOT NULL,
    unit_price          NUMERIC(19,4) NOT NULL,
    effective_from      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    effective_to        TIMESTAMP(6) WITHOUT TIME ZONE,
    source_invoice_id   UUID,
    version             INTEGER       NOT NULL,
    is_deleted          BOOLEAN       NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT uk_cust_prod_branch UNIQUE (customer_id, product_id, branch_id)
);
CREATE INDEX ix_cpp_customer_branch ON customer_product_price (customer_id, branch_id);

CREATE TABLE customer_product_price_history (
    id                  UUID          PRIMARY KEY,
    customer_id         UUID          NOT NULL,
    product_id          BIGINT        NOT NULL,
    branch_id           BIGINT        NOT NULL,
    old_price           NUMERIC(19,4),            -- NULL = lần đầu khách có giá riêng
    new_price           NUMERIC(19,4) NOT NULL,
    list_price          NUMERIC(19,4),            -- giá niêm yết tại thời điểm thay đổi (tham khảo)
    source_invoice_id   UUID,
    changed_by          UUID,
    changed_at          TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_cpp_history_new_price CHECK (new_price >= 0)
);
CREATE INDEX ix_cpp_history_key ON customer_product_price_history (customer_id, product_id, branch_id, changed_at DESC);
COMMENT ON TABLE customer_product_price_history IS
    'Lịch sử giá riêng theo khách. Mỗi lần customer_product_price được tạo/cập nhật từ hóa đơn sinh 1 dòng.';
