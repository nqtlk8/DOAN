-- 1. Cập nhật inventory_alert_config
-- Đảm bảo is_active không null
UPDATE inventory_alert_config SET is_active = true WHERE is_active IS NULL;
ALTER TABLE inventory_alert_config ALTER COLUMN is_active SET DEFAULT true;
ALTER TABLE inventory_alert_config ALTER COLUMN is_active SET NOT NULL;

-- Xoá các dòng rác nếu thiếu thông tin bắt buộc
DELETE FROM inventory_alert_config WHERE product_id IS NULL OR branch_id IS NULL OR min_quantity_threshold IS NULL;

-- Khử trùng lặp (product_id, branch_id), giữ lại dòng id lớn nhất
DELETE FROM inventory_alert_config a
WHERE a.id < (
    SELECT MAX(id) FROM inventory_alert_config b
    WHERE a.product_id = b.product_id AND a.branch_id = b.branch_id
);

-- Bật các cột bắt buộc
ALTER TABLE inventory_alert_config ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE inventory_alert_config ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE inventory_alert_config ALTER COLUMN min_quantity_threshold SET NOT NULL;

-- Ràng buộc logic
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_alert_threshold_non_negative') THEN
        ALTER TABLE inventory_alert_config ADD CONSTRAINT ck_alert_threshold_non_negative CHECK (min_quantity_threshold >= 0);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_alert_config_product_branch') THEN
        ALTER TABLE inventory_alert_config ADD CONSTRAINT uk_alert_config_product_branch UNIQUE (product_id, branch_id);
    END IF;
END $$;

-- 2. Cài đặt Replica identity để hỗ trợ row filter publication
ALTER TABLE stock_on_hand REPLICA IDENTITY USING INDEX uk_stock_product_branch;
ALTER TABLE receivable_debt REPLICA IDENTITY USING INDEX uk_debt_customer_branch;

-- 3. Xoá bảng inventory_alert_log
DROP TABLE IF EXISTS inventory_alert_log;

-- 4. Index hỗ trợ lấy danh sách hóa đơn đã xác nhận
CREATE INDEX IF NOT EXISTS ix_sales_invoice_confirmed ON sales_invoice (confirmed_at) WHERE status = 'CONFIRMED' AND is_deleted = false;
