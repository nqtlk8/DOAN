-- ============================================================================
-- MODULE: SEED DATA (For Docker / Manual Testing)
-- ============================================================================

INSERT INTO branch (id, code, name, internal_url, is_active, created_at, updated_at) 
VALUES 
(1, 'TP1', 'Chi nhanh TP1', 'http://branch-tp1.local:80', true, now(), now()),
(2, 'TP2', 'Chi nhanh TP2', 'http://branch-tp2.local:80', true, now(), now()),
(3, 'HCM01', 'Chi nhanh HCM01', 'http://branch-hcm01.local:80', true, now(), now())
ON CONFLICT (code) DO UPDATE SET internal_url = EXCLUDED.internal_url;

SELECT setval(pg_get_serial_sequence('branch', 'id'), (SELECT MAX(id) FROM branch));

INSERT INTO role (id, code, name) VALUES 
(1, 'STAFF', 'Nhan vien'),
(2, 'ADMIN', 'Quan tri vien')
ON CONFLICT (code) DO NOTHING;
SELECT setval(pg_get_serial_sequence('role', 'id'), (SELECT MAX(id) FROM role));

INSERT INTO user_account (id, username, password_hash, full_name, is_active, created_at, updated_at)
VALUES 
(1, 'admin', '$2a$10$iGQ7Sy5sYyDeakHKzeu3AOpipgneKau3yF8Oq8BQRqofkhgevv..G', 'Quan tri vien', true, now(), now()),
(2, 'staff_tp1', '$2a$10$iGQ7Sy5sYyDeakHKzeu3AOpipgneKau3yF8Oq8BQRqofkhgevv..G', 'Nhan vien TP1', true, now(), now()),
(3, 'staff_tp2', '$2a$10$iGQ7Sy5sYyDeakHKzeu3AOpipgneKau3yF8Oq8BQRqofkhgevv..G', 'Nhan vien TP2', true, now(), now())
ON CONFLICT (username) DO NOTHING;
SELECT setval(pg_get_serial_sequence('user_account', 'id'), (SELECT MAX(id) FROM user_account));

DELETE FROM user_branch_role WHERE user_id IN (1, 2, 3);
INSERT INTO user_branch_role (user_id, role_id, branch_id) VALUES (1, 2, NULL);
INSERT INTO user_branch_role (user_id, role_id, branch_id) VALUES (2, 1, 1);
INSERT INTO user_branch_role (user_id, role_id, branch_id) VALUES (3, 1, 2);

INSERT INTO category (id, code, name, is_active, created_at, updated_at) VALUES 
(1, 'CAT01', 'Dien thoai', true, now(), now()),
(2, 'CAT02', 'Phu kien', true, now(), now())
ON CONFLICT (code) DO NOTHING;
SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT MAX(id) FROM category));

INSERT INTO product (id, code, name, category_id, base_unit, is_active, created_at, updated_at) VALUES 
(1, 'P001', 'iPhone 15 Pro', 1, 'Cai', true, now(), now()),
(2, 'P002', 'Op lung iPhone 15', 2, 'Cai', true, now(), now())
ON CONFLICT (code) DO NOTHING;
SELECT setval(pg_get_serial_sequence('product', 'id'), (SELECT MAX(id) FROM product));

INSERT INTO customer (id, customer_code, name, phone, is_deleted, version, created_at, updated_at) VALUES 
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'C001', 'Khach hang VIP 1', '0901234567', false, 0, now(), now()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'C002', 'Khach hang Le', '0909876543', false, 0, now(), now())
ON CONFLICT (customer_code) DO NOTHING;

INSERT INTO supplier (id, code, name, is_active, created_at, updated_at) VALUES 
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'SUP01', 'Nha cung cap Apple', true, now(), now()),
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'SUP02', 'Nha cung cap Phu kien', true, now(), now())
ON CONFLICT (code) DO NOTHING;

