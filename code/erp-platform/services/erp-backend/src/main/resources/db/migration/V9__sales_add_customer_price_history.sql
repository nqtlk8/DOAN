-- ============================================================================
-- V9 — Giá riêng theo khách hàng: lưu tự động khi xác nhận hóa đơn bán
--
-- Quy tắc xác định giá bán (CustomerProductPriceService.resolvePrices):
--   1. Giá riêng của khách (customer_product_price) còn hiệu lực
--      effective_from <= now AND (effective_to IS NULL OR effective_to > now)
--   2. Nếu không có -> giá niêm yết chi nhánh (price_list, effective_date <= now, mới nhất)
--   3. Nếu không có -> nhân viên nhập tay
--
-- Khi xác nhận hóa đơn (SalesInvoiceService.applyConfirmationEffects), dòng nào có
-- unit_price khác giá đang áp dụng ở trên thì được ghi (upsert) vào customer_product_price
-- theo khóa (customer_id, product_id, branch_id) và ghi một dòng lịch sử.
--
-- Owner dữ liệu: Branch. Replicate Branch -> HQ (TRANSACTION_TABLES trong
-- scripts/replication-tables.conf). HQ không ghi vào 2 bảng này.
-- ============================================================================

-- Hóa đơn đã tạo ra/cập nhật giá riêng gần nhất (truy vết).
ALTER TABLE customer_product_price ADD COLUMN source_invoice_id UUID;

-- Tra giá hàng loạt theo khách tại chi nhánh.
CREATE INDEX ix_cpp_customer_branch ON customer_product_price (customer_id, branch_id);

-- Lịch sử thay đổi giá riêng. Append-only.
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
