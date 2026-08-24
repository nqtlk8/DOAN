-- Nhà cung cấp
CREATE TABLE supplier (
    id UUID PRIMARY KEY,
    supplier_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(200),
    address TEXT,
    tax_code VARCHAR(20),
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

-- Công nợ phải trả (Payable Debt)
CREATE TABLE payable_debt (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    branch_id UUID NOT NULL,
    total_debt NUMERIC(19,4) NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID,
    CONSTRAINT uk_debt_supplier_branch UNIQUE (supplier_id, branch_id)
);

-- Đơn đặt hàng nhà cung cấp (Purchase Order)
CREATE TABLE supplier_purchase_order (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    po_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    note TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

CREATE TABLE supplier_purchase_order_line (
    id UUID PRIMARY KEY,
    po_id UUID NOT NULL REFERENCES supplier_purchase_order(id),
    product_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_cost NUMERIC(19,4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

-- Chuyển kho (Stock Transfer)
CREATE TABLE stock_transfer (
    id UUID PRIMARY KEY,
    from_branch_id UUID NOT NULL,
    to_branch_id UUID NOT NULL,
    transfer_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    outbound_receipt_id UUID, -- Liên kết phiếu xuất kho ở nhánh đi
    inbound_receipt_id UUID,  -- Liên kết phiếu nhập kho ở nhánh đến
    note TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

CREATE TABLE stock_transfer_line (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES stock_transfer(id),
    product_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

-- Bổ sung mapping InboundReceipt <-> PurchaseOrder
ALTER TABLE inbound_receipt ADD COLUMN purchase_order_id UUID REFERENCES supplier_purchase_order(id);
