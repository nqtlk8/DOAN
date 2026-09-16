-- ============================================================================
-- V13: SEED TRANSACTIONS (Inbound, Sales, Inventory, Debt, Analytics)
-- Depends on: V2 (master data), V11 (stock_movement, cost_layer), V12 (UUID)
--
-- Dữ liệu test mô phỏng luồng nghiệp vụ thực tế:
--   1. Nhập hàng (Inbound Receipt) từ NCC Viglacera → TP1
--   2. Tạo Stock Movement + Cost Layer (FIFO)
--   3. Cập nhật Stock On Hand
--   4. Bán hàng (Sales Invoice) cho KH Hòa Bình
--   5. Ghi nhận Công Nợ (Receivable Debt)
--   6. Trả hàng (Goods Return) 1 phần
--   7. Seed dim_date + fact_sales cho Dashboard Analytics
--
-- Tài khoản test:
--   admin/password    → role ADMIN (HQ, branch_id=NULL)
--   staff_tp1/password → role STAFF (branch_id=1)
-- ============================================================================

-- ============================================================================
-- 1. PHIẾU NHẬP HÀNG (Inbound Receipt) — NCC Viglacera → Chi nhánh TP1
-- ============================================================================
INSERT INTO inbound_receipt (id, receipt_code, supplier_id, branch_id, status,
    created_at, updated_at, confirmed_at, version, is_deleted, note)
VALUES
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PN-001',
     'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',  -- SUP-001 Viglacera
     1,  -- TP1
     'CONFIRMED', now(), now(), now(), 0, false,
     'Nhập hàng đầu kỳ — Gạch Granite + Ceramic')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inbound_receipt_line (id, receipt_id, product_id, quantity, unit_cost,
    unit_of_measure, version, is_deleted, created_at, updated_at)
VALUES
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
     'd1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     1, 100, 150000.0000, 'Hộp', 0, false, now(), now()),   -- SP-G001 Granite 60x60
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
     'd1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     2, 50, 100000.0000, 'Hộp', 0, false, now(), now())     -- SP-G002 Ceramic 30x60
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 2. STOCK MOVEMENT — Ghi nhận nhập kho (append-only)
-- ============================================================================
INSERT INTO stock_movement (id, product_id, branch_id, movement_type, quantity,
    ref_type, ref_id, created_at)
VALUES
    ('a1000000-0000-4000-8000-000000000001', 1, 1, 'INBOUND', 100.000,
     'inbound_receipt', 'd1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', now()),
    ('a1000000-0000-4000-8000-000000000002', 2, 1, 'INBOUND', 50.000,
     'inbound_receipt', 'd1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', now())
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 3. COST LAYER — FIFO cho từng lô nhập
-- ============================================================================
INSERT INTO cost_layer (id, product_id, branch_id, unit_cost, initial_qty,
    remaining_qty, inbound_movement_id, cost_basis, version, created_at)
VALUES
    ('b1000000-0000-4000-8000-000000000001', 1, 1, 150000.0000, 100.000, 80.000,
     'a1000000-0000-4000-8000-000000000001', 'NORMAL', 0, now()),
    ('b1000000-0000-4000-8000-000000000002', 2, 1, 100000.0000, 50.000, 50.000,
     'a1000000-0000-4000-8000-000000000002', 'NORMAL', 0, now())
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 4. CẬP NHẬT STOCK ON HAND
--    V2 đã seed stock_on_hand cho product 1 (qty=500), product 4, 5.
--    Ở đây ta cộng thêm cho product 2 (chưa có trong V2).
-- ============================================================================
INSERT INTO stock_on_hand (id, branch_id, product_id, quantity, version,
    created_at, updated_at, is_deleted)
