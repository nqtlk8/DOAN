-- V4__sales_invoice_schema.sql
-- Triển khai module Order (SalesInvoice) và Idempotency Record

-- Customer
CREATE TABLE customer (
    id UUID PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(200),
    address TEXT,
    tax_code VARCHAR(20),
    customer_type VARCHAR(50) DEFAULT 'RETAIL',  -- RETAIL, WHOLESALE, CONSTRUCTION
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

-- Giá riêng theo khách hàng x sản phẩm
CREATE TABLE customer_product_price (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    product_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID,
    CONSTRAINT uk_cust_prod_branch UNIQUE (customer_id, product_id, branch_id)
);

-- Hoá đơn bán hàng
CREATE TABLE sales_invoice (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customer(id),
    invoice_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    previous_debt NUMERIC(19,4) NOT NULL DEFAULT 0,   -- snapshot công nợ cũ
    remaining_debt NUMERIC(19,4) NOT NULL DEFAULT 0,   -- snapshot công nợ mới
    payment_method VARCHAR(50),                         -- CASH, CREDIT, MIXED
    note TEXT,
    confirmed_at TIMESTAMP,
    confirmed_by UUID,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

CREATE TABLE sales_invoice_line (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES sales_invoice(id),
    product_id UUID NOT NULL,
    product_name VARCHAR(255),        -- snapshot tên SP lúc tạo hoá đơn
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL, -- giá bán thực tế (có thể từ CustomerProductPrice hoặc PriceList)
    unit_cost NUMERIC(19,4),           -- snapshot giá vốn lúc confirm
    line_total NUMERIC(19,4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

-- Công nợ phải thu (Receivable Debt)
CREATE TABLE receivable_debt (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    branch_id UUID NOT NULL,
    total_debt NUMERIC(19,4) NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID,
    CONSTRAINT uk_debt_customer_branch UNIQUE (customer_id, branch_id)
);

-- Idempotency Record (common)
CREATE TABLE idempotency_record (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_hash VARCHAR(64),
    response_snapshot TEXT,        -- JSON string
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
