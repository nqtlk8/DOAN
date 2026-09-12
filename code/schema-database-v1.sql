-- ============================================================
-- L?CH S?: ��y l� thi?t k? schema v1 ban d?u c?a d? �n.
-- Schema TH?C T? dang ch?y du?c qu?n l� b?i Flyway t?i:
-- erp-platform/services/erp-backend/src/main/resources/db/migration/
-- File n�y gi? l?i d? tham kh?o l?ch s? thi?t k?, KH�NG ph?n �nh
-- schema hi?n h�nh (v� d?: unit_conversion, supplier_purchase_order,
-- outbound_receipt, stock_transfer, avg_cost d� b? lo?i b? ? V11).
-- ============================================================
-- ============================================================================
-- DATABASE SCHEMA — Website & ERP Cửa hàng VLXD & TTNT
-- PostgreSQL 15+ (yêu cầu bắt buộc — dùng row filter trong publication cho
-- logical replication Hub-and-Spoke, xem tài liệu Kiến trúc v2 Phần 4)
--
-- NGUYÊN TẮC THIẾT KẾ:
--   1. Toàn bộ bảng thiết kế đạt CHUẨN 3NF trước (xem phần ghi chú NF ở mỗi
--      bảng và tài liệu "giai-thich-chuan-hoa-database.md" đi kèm).
--   2. Các quyết định DENORMALIZE (phá chuẩn có chủ đích để tối ưu hiệu năng)
--      được đánh dấu rõ bằng khối comment "[DENORMALIZED]" kèm lý do — KHÔNG
--      phá chuẩn tuỳ tiện ở bất kỳ chỗ nào khác ngoài các điểm đã đánh dấu.
--   3. Bảng nào là dữ liệu MASTER (HQ) hay BRANCH-LOCAL được ghi rõ trong
--      comment đầu mỗi bảng — đúng theo kiến trúc Hub-and-Spoke đã thiết kế.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- cho gen_random_uuid()

-- ============================================================================
-- MODULE: IDENTITY (HQ — master data)
-- ============================================================================