VALUES
    ('20eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 1, 2, 50.0000, 0, now(), now(), false)
ON CONFLICT (product_id, branch_id) DO NOTHING;

-- Cập nhật lại quantity cho product 1 (đã có từ V2, cộng thêm 100 từ phiếu nhập)
UPDATE stock_on_hand SET quantity = quantity + 100
WHERE branch_id = 1 AND product_id = 1
  AND quantity < 700;  -- guard: chỉ cộng nếu chưa cộng lần nào

-- ============================================================================
-- 5. HOÁ ĐƠN BÁN HÀNG (Sales Invoice) — Bán cho KH Hòa Bình, trả sau
-- ============================================================================
INSERT INTO sales_invoice (id, invoice_code, customer_id, branch_id,
    total_amount, previous_debt, remaining_debt, status, payment_method,
    version, is_deleted, created_at, updated_at, confirmed_at)
VALUES
    ('e1000000-0000-4000-8000-000000000001', 'HD-001',
     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',  -- KH-001 Hòa Bình
     1,  -- TP1
     3700000.0000, 0.0000, 3700000.0000,
     'CONFIRMED', 'CREDIT', 0, false,
     now(), now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO sales_invoice_line (id, invoice_id, product_id, product_name,
    quantity, unit_price, unit_cost, line_total, unit_of_measure,
    version, is_deleted, created_at, updated_at)
VALUES
    ('e1000000-0000-4000-8000-000000000011', 'e1000000-0000-4000-8000-000000000001',
     1, 'Gạch Granite 60x60 Taicera',
     20.0000, 185000.0000, 150000.0000, 3700000.0000, 'Hộp',
     0, false, now(), now())
ON CONFLICT (id) DO NOTHING;

-- Stock Movement cho bán hàng (quantity âm)
INSERT INTO stock_movement (id, product_id, branch_id, movement_type, quantity,
    ref_type, ref_id, created_at)
VALUES
    ('a1000000-0000-4000-8000-000000000003', 1, 1, 'SALE', -20.000,
     'sales_invoice', 'e1000000-0000-4000-8000-000000000001', now())
ON CONFLICT (id) DO NOTHING;

-- Giảm stock on hand
UPDATE stock_on_hand SET quantity = quantity - 20
WHERE branch_id = 1 AND product_id = 1
  AND quantity >= 20;  -- guard: chỉ trừ nếu đủ hàng

-- ============================================================================
-- 6. CÔNG NỢ PHẢI THU (Receivable Debt)
-- ============================================================================
INSERT INTO receivable_debt (id, customer_id, branch_id, total_debt,
    version, is_deleted, created_at, updated_at)
VALUES
    ('f1000000-0000-4000-8000-000000000001',
     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',  -- KH-001
     1,  -- TP1
     3700000.0000, 0, false, now(), now())
ON CONFLICT (customer_id, branch_id)
DO UPDATE SET total_debt = receivable_debt.total_debt + EXCLUDED.total_debt;

-- ============================================================================
-- 7. TRẢ HÀNG (Goods Return) — KH trả lại 5 hộp Gạch Granite
-- ============================================================================
INSERT INTO goods_return (id, return_code, customer_id, invoice_id, branch_id,
    total_amount, status, reason, version, is_deleted,
    created_at, updated_at, confirmed_at)
VALUES
    ('f2000000-0000-4000-8000-000000000001', 'TH-001',
     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',  -- KH-001
     'e1000000-0000-4000-8000-000000000001',   -- HD-001
     1,  -- TP1
     925000.00, 'CONFIRMED', 'Gạch bị nứt khi vận chuyển',
     0, false, now(), now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO goods_return_line (id, return_id, product_id, quantity,
    unit_price, unit_of_measure, version, is_deleted, created_at, updated_at)
VALUES
    ('f2000000-0000-4000-8000-000000000011',
     'f2000000-0000-4000-8000-000000000001',
     1, 5.00, 185000.00, 'Hộp',
     0, false, now(), now())
ON CONFLICT (id) DO NOTHING;

-- Stock Movement cho trả hàng (quantity dương = hàng quay về kho)
INSERT INTO stock_movement (id, product_id, branch_id, movement_type, quantity,
    ref_type, ref_id, created_at)
VALUES
    ('a1000000-0000-4000-8000-000000000004', 1, 1, 'RETURN', 5.000,
     'goods_return', 'f2000000-0000-4000-8000-000000000001', now())
ON CONFLICT (id) DO NOTHING;

-- Cost Layer cho hàng trả về
INSERT INTO cost_layer (id, product_id, branch_id, unit_cost, initial_qty,
    remaining_qty, inbound_movement_id, cost_basis, version, created_at)
VALUES
    ('b1000000-0000-4000-8000-000000000003', 1, 1, 150000.0000, 5.000, 5.000,
     'a1000000-0000-4000-8000-000000000004', 'RETURN', 0, now())
ON CONFLICT (id) DO NOTHING;

-- Cộng lại stock
UPDATE stock_on_hand SET quantity = quantity + 5
WHERE branch_id = 1 AND product_id = 1;

-- Giảm công nợ
UPDATE receivable_debt SET total_debt = total_debt - 925000
WHERE customer_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11' AND branch_id = 1;

-- ============================================================================
-- 8. HOÁ ĐƠN BÁN HÀNG #2 — Bán Smart Home cho Anh Tuấn, thanh toán tiền mặt
-- ============================================================================
INSERT INTO sales_invoice (id, invoice_code, customer_id, branch_id,
    total_amount, previous_debt, remaining_debt, status, payment_method,
    version, is_deleted, created_at, updated_at, confirmed_at)
VALUES
    ('e1000000-0000-4000-8000-000000000002', 'HD-002',
     'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',  -- KH-002 Anh Tuấn
     1,  -- TP1
     2100000.0000, 0.0000, 0.0000,
     'CONFIRMED', 'CASH', 0, false,
     now(), now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO sales_invoice_line (id, invoice_id, product_id, product_name,
    quantity, unit_price, unit_cost, line_total, unit_of_measure,
    version, is_deleted, created_at, updated_at)
VALUES
    ('e1000000-0000-4000-8000-000000000021', 'e1000000-0000-4000-8000-000000000002',
     4, 'Công Tắc Thông Minh Tuya Zigbee 3 Nút',
     6.0000, 350000.0000, 250000.0000, 2100000.0000, 'Cái',
     0, false, now(), now())
ON CONFLICT (id) DO NOTHING;

-- Stock Movement cho bán hàng #2
INSERT INTO stock_movement (id, product_id, branch_id, movement_type, quantity,
    ref_type, ref_id, created_at)
VALUES
    ('a1000000-0000-4000-8000-000000000005', 4, 1, 'SALE', -6.000,
     'sales_invoice', 'e1000000-0000-4000-8000-000000000002', now())
ON CONFLICT (id) DO NOTHING;

-- Giảm stock on hand product 4
UPDATE stock_on_hand SET quantity = quantity - 6
WHERE branch_id = 1 AND product_id = 4
  AND quantity >= 6;

-- ============================================================================
-- 9. DIM_DATE — Seed ngày cho tháng hiện tại (cho Dashboard Analytics)
-- ============================================================================
INSERT INTO dim_date (date_key, full_date, year, month, day)
SELECT
    CAST(to_char(d, 'YYYYMMDD') AS INTEGER) AS date_key,
    d::date AS full_date,
    EXTRACT(YEAR FROM d)::SMALLINT AS year,
    EXTRACT(MONTH FROM d)::SMALLINT AS month,
    EXTRACT(DAY FROM d)::SMALLINT AS day
FROM generate_series(
    date_trunc('month', CURRENT_DATE),
    date_trunc('month', CURRENT_DATE) + INTERVAL '1 month' - INTERVAL '1 day',
    INTERVAL '1 day'
) AS d
ON CONFLICT (date_key) DO NOTHING;

-- Seed thêm tháng trước (để lọc range trên Dashboard)
INSERT INTO dim_date (date_key, full_date, year, month, day)
SELECT
    CAST(to_char(d, 'YYYYMMDD') AS INTEGER) AS date_key,
    d::date AS full_date,
    EXTRACT(YEAR FROM d)::SMALLINT AS year,
    EXTRACT(MONTH FROM d)::SMALLINT AS month,
    EXTRACT(DAY FROM d)::SMALLINT AS day
FROM generate_series(
    date_trunc('month', CURRENT_DATE) - INTERVAL '1 month',
    date_trunc('month', CURRENT_DATE) - INTERVAL '1 day',
    INTERVAL '1 day'
) AS d
ON CONFLICT (date_key) DO NOTHING;

-- ============================================================================
-- 10. FACT_SALES — Analytics cho Dashboard (doanh thu, lợi nhuận)
-- ============================================================================
INSERT INTO fact_sales (date_key, product_id, branch_id, customer_id, invoice_id,
    quantity, unit_price, unit_cost, revenue, gross_profit)
VALUES
    -- HD-001: Bán 20 hộp Gạch Granite
    (CAST(to_char(CURRENT_DATE, 'YYYYMMDD') AS INTEGER),
     1, 1,
     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     'e1000000-0000-4000-8000-000000000001',
     20.000, 185000.00, 150000.0000, 3700000.00, 700000.00),
    -- HD-002: Bán 6 Công tắc Smart Home
    (CAST(to_char(CURRENT_DATE, 'YYYYMMDD') AS INTEGER),
     4, 1,
     'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
     'e1000000-0000-4000-8000-000000000002',
     6.000, 350000.00, 250000.0000, 2100000.00, 600000.00);

-- ============================================================================
-- 11. FACT_STOCK_MOVEMENT — Analytics cho Dashboard (tồn kho)
-- ============================================================================
INSERT INTO fact_stock_movement (date_key, product_id, branch_id,
    movement_type, quantity)
VALUES
    (CAST(to_char(CURRENT_DATE, 'YYYYMMDD') AS INTEGER), 1, 1, 'INBOUND', 100.000),
    (CAST(to_char(CURRENT_DATE, 'YYYYMMDD') AS INTEGER), 2, 1, 'INBOUND', 50.000),
    (CAST(to_char(CURRENT_DATE, 'YYYYMMDD') AS INTEGER), 1, 1, 'SALE', -20.000),
    (CAST(to_char(CURRENT_DATE, 'YYYYMMDD') AS INTEGER), 4, 1, 'SALE', -6.000),
    (CAST(to_char(CURRENT_DATE, 'YYYYMMDD') AS INTEGER), 1, 1, 'RETURN', 5.000);