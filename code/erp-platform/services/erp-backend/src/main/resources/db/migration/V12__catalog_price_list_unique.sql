-- ============================================================================
-- V12 — Mỗi cặp (sản phẩm, chi nhánh) chỉ có MỘT giá niêm yết.
--
-- Cột effective_date được giữ lại làm ngày ghi nhận giá; hệ thống không đặt lịch giá
-- theo ngày. Ràng buộc này ngăn hai dòng giá cho cùng cặp (trước đây ProductReader
-- ném lỗi khi gặp dòng trùng).
--
-- Migration thất bại nếu dữ liệu hiện có đã trùng cặp (product_id, branch_id): cần gộp
-- tay trước khi chạy. price_list là bảng MASTER (HQ -> Branch); Flyway chạy ở cả HQ và
-- chi nhánh nên ràng buộc có ở mọi nơi.
-- ============================================================================

ALTER TABLE price_list
    ADD CONSTRAINT uk_price_list_product_branch UNIQUE (product_id, branch_id);