CREATE TABLE branch (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    address         VARCHAR(500),
    phone           VARCHAR(20),
    opening_hours   VARCHAR(100),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE branch IS 'HQ master. 3NF: mỗi cột phụ thuộc trực tiếp vào id, không có cột dẫn xuất.';

CREATE TABLE user_account (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(20),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,  -- ADMIN_TONG, QUAN_LY_CHI_NHANH, NHAN_VIEN_BAN_HANG, THU_KHO, KE_TOAN
    name            VARCHAR(200) NOT NULL
);

CREATE TABLE permission (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,  -- VD: product:write, invoice:confirm
    description     VARCHAR(255)
);

-- Bảng liên kết (junction) chuẩn 3NF cho quan hệ N-N Role <-> Permission
CREATE TABLE role_permission (
    role_id         SMALLINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id   SMALLINT NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Gán role cho user, có thể scope theo 1 chi nhánh cụ thể hoặc toàn hệ thống
-- (branch_id = NULL nghĩa là role áp dụng toàn hệ thống, VD ADMIN_TONG)
CREATE TABLE user_branch_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT   NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role_id         SMALLINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    branch_id       BIGINT   REFERENCES branch(id) ON DELETE CASCADE  -- NULL = toàn hệ thống
);
CREATE UNIQUE INDEX ux_user_branch_role ON user_branch_role (user_id, role_id, COALESCE(branch_id, 0));
COMMENT ON TABLE user_branch_role IS 'Triển khai RBAC Role x Branch scope — nguồn dữ liệu cho @BranchScoped Aspect.';

-- ============================================================================
-- MODULE: CATALOG (HQ — master data, replicate READ-ONLY xuống branch)
-- ============================================================================

CREATE TABLE category (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT REFERENCES category(id) ON DELETE RESTRICT,  -- tự tham chiếu, phân cấp
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_category_parent ON category (parent_id);
COMMENT ON TABLE category IS '3NF: self-reference cho danh mục cha/con, không lưu đường dẫn/level dẫn xuất trong bảng này (tính động khi cần, tránh dữ liệu suy ra từ dữ liệu khác — vi phạm 3NF).';

CREATE TABLE unit_of_measure (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,   -- 'M2', 'VIEN', 'THUNG', 'BAO', 'KG'
    name            VARCHAR(100) NOT NULL
);

CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
    sku             VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(300) NOT NULL,
    base_unit_id    SMALLINT NOT NULL REFERENCES unit_of_measure(id),
    description     TEXT,
    image_url       VARCHAR(500),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    -- [DENORMALIZED — xem giai-thich-chuan-hoa-database.md mục 2.1]
    -- Cache toàn bộ thuộc tính động (từ product_attribute_value) dạng JSONB
    -- để trang public đọc 1 lần duy nhất, không cần JOIN nhiều bảng EAV mỗi
    -- request. Đồng bộ qua application service mỗi khi product_attribute_value
    -- thay đổi — bảng product_attribute_value (chuẩn 3NF) mới là nguồn sự thật.
    attributes_cache JSONB      NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_product_category ON product (category_id);
CREATE INDEX ix_product_name_trgm ON product USING gin (name gin_trgm_ops); -- cần extension pg_trgm cho tìm kiếm gần đúng
CREATE INDEX ix_product_attributes_cache ON product USING gin (attributes_cache);

CREATE TABLE unit_conversion (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT   NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    from_unit_id    SMALLINT NOT NULL REFERENCES unit_of_measure(id),
    to_unit_id      SMALLINT NOT NULL REFERENCES unit_of_measure(id),
    ratio           NUMERIC(18,6) NOT NULL CHECK (ratio > 0)  -- 1 from_unit = ratio * to_unit
);
CREATE UNIQUE INDEX ux_unit_conversion ON unit_conversion (product_id, from_unit_id, to_unit_id);
COMMENT ON TABLE unit_conversion IS '3NF: tỷ lệ quy đổi khác nhau theo từng sản phẩm (VD 1 thùng gạch A = 1.44 m2, thùng gạch B = 1.2 m2) nên không thể gộp cứng vào unit_of_measure.';

-- Thuộc tính động — thiết kế EAV chuẩn 3NF (nguồn sự thật)
CREATE TABLE attribute_definition (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,   -- 'color', 'size', 'origin'
    name            VARCHAR(200) NOT NULL,
    data_type       VARCHAR(20)  NOT NULL CHECK (data_type IN ('TEXT','NUMBER','BOOLEAN')),
    unit_label      VARCHAR(50)                      -- VD 'mm', 'kg' — chỉ để hiển thị
);

-- Khai báo thuộc tính nào áp dụng cho danh mục nào (N-N, junction chuẩn 3NF)
CREATE TABLE category_attribute (
    category_id             BIGINT   NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    attribute_definition_id SMALLINT NOT NULL REFERENCES attribute_definition(id) ON DELETE CASCADE,
    is_required              BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (category_id, attribute_definition_id)
);

CREATE TABLE product_attribute_value (
    id                       BIGSERIAL PRIMARY KEY,
    product_id               BIGINT   NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    attribute_definition_id  SMALLINT NOT NULL REFERENCES attribute_definition(id) ON DELETE CASCADE,
    value_text               VARCHAR(500),
    value_number              NUMERIC(18,4),
    value_boolean             BOOLEAN
);
CREATE UNIQUE INDEX ux_product_attribute ON product_attribute_value (product_id, attribute_definition_id);
COMMENT ON TABLE product_attribute_value IS '3NF thuần (EAV) — nguồn sự thật cho thuộc tính động. product.attributes_cache (JSONB) là bản sao denormalize để tối ưu đọc, KHÔNG sửa trực tiếp vào cache mà không qua bảng này.';

-- Giá niêm yết theo chi nhánh — theo thời gian hiệu lực
CREATE TABLE price_list (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT      NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    branch_id       BIGINT      NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
    price           NUMERIC(18,2) NOT NULL CHECK (price >= 0),
    effective_date  DATE        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_price_list_lookup ON price_list (product_id, branch_id, effective_date DESC);
COMMENT ON TABLE price_list IS '3NF: lưu lịch sử giá theo effective_date thay vì chỉ 1 cột current_price trên product (tránh mất lịch sử — cần cho đối soát và báo cáo).';

-- ============================================================================
-- MODULE: CRM (customer = HQ master; receivable_debt, customer_product_price
-- = BRANCH-LOCAL, theo đúng thiết kế Hub-and-Spoke — xem Kiến trúc v2 Phần 4.3)
-- ============================================================================

CREATE TABLE customer (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL UNIQUE,
    full_name       VARCHAR(200) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(200),
    address         VARCHAR(500),
    customer_type   VARCHAR(20)  NOT NULL DEFAULT 'RETAIL' CHECK (customer_type IN ('RETAIL','CONSTRUCTION')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_customer_phone ON customer (phone);

-- Số dư công nợ phải thu — BRANCH-LOCAL (mỗi chi nhánh giữ 1 sổ nợ riêng cho
-- cùng 1 khách hàng, đúng nguyên tắc "ACID tại nơi phát sinh giao dịch" đã
-- thống nhất trong kiến trúc — tránh distributed transaction xuyên chi nhánh)
CREATE TABLE receivable_debt (
    customer_id     BIGINT NOT NULL,
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, branch_id)
);
COMMENT ON TABLE receivable_debt IS 'BRANCH-LOCAL. 3NF: bảng chỉ giữ số dư hiện tại (không suy ra từ tổng sales_invoice mỗi lần đọc — đây là quyết định [DENORMALIZED] có chủ đích, xem mục 2.4 tài liệu giải thích).';

CREATE TABLE customer_product_price (
    customer_id     BIGINT NOT NULL,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    last_price      NUMERIC(18,2) NOT NULL,
    last_invoice_id UUID,  -- FK logic tới sales_invoice.id, không đặt FK cứng để tránh phụ thuộc vòng lúc khởi tạo dữ liệu
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, product_id, branch_id)
);
COMMENT ON TABLE customer_product_price IS 'BRANCH-LOCAL, [DENORMALIZED có chủ đích]: lưu sẵn giá gần nhất thay vì query lịch sử sales_invoice_line mỗi lần thêm dòng hoá đơn (hot path — chạy mỗi khi bán hàng).';

-- ============================================================================
-- MODULE: PROCUREMENT (BRANCH-LOCAL — mỗi chi nhánh tự quản lý NCC của mình)
-- ============================================================================

CREATE TABLE supplier (
    id              BIGSERIAL PRIMARY KEY,
    branch_id       BIGINT      NOT NULL REFERENCES branch(id),
    code            VARCHAR(30) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    phone           VARCHAR(20),
    address         VARCHAR(500),
    tax_code        VARCHAR(30),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_supplier_branch_code ON supplier (branch_id, code);

CREATE TABLE payable_debt (
    supplier_id     BIGINT PRIMARY KEY REFERENCES supplier(id),
    current_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Đơn đặt hàng NCC — CÔNG CỤ THEO DÕI, không ảnh hưởng stock_on_hand khi tạo
-- (xem Kiến trúc v2 mục 5.2.7 — ràng buộc nghiệp vụ bắt buộc ở tầng application)
CREATE TABLE supplier_purchase_order (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_no           VARCHAR(30) NOT NULL,
    supplier_id     BIGINT      NOT NULL REFERENCES supplier(id),
    branch_id       BIGINT      NOT NULL REFERENCES branch(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RECEIVED','CANCELLED')),
    expected_date   DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_po_branch_no ON supplier_purchase_order (branch_id, po_no);
CREATE INDEX ix_po_status ON supplier_purchase_order (branch_id, status);

CREATE TABLE supplier_purchase_order_line (
    id                  BIGSERIAL PRIMARY KEY,
    purchase_order_id   UUID   NOT NULL REFERENCES supplier_purchase_order(id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES product(id),
    ordered_quantity    NUMERIC(18,3) NOT NULL CHECK (ordered_quantity > 0),
    received_quantity   NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (received_quantity >= 0)
);

-- ============================================================================
-- MODULE: INVENTORY (BRANCH-LOCAL)
-- ============================================================================

-- [DENORMALIZED có chủ đích, xem mục 2.3 tài liệu giải thích]
-- Đây là bảng số dư tổng hợp (running balance), KHÔNG phải chuẩn 3NF thuần
-- (về mặt lý thuyết, quantity có thể SUM từ toàn bộ inbound/outbound line).
-- Denormalize bắt buộc vì đây là hot path (kiểm tra/trừ kho mỗi giao dịch bán)
-- — SUM động toàn bộ lịch sử mỗi lần bán sẽ không đạt NFR hiệu năng.
CREATE TABLE stock_on_hand (
    product_id      BIGINT NOT NULL REFERENCES product(id),
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    quantity        NUMERIC(18,3) NOT NULL DEFAULT 0,   -- CHO PHÉP ÂM — quyết định nghiệp vụ đã chốt (Kiến trúc v2 mục 5.2.4)
    avg_cost        NUMERIC(18,4) NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,           -- optimistic locking (JPA @Version)
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, branch_id)
);
COMMENT ON TABLE stock_on_hand IS 'KHÔNG có CHECK quantity >= 0 — âm kho là trạng thái nghiệp vụ hợp lệ (bán khống có kiểm soát).';

CREATE TABLE inbound_receipt (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_no         VARCHAR(30) NOT NULL,
    branch_id          BIGINT      NOT NULL REFERENCES branch(id),
    supplier_id        BIGINT      REFERENCES supplier(id),
    purchase_order_id  UUID        REFERENCES supplier_purchase_order(id),  -- tuỳ chọn, liên kết PO
    status             VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    is_on_credit       BOOLEAN     NOT NULL DEFAULT FALSE,  -- mua chịu -> phát sinh payable_debt
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at       TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_inbound_branch_no ON inbound_receipt (branch_id, receipt_no);

CREATE TABLE inbound_receipt_line (
    id              BIGSERIAL PRIMARY KEY,
    receipt_id      UUID   NOT NULL REFERENCES inbound_receipt(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    quantity        NUMERIC(18,3) NOT NULL CHECK (quantity > 0),
    unit_cost       NUMERIC(18,4) NOT NULL CHECK (unit_cost >= 0)
);

-- Dùng cho xuất kho NỘI BỘ (chuyển kho, huỷ/hao hụt) — KHÔNG dùng cho bán
-- hàng (bán hàng dùng sales_invoice, xem module order bên dưới)
CREATE TABLE outbound_receipt (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_no      VARCHAR(30) NOT NULL,
    branch_id       BIGINT      NOT NULL REFERENCES branch(id),
    reason          VARCHAR(20) NOT NULL CHECK (reason IN ('TRANSFER','DAMAGE','OTHER')),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at    TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_outbound_branch_no ON outbound_receipt (branch_id, receipt_no);

CREATE TABLE outbound_receipt_line (
    id              BIGSERIAL PRIMARY KEY,
    receipt_id      UUID   NOT NULL REFERENCES outbound_receipt(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    quantity        NUMERIC(18,3) NOT NULL CHECK (quantity > 0)
);

CREATE TABLE stock_transfer (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_no         VARCHAR(30) NOT NULL,
    from_branch_id      BIGINT      NOT NULL REFERENCES branch(id),
    to_branch_id        BIGINT      NOT NULL REFERENCES branch(id) CHECK (to_branch_id <> from_branch_id),
    status              VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
                            CHECK (status IN ('REQUESTED','IN_TRANSIT','RECEIVED','CANCELLED')),
    outbound_receipt_id UUID REFERENCES outbound_receipt(id),
    inbound_receipt_id  UUID REFERENCES inbound_receipt(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stock_transfer_line (
    id              BIGSERIAL PRIMARY KEY,
    transfer_id     UUID   NOT NULL REFERENCES stock_transfer(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    quantity        NUMERIC(18,3) NOT NULL CHECK (quantity > 0)
);

-- ============================================================================
-- MODULE: ORDER (BRANCH-LOCAL) — hoá đơn bán hàng, đặt hàng khách, trả hàng
-- ============================================================================

CREATE TABLE sales_invoice (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- UUID: tránh xung đột PK khi hợp nhất về HQ (xem Kiến trúc v2)
    invoice_no      VARCHAR(30)  NOT NULL,
    customer_id     BIGINT       NOT NULL REFERENCES customer(id),
    branch_id       BIGINT       NOT NULL REFERENCES branch(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    -- [DENORMALIZED có chủ đích, xem mục 2.2 tài liệu giải thích]
    -- previous_debt/total_amount/remaining_debt là SNAPSHOT lưu cứng tại thời
    -- điểm CONFIRMED — vi phạm 3NF có chủ ý (đáng lẽ suy ra được từ
    -- receivable_debt + sales_invoice_line), nhưng bắt buộc để hoá đơn cũ
    -- không đổi số liệu khi công nợ khách hàng thay đổi ở giao dịch sau.
    previous_debt   NUMERIC(18,2),
    total_amount    NUMERIC(18,2),
    amount_paid     NUMERIC(18,2) NOT NULL DEFAULT 0,
    remaining_debt  NUMERIC(18,2),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    confirmed_at    TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_invoice_branch_no ON sales_invoice (branch_id, invoice_no);
CREATE INDEX ix_invoice_customer ON sales_invoice (customer_id, branch_id);

CREATE TABLE sales_invoice_line (
    id                      BIGSERIAL PRIMARY KEY,
    invoice_id              UUID   NOT NULL REFERENCES sales_invoice(id) ON DELETE CASCADE,
    product_id              BIGINT NOT NULL REFERENCES product(id),
    quantity                NUMERIC(18,3) NOT NULL CHECK (quantity > 0),
    default_unit_price      NUMERIC(18,2) NOT NULL,
    unit_price               NUMERIC(18,2) NOT NULL,
    is_price_overridden      BOOLEAN NOT NULL DEFAULT FALSE,
    -- [DENORMALIZED có chủ đích] Giá vốn snapshot tại thời điểm bán, phục vụ
    -- báo cáo lợi nhuận gộp ổn định dù avg_cost trên stock_on_hand thay đổi
    -- sau đó (do các giao dịch nhập/xuất khác diễn ra tiếp theo).
    unit_cost_snapshot        NUMERIC(18,4),
    customer_order_line_id    BIGINT  -- FK logic tới customer_order_line, xem bảng bên dưới (đặt FK cứng sau khi bảng đó được tạo)
);
CREATE INDEX ix_invoice_line_invoice ON sales_invoice_line (invoice_id);
CREATE INDEX ix_invoice_line_product ON sales_invoice_line (product_id);

CREATE TABLE customer_order (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_no        VARCHAR(30) NOT NULL,
    customer_id     BIGINT      NOT NULL REFERENCES customer(id),
    branch_id       BIGINT      NOT NULL REFERENCES branch(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_customer_order_branch_no ON customer_order (branch_id, order_no);

CREATE TABLE customer_order_line (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            UUID   NOT NULL REFERENCES customer_order(id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES product(id),
    ordered_quantity     NUMERIC(18,3) NOT NULL CHECK (ordered_quantity > 0),
    agreed_price          NUMERIC(18,2) NOT NULL,
    fulfilled_quantity    NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (fulfilled_quantity >= 0),
    version                BIGINT NOT NULL DEFAULT 0,  -- optimistic locking
    CHECK (fulfilled_quantity <= ordered_quantity)
);
CREATE INDEX ix_customer_order_line_lookup ON customer_order_line (order_id, product_id);

-- Bổ sung FK cứng cho sales_invoice_line -> customer_order_line (tạo sau vì
-- customer_order_line phải tồn tại trước)
ALTER TABLE sales_invoice_line
    ADD CONSTRAINT fk_invoice_line_order_line
    FOREIGN KEY (customer_order_line_id) REFERENCES customer_order_line(id);

CREATE TABLE goods_return (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_no           VARCHAR(30) NOT NULL,
    customer_id         BIGINT      NOT NULL REFERENCES customer(id),
    branch_id           BIGINT      NOT NULL REFERENCES branch(id),
    original_invoice_id UUID        REFERENCES sales_invoice(id),  -- tuỳ chọn
    total_return_amount NUMERIC(18,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','CANCELLED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_return_branch_no ON goods_return (branch_id, return_no);

CREATE TABLE goods_return_line (
    id              BIGSERIAL PRIMARY KEY,
    return_id       UUID   NOT NULL REFERENCES goods_return(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    quantity        NUMERIC(18,3) NOT NULL CHECK (quantity > 0),
    return_price     NUMERIC(18,2) NOT NULL CHECK (return_price >= 0)
);

-- ============================================================================
-- MODULE: VISUALIZER (HQ)
-- ============================================================================

CREATE TABLE visualizer_room (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE visualizer_room_zone (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT       NOT NULL REFERENCES visualizer_room(id) ON DELETE CASCADE,
    zone_name       VARCHAR(50)  NOT NULL,           -- 'san', 'tuong'...
    polygon_json    JSONB        NOT NULL,           -- 4 điểm góc [{x,y}, ...]
    pixel_per_cm    NUMERIC(10,4) NOT NULL            -- tỷ lệ hiệu chỉnh để tính đúng kích thước gạch thật
);

CREATE TABLE visualizer_session (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT      NOT NULL REFERENCES visualizer_room(id),
    product_id      BIGINT      NOT NULL REFERENCES product(id),
    result_image_url VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- MODULE: COMMON — audit log (dùng chung cho AuditAspect toàn hệ thống)
-- ============================================================================

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    action          VARCHAR(30)  NOT NULL,     -- CREATE, UPDATE, CONFIRM, CANCEL...
    changed_by      BIGINT REFERENCES user_account(id),
    branch_id       BIGINT REFERENCES branch(id),
    before_data     JSONB,
    after_data      JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entity ON audit_log (entity_name, entity_id);
CREATE INDEX ix_audit_created ON audit_log (created_at);
COMMENT ON TABLE audit_log IS '[DENORMALIZED có chủ đích]: before_data/after_data lưu dạng JSONB snapshot toàn bộ record thay vì chuẩn hoá từng field-level change — chấp nhận vì đây là bảng ghi log thuần, không tham gia truy vấn nghiệp vụ, ưu tiên tốc độ ghi (write-heavy) hơn khả năng truy vấn từng field.';

-- ============================================================================
-- MODULE: ANALYTICS (Read Replica riêng — data mart, star-schema, ETL định kỳ)
-- Toàn bộ nhóm bảng dưới đây ĐƯỢC PHÉP denormalize hoàn toàn theo thiết kế —
-- đây là mục đích của data mart (tối ưu đọc cho báo cáo), không áp chuẩn 3NF.
-- ============================================================================

CREATE TABLE dim_date (
    date_key        INTEGER PRIMARY KEY,   -- YYYYMMDD
    full_date       DATE NOT NULL,
    year             SMALLINT NOT NULL,
    month             SMALLINT NOT NULL,
    day               SMALLINT NOT NULL
);

CREATE TABLE fact_sales (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      UUID   NOT NULL,
    date_key        INTEGER NOT NULL REFERENCES dim_date(date_key),
    product_id      BIGINT NOT NULL,
    branch_id       BIGINT NOT NULL,
    customer_id     BIGINT NOT NULL,
    quantity        NUMERIC(18,3) NOT NULL,
    unit_price      NUMERIC(18,2) NOT NULL,
    unit_cost       NUMERIC(18,4) NOT NULL,
    revenue         NUMERIC(18,2) NOT NULL,
    gross_profit    NUMERIC(18,2) NOT NULL
);
CREATE INDEX ix_fact_sales_date_branch ON fact_sales (date_key, branch_id);
CREATE INDEX ix_fact_sales_product ON fact_sales (product_id);

CREATE TABLE fact_stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    date_key        INTEGER NOT NULL REFERENCES dim_date(date_key),
    product_id      BIGINT  NOT NULL,
    branch_id       BIGINT  NOT NULL,
    movement_type   VARCHAR(20) NOT NULL,  -- INBOUND, OUTBOUND, TRANSFER, RETURN
    quantity        NUMERIC(18,3) NOT NULL
);
CREATE INDEX ix_fact_stock_date_branch ON fact_stock_movement (date_key, branch_id);

-- ============================================================================
-- GHI CHÚ CẤU HÌNH LOGICAL REPLICATION (Hub-and-Spoke) — chạy tại HQ, ví dụ
-- cho branch_id = 1; lặp lại tương tự cho từng chi nhánh với branch_id riêng
-- ============================================================================
-- CREATE PUBLICATION pub_branch_1_master
--     FOR TABLE product, category, product_attribute_value, price_list, customer
--     WHERE (branch_id = 1);  -- áp dụng cho bảng nào có cột branch_id; product/category không có branch_id nên không lọc
--
-- Tại DB chi nhánh 1:
-- CREATE SUBSCRIPTION sub_branch_1_from_hq
--     CONNECTION 'host=hq-db ...' PUBLICATION pub_branch_1_master;
--
-- Chiều ngược lại (branch -> HQ) tạo publication tương tự cho các bảng
-- sales_invoice, sales_invoice_line, inbound_receipt, customer_order,
-- goods_return, supplier_purchase_order tại mỗi branch DB, subscribe từ HQ.

