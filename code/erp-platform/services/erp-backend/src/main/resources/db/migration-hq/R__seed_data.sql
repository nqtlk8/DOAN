-- ============================================================================
-- R__seed_data — Dữ liệu DEMO master của HQ. CHỈ chạy ở HQ (application-hq.yml:
-- spring.flyway.locations += classpath:db/migration-hq).
--
-- Vì sao là migration lặp lại (R__), không phải V__:
--   * HQ và mọi chi nhánh phải có CÙNG danh sách version Flyway (setup-replication.sh,
--     bootstrap.sh, add-branch.sh, check-schema-version.sh so khớp danh sách này). Một V__
--     chỉ có ở HQ làm lệch danh sách và chiếm số version của migration dùng chung kế tiếp.
--   * Flyway chạy R__ sau mọi V__ và theo thứ tự tên: "reference data" (chi nhánh, danh mục,
--     tài khoản) chạy trước "seed data", nên dữ liệu demo luôn thấy danh mục và chi nhánh.
--   * Mọi lệnh "chèn nếu chưa có" (theo tên / khóa tự nhiên): chạy lại khi file đổi không tạo trùng
--     và không ghi đè dữ liệu đã sửa qua giao diện.
--
-- Nguyên tắc (đúng quy trình hệ thống, không tự đặt id):
--   * id số do IDENTITY cấp; id UUID (NCC, khách) do gen_random_uuid().
--   * Mã sản phẩm PRD-<epoch ms>, NCC SUP-<epoch ms> theo quy ước frontend (ProductList, SupplierList).
--   * Mã khách hàng KH + 8 ký tự UUID viết hoa, đúng quy tắc CustomerWriteService khi không nhập mã.
--   * Ngày ghi nhận = now() (thời điểm migration chạy), giống khi tạo qua giao diện.
--   * Bảng giá: đúng một giá cho mỗi cặp (sản phẩm, chi nhánh) — uk_price_list_product_branch.
--
-- KHÔNG chứa giao dịch (phiếu nhập, hóa đơn, trả hàng): các bảng đó do CHI NHÁNH sở hữu
-- (single-writer, scripts/replication-tables.conf). Giao dịch demo: scripts/seed-demo.py qua API.
-- ============================================================================

-- B1. Sản phẩm demo (12 sản phẩm, 10 danh mục con). Giá bán theo chi nhánh ở B4.
INSERT INTO product (code, name, category_id, base_unit, attributes_cache, is_active, created_at, updated_at)
SELECT 'PRD-' || (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint + s.ord),
       s.name, c.id, s.base_unit, NULL, true, now(), now()
FROM (VALUES
        (1,  'Bồn cầu 1 khối TOTO CW166RB/TC384CVK',   'ban-cau',            'Bộ'),
        (2,  'Bồn cầu 1 khối TOTO CW166RB/TCF33320GAA', 'bon-cau-1-khoi',     'Bộ'),
        (3,  'Bồn cầu 1 khối TOTO CW166RB/TCF34320GAA', 'bon-cau-1-khoi',     'Bộ'),
        (4,  'Bồn cầu 2 khối men chống bám',           'bon-cau-2-khoi',     'Bộ'),
        (5,  'Bồn cầu thông minh nắp rửa điện tử',     'bon-cau-thong-minh', 'Bộ'),
        (6,  'Chậu rửa đặt bàn TOTO LT4706',           'chau-rua',           'Cái'),
        (7,  'Chậu rửa âm bàn TOTO LT548',             'chau-rua',           'Cái'),
        (8,  'Đèn chùm pha lê cao cấp Luxury L500',    'den-trang-tri',      'Bộ'),
        (9,  'Rèm vải chống nắng châu Âu cao cấp',     'rem-cua',            'Bộ'),
        (10, 'Sofa da thật Italy hiện đại',            'sofa',               'Bộ'),
        (11, 'Bàn trà mặt đá cẩm thạch',               'ban-tra',            'Cái'),
        (12, 'Bình hoa pha lê châu Âu nghệ thuật',     'do-trang-tri',       'Cái')
     ) AS s(ord, name, cat_code, base_unit)
JOIN category c ON c.code = s.cat_code
WHERE NOT EXISTS (SELECT 1 FROM product p WHERE p.name = s.name)
ORDER BY s.ord;

-- B2. Nhà cung cấp demo (dùng chung toàn hệ thống, không có branch_id).
INSERT INTO supplier (id, code, name, phone, email, address, tax_code, is_active, created_at, updated_at)
SELECT gen_random_uuid(),
       'SUP-' || (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint + s.ord),
       s.name, s.phone, s.email, s.address, s.tax_code, true, now(), now()
FROM (VALUES
        (1, 'Nhà phân phối Thiết bị Vệ sinh Minh Phát', '0283 8123 456', 'kinhdoanh@minhphat-demo.vn', 'KCN Tân Tạo, Bình Tân, TP.HCM',  '0312345671'),
        (2, 'Công ty TNHH Thiết bị Vệ sinh Phú Thịnh',  '0274 3812 999', 'banhang@phuthinh-demo.vn',   'KCN Sóng Thần, Dĩ An, Bình Dương', '3701234562'),
        (3, 'Công ty CP Đèn Trang Trí Ánh Sáng',        '0243 8566 120', 'sales@anhsang-demo.vn',      'Thanh Xuân, Hà Nội',               '0109876544'),
        (4, 'Xưởng Nội thất Hoàng Long',                '0283 9300 456', 'hoanglong@noithat-demo.vn',  'Q.7, TP.HCM',                      '0318765435'),
        (5, 'Đại lý Rèm & Decor Mộc An',                '0287 1088 789', 'order@mocan-demo.vn',        'Q.10, TP.HCM',                     '0315678903')
     ) AS s(ord, name, phone, email, address, tax_code)
