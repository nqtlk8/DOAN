-- ============================================================================
-- V8 — Hỗ trợ logical replication
--
-- stock_on_hand và receivable_debt được replicate Branch -> HQ bằng publication có
-- row filter `WHERE (branch_id = N)` (scripts/setup-replication.sh). PostgreSQL chỉ cho
-- UPDATE/DELETE đi qua publication có row filter khi mọi cột trong filter thuộc
-- REPLICA IDENTITY, nên dùng unique key (product_id|customer_id, branch_id) làm identity
-- thay cho khóa chính id.
-- ============================================================================

ALTER TABLE stock_on_hand   REPLICA IDENTITY USING INDEX uk_stock_product_branch;
ALTER TABLE receivable_debt REPLICA IDENTITY USING INDEX uk_debt_customer_branch;
