-- ============================================================================
-- V10 — Giá vốn của hàng khách trả (snapshot)
--
-- goods_return_line.unit_cost = giá vốn của hàng trả, chụp lúc xác nhận phiếu trả
-- (GoodsReturnService.confirmReturn):
--   1. Có hóa đơn gốc     -> sales_invoice_line.unit_cost của dòng khớp sản phẩm
--   2. Không có hóa đơn   -> unit_cost lần bán gần nhất sản phẩm đó cho khách đó tại chi nhánh
--   3. Không có lần bán   -> unit_cost của lớp giá vốn gần nhất; không có nữa -> 0
-- Lớp giá vốn RETURN (cost_layer) dùng đúng giá trị này, không dùng giá bán (unit_price).
-- Dashboard trừ quantity * unit_cost khỏi giá vốn của kỳ xác nhận phiếu trả.
-- ============================================================================

ALTER TABLE goods_return_line ADD COLUMN unit_cost NUMERIC(19,4);
