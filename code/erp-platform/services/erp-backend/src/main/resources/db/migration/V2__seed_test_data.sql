-- ============================================================================
-- MODULE: SEED DATA (For Docker / Manual Testing) - VLXD & Smart Home
-- ============================================================================

-- 1. Branches
INSERT INTO branch (id, code, name, internal_url, is_active, created_at, updated_at) 
VALUES 
(1, 'TP1', 'Chi nhánh Trung Tâm (TP1)', 'http://branch-tp1.local:80', true, now(), now()),
(2, 'TP2', 'Chi nhánh Quận 7 (TP2)', 'http://branch-tp2.local:80', true, now(), now())
ON CONFLICT (code) DO UPDATE SET internal_url = EXCLUDED.internal_url;

SELECT setval(pg_get_serial_sequence('branch', 'id'), (SELECT MAX(id) FROM branch));

-- 2. Roles
INSERT INTO role (id, code, name) VALUES 
(1, 'STAFF', 'Nhân viên bán hàng/Kho'),
(2, 'ADMIN', 'Quản trị viên Hệ thống')
ON CONFLICT (code) DO NOTHING;
SELECT setval(pg_get_serial_sequence('role', 'id'), (SELECT MAX(id) FROM role));

-- 3. Users (Password is 'password')
INSERT INTO user_account (id, username, password_hash, full_name, is_active, created_at, updated_at)
VALUES 
(1, 'admin', '$2a$10$iGQ7Sy5sYyDeakHKzeu3AOpipgneKau3yF8Oq8BQRqofkhgevv..G', 'Quản trị viên HQ', true, now(), now()),
(2, 'staff_tp1', '$2a$10$iGQ7Sy5sYyDeakHKzeu3AOpipgneKau3yF8Oq8BQRqofkhgevv..G', 'Nhân viên TP1', true, now(), now()),
(3, 'staff_tp2', '$2a$10$iGQ7Sy5sYyDeakHKzeu3AOpipgneKau3yF8Oq8BQRqofkhgevv..G', 'Nhân viên TP2', true, now(), now())
ON CONFLICT (username) DO NOTHING;
SELECT setval(pg_get_serial_sequence('user_account', 'id'), (SELECT MAX(id) FROM user_account));

-- 4. User-Role-Branch Mapping
DELETE FROM user_branch_role WHERE user_id IN (1, 2, 3);
INSERT INTO user_branch_role (user_id, role_id, branch_id) VALUES (1, 2, NULL); -- Admin at HQ
INSERT INTO user_branch_role (user_id, role_id, branch_id) VALUES (2, 1, 1);    -- Staff at TP1
INSERT INTO user_branch_role (user_id, role_id, branch_id) VALUES (3, 1, 2);    -- Staff at TP2

-- 5. Categories
INSERT INTO category (id, code, name, is_active, created_at, updated_at) VALUES 
(1, 'CAT-G', 'Gạch Ốp Lát', true, now(), now()),
(2, 'CAT-S', 'Sắt Thép Xây Dựng', true, now(), now()),
(3, 'CAT-SH', 'Thiết Bị Smart Home', true, now(), now()),
(4, 'CAT-LED', 'Đèn LED & Chiếu Sáng', true, now(), now())
ON CONFLICT (code) DO NOTHING;
SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT MAX(id) FROM category));

-- 6. Products
INSERT INTO product (id, code, name, category_id, base_unit, is_active, created_at, updated_at) VALUES 
(1, 'SP-G001', 'Gạch Granite 60x60 Taicera', 1, 'Hộp', true, now(), now()),
(2, 'SP-G002', 'Gạch Men Ceramic 30x60 Viglacera', 1, 'Hộp', true, now(), now()),
(3, 'SP-S001', 'Thép Cuộn Pomina Phi 8', 2, 'Cuộn', true, now(), now()),
(4, 'SP-SH001', 'Công Tắc Thông Minh Tuya Zigbee 3 Nút', 3, 'Cái', true, now(), now()),
(5, 'SP-SH002', 'Khóa Cửa Vân Tay Xiaomi Smart Door Lock', 3, 'Bộ', true, now(), now()),
(6, 'SP-L001', 'Bóng Đèn LED Âm Trần Rạng Đông 9W', 4, 'Cái', true, now(), now())
ON CONFLICT (code) DO NOTHING;
SELECT setval(pg_get_serial_sequence('product', 'id'), (SELECT MAX(id) FROM product));

-- 7. Price List (Effective immediately)
INSERT INTO price_list (id, product_id, branch_id, price, effective_date, created_at) VALUES
(1, 1, 1, 185000, now(), now()),
(2, 2, 1, 125000, now(), now()),
(3, 3, 1, 850000, now(), now()),
(4, 4, 1, 350000, now(), now()),
(5, 5, 1, 4500000, now(), now()),
(6, 6, 1, 55000, now(), now()),
(7, 1, 2, 190000, now(), now()), -- TP2 has slightly different prices
(8, 2, 2, 130000, now(), now()),
(9, 4, 2, 350000, now(), now())
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('price_list', 'id'), (SELECT MAX(id) FROM price_list));

-- 8. Customers
INSERT INTO customer (id, customer_code, name, phone, is_deleted, version, created_at, updated_at) VALUES 
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'KH-001', 'Công ty XD Hòa Bình', '0901234567', false, 0, now(), now()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'KH-002', 'Anh Tuấn - Thầu xây dựng', '0909876543', false, 0, now(), now()),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'KH-003', 'Chị Lan Phương - Khách lẻ', '0912345678', false, 0, now(), now())
ON CONFLICT (customer_code) DO NOTHING;

-- 9. Suppliers
INSERT INTO supplier (id, code, name, phone, address, is_active, created_at, updated_at) VALUES 
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'SUP-001', 'Nhà máy Gạch Viglacera', '0243123456', 'KCN Tiên Sơn, Bắc Ninh', true, now(), now()),
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'SUP-002', 'Công ty Phân phối Tuya Smarthome', '0287123456', 'Q10, TP.HCM', true, now(), now())
ON CONFLICT (code) DO NOTHING;

-- 10. Initial Stock On Hand (Branch TP1)
INSERT INTO stock_on_hand (id, branch_id, product_id, quantity, avg_cost, version, created_at, updated_at, is_deleted) VALUES
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', 1, 1, 500, 150000, 0, now(), now(), false),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66', 1, 4, 50, 250000, 0, now(), now(), false),
('10eebc99-9c0b-4ef8-bb6d-6bb9bd380a77', 1, 5, 10, 3800000, 0, now(), now(), false)
ON CONFLICT (branch_id, product_id) DO NOTHING;