WHERE NOT EXISTS (SELECT 1 FROM supplier x WHERE x.name = s.name)
ORDER BY s.ord;

-- B3. Khách hàng demo. Khách có branch_id = chi nhánh riêng; branch_id NULL = khách dùng chung.
INSERT INTO customer (id, customer_code, name, phone, email, address, tax_code, branch_id,
                      version, is_deleted, created_at, updated_at)
SELECT gen_random_uuid(),
       'KH' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8)),
       s.name, s.phone, NULL, s.address, s.tax_code,
       (SELECT b.id FROM branch b WHERE b.code = s.branch_code),
       0, false, now(), now()
FROM (VALUES
        (1, 'Công ty TNHH Xây dựng An Phú',          '0901 234 567', 'Thủ Đức, TP.HCM', '0311112223', NULL),
        (2, 'Anh Tuấn - Thầu xây dựng',              '0909 876 543', 'Gò Vấp, TP.HCM',  NULL,         NULL),
        (3, 'Chị Lan - Khách lẻ',                    '0912 345 678', NULL,              NULL,         NULL),
        (4, 'Công ty CP Nội thất Hoàng Gia',         '0938 111 222', 'Q.3, TP.HCM',     '0314445556', NULL),
        (5, 'Anh Minh - Thợ lắp đặt',                '0977 333 444', NULL,              NULL,         'TP1'),
        (6, 'Công ty TNHH Nội thất Nam Sài Gòn',     '0283 7700 888', 'Q.7, TP.HCM',    '0319990001', 'TP2')
     ) AS s(ord, name, phone, address, tax_code, branch_code)
WHERE NOT EXISTS (SELECT 1 FROM customer x WHERE x.name = s.name)
ORDER BY s.ord;

-- B4. Bảng giá niêm yết: đúng một giá cho mỗi (sản phẩm, chi nhánh). Giá TP1 và TP2 có thể khác nhau.
INSERT INTO price_list (product_id, branch_id, price, effective_date, created_at)
SELECT p.id, b.id, s.price, now(), now()
FROM (VALUES
        ('Bồn cầu 1 khối TOTO CW166RB/TC384CVK',    'TP1', 27941200), ('Bồn cầu 1 khối TOTO CW166RB/TC384CVK',    'TP2', 28500000),
        ('Bồn cầu 1 khối TOTO CW166RB/TCF33320GAA', 'TP1', 44787350), ('Bồn cầu 1 khối TOTO CW166RB/TCF33320GAA', 'TP2', 45500000),
        ('Bồn cầu 1 khối TOTO CW166RB/TCF34320GAA', 'TP1', 52063350), ('Bồn cầu 1 khối TOTO CW166RB/TCF34320GAA', 'TP2', 53000000),
        ('Bồn cầu 2 khối men chống bám',            'TP1',  4200000), ('Bồn cầu 2 khối men chống bám',            'TP2',  4300000),
        ('Bồn cầu thông minh nắp rửa điện tử',      'TP1', 38500000), ('Bồn cầu thông minh nắp rửa điện tử',      'TP2', 39000000),
        ('Chậu rửa đặt bàn TOTO LT4706',            'TP1',  4000000), ('Chậu rửa đặt bàn TOTO LT4706',            'TP2',  4100000),
        ('Chậu rửa âm bàn TOTO LT548',              'TP1',  1800000), ('Chậu rửa âm bàn TOTO LT548',              'TP2',  1850000),
        ('Đèn chùm pha lê cao cấp Luxury L500',     'TP1',  4800000), ('Đèn chùm pha lê cao cấp Luxury L500',     'TP2',  4900000),
        ('Rèm vải chống nắng châu Âu cao cấp',      'TP1',   950000), ('Rèm vải chống nắng châu Âu cao cấp',      'TP2',   990000),
        ('Sofa da thật Italy hiện đại',             'TP1', 35000000), ('Sofa da thật Italy hiện đại',             'TP2', 35500000),
        ('Bàn trà mặt đá cẩm thạch',                'TP1',  3800000), ('Bàn trà mặt đá cẩm thạch',                'TP2',  3900000),
        ('Bình hoa pha lê châu Âu nghệ thuật',      'TP1',   750000), ('Bình hoa pha lê châu Âu nghệ thuật',      'TP2',   790000)
     ) AS s(product_name, branch_code, price)
JOIN product p ON p.name = s.product_name
JOIN branch  b ON b.code = s.branch_code
ON CONFLICT (product_id, branch_id) DO NOTHING;

-- B5. Ngưỡng cảnh báo tồn tối thiểu theo (sản phẩm, chi nhánh) — phục vụ UC13 (cảnh báo tồn thấp).
INSERT INTO inventory_alert_config (product_id, branch_id, min_quantity_threshold, email_recipients,
                                    is_active, created_at, updated_at)
SELECT p.id, b.id, s.threshold, NULL, true, now(), now()
FROM (VALUES
        ('Sofa da thật Italy hiện đại',             'TP1', 4),
        ('Bồn cầu 1 khối TOTO CW166RB/TCF33320GAA', 'TP1', 5),
        ('Chậu rửa âm bàn TOTO LT548',              'TP1', 10),
        ('Đèn chùm pha lê cao cấp Luxury L500',     'TP2', 3),
        ('Bình hoa pha lê châu Âu nghệ thuật',      'TP2', 5)
     ) AS s(product_name, branch_code, threshold)
JOIN product p ON p.name = s.product_name
JOIN branch  b ON b.code = s.branch_code
ON CONFLICT (product_id, branch_id) DO NOTHING;
