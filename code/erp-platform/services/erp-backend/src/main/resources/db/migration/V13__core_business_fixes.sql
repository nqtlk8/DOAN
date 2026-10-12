-- ============================================================================
-- V13 — Sửa lỗi nghiệp vụ lõi (bán hàng, trả hàng, tồn kho)
-- Chạy trên HQ và MỌI chi nhánh (cùng một lần triển khai) để schema hai đầu
-- logical replication luôn giống nhau.
--
-- 1. Thống nhất số lượng NUMERIC(19,4) cho mọi bảng tồn kho.
--    Trước đây stock_on_hand là (19,4) nhưng stock_movement và cost_layer là (18,3):
--    nhập 10,0004 thì tồn ghi 10,0004 còn lớp giá vốn chỉ còn 10,000, bán 10,0004 bị
--    từ chối vì "lệch tồn kho/lớp giá vốn" dù vẫn còn hàng.
-- 2. goods_return(_line) dùng NUMERIC(38,2) cho tiền và số lượng; đổi về (19,4)
--    như sales_invoice(_line).
-- 3. goods_return.refund_amount: phần tiền trả hàng KHÔNG trừ được vào công nợ
--    (khách đã trả đủ) và phải hoàn lại cho khách bằng tiền mặt.
-- 4. sales_invoice.status bỏ 'CANCELLED': không có luồng hủy hóa đơn. Hủy một hóa đơn
--    đã xác nhận được thực hiện bằng phiếu trả hàng toàn bộ (hoàn kho, giảm công nợ).
-- ============================================================================

ALTER TABLE stock_movement ALTER COLUMN quantity      TYPE NUMERIC(19,4);
ALTER TABLE cost_layer     ALTER COLUMN initial_qty   TYPE NUMERIC(19,4);
ALTER TABLE cost_layer     ALTER COLUMN remaining_qty TYPE NUMERIC(19,4);

ALTER TABLE goods_return      ALTER COLUMN total_amount TYPE NUMERIC(19,4);
ALTER TABLE goods_return_line ALTER COLUMN quantity     TYPE NUMERIC(19,4);
ALTER TABLE goods_return_line ALTER COLUMN unit_price   TYPE NUMERIC(19,4);

ALTER TABLE goods_return ADD COLUMN refund_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
COMMENT ON COLUMN goods_return.refund_amount IS
    'Tiền trả hàng hoàn lại cho khách (không trừ được vào công nợ vì công nợ không đủ). total_amount = phần trừ công nợ + refund_amount.';

ALTER TABLE sales_invoice DROP CONSTRAINT ck_sales_invoice_status;
ALTER TABLE sales_invoice ADD CONSTRAINT ck_sales_invoice_status CHECK (status IN ('DRAFT', 'CONFIRMED'));
