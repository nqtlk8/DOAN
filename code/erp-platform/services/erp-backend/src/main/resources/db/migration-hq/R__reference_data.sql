-- ============================================================================
-- R__reference_data — Dữ liệu hệ thống bắt buộc. CHỈ chạy ở HQ
-- (application-hq.yml: spring.flyway.locations += classpath:db/migration-hq).
--
-- Vì sao chỉ ở HQ: các bảng này được replicate HQ -> Branch với copy_data = true.
-- Nếu chi nhánh cũng tự chèn cùng dữ liệu, lần copy đầu tiên sẽ lỗi trùng khóa.
--
-- Đây là migration lặp lại (repeatable): Flyway chạy lại khi nội dung file đổi.
-- Mọi lệnh đều "chèn nếu chưa có" — KHÔNG ghi đè dữ liệu đã bị sửa qua giao diện.
--
-- Ngoài dữ liệu hệ thống, file chứa sẵn cây DANH MỤC SẢN PHẨM (bảng category) dùng chung với
-- website bán hàng (apps/web-public). Không chứa sản phẩm, NCC, khách hàng hay giao dịch —
-- dữ liệu demo: scripts/seed-demo.py (chạy tay, qua API).
-- ============================================================================

-- 1. Chi nhánh. id cố định vì biến môi trường BRANCH_ID của app chi nhánh
--    (docker-compose.yml) và claim branchId trong JWT phải khớp id này.
INSERT INTO branch (id, code, name, internal_url, is_active, created_at, updated_at) VALUES
    (1, 'TP1', 'Chi nhánh Trung Tâm (TP1)', 'http://localhost:81', true, now(), now()),
    (2, 'TP2', 'Chi nhánh Quận 7 (TP2)',    'http://localhost:82', true, now(), now())
ON CONFLICT (code) DO NOTHING;

SELECT setval(pg_get_serial_sequence('branch', 'id'), GREATEST((SELECT MAX(id) FROM branch), 1));

-- 2. Vai trò. Ứng dụng tra theo code (STAFF/ADMIN), id do sequence cấp.
INSERT INTO role (code, name) VALUES
    ('STAFF', 'Nhân viên bán hàng/Kho'),
    ('ADMIN', 'Quản trị viên Hệ thống')
ON CONFLICT (code) DO NOTHING;

-- 3. Tài khoản mặc định. Mật khẩu: "password" (BCrypt). Đổi sau khi triển khai thật.
INSERT INTO user_account (username, password_hash, full_name, is_active, created_at, updated_at) VALUES
    ('admin',     '$2a$10$F/P0AgymR4shq0Klom269uWwtOZzNDMsLs.0PqdzGsDBhCbnsXtlm', 'Quản trị viên HQ', true, now(), now()),
    ('staff_tp1', '$2a$10$F/P0AgymR4shq0Klom269uWwtOZzNDMsLs.0PqdzGsDBhCbnsXtlm', 'Nhân viên TP1',    true, now(), now()),
    ('staff_tp2', '$2a$10$F/P0AgymR4shq0Klom269uWwtOZzNDMsLs.0PqdzGsDBhCbnsXtlm', 'Nhân viên TP2',    true, now(), now())
ON CONFLICT (username) DO NOTHING;

-- 4. Gán vai trò (tra theo username / role code / branch code, không phụ thuộc id).
INSERT INTO user_branch_role (user_id, role_id, branch_id)
SELECT u.id, r.id, b.id
FROM (VALUES
        ('admin',     'ADMIN', NULL),   -- ADMIN toàn hệ thống (branch_id NULL)
        ('staff_tp1', 'STAFF', 'TP1'),
        ('staff_tp2', 'STAFF', 'TP2')
     ) AS m(username, role_code, branch_code)
JOIN user_account u ON u.username = m.username
JOIN role r ON r.code = m.role_code
LEFT JOIN branch b ON b.code = m.branch_code
WHERE NOT EXISTS (
    SELECT 1 FROM user_branch_role x
    WHERE x.user_id = u.id
      AND x.role_id = r.id
      AND x.branch_id IS NOT DISTINCT FROM b.id
);

-- 5. Danh mục sản phẩm — cùng cây danh mục với website bán hàng
--    (apps/web-public/src/test/mockData.ts: MOCK_CATEGORIES).
--    code = slug của website để hai bên tra chéo được; name giữ đúng chữ hiển thị trên website.
--    Sản phẩm gắn vào danh mục CON; danh mục gốc chỉ dùng để nhóm (dropdown của erp-frontend).
INSERT INTO category (code, name, parent_id, is_active, created_at, updated_at) VALUES
    ('vlxd', 'VẬT LIỆU XÂY DỰNG', NULL, true, now(), now()),
    ('ttnt', 'TRANG TRÍ NỘI THẤT', NULL, true, now(), now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO category (code, name, parent_id, is_active, created_at, updated_at)
SELECT c.code, c.name, p.id, true, now(), now()
FROM (VALUES
        (1,  'vlxd', 'ban-cau',            'Bàn Cầu - Bồn Tiểu'),
        (2,  'vlxd', 'bon-cau-1-khoi',     'Bồn cầu 1 khối'),
        (3,  'vlxd', 'bon-cau-2-khoi',     'Bồn cầu 2 khối'),
        (4,  'vlxd', 'bon-cau-thong-minh', 'Bồn cầu thông minh'),
        (5,  'vlxd', 'chau-rua',           'Chậu Rửa - Lavabo'),
        (6,  'ttnt', 'den-trang-tri',      'Đèn Trang Trí'),
        (7,  'ttnt', 'rem-cua',            'Rèm Cửa'),
        (8,  'ttnt', 'sofa',               'Sofa'),
        (9,  'ttnt', 'ban-tra',            'Bàn Trà'),
        (10, 'ttnt', 'do-trang-tri',       'Đồ Trang Trí')
     ) AS c(ord, parent_code, code, name)
JOIN category p ON p.code = c.parent_code
ORDER BY c.ord   -- id tăng theo đúng thứ tự trên website (dropdown sắp theo id)
ON CONFLICT (code) DO NOTHING;
