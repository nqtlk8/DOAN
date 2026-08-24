CREATE TABLE stock_on_hand (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    avg_cost NUMERIC(19,4) DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID,
    CONSTRAINT uk_stock_product_branch UNIQUE (product_id, branch_id)
);

CREATE TABLE inbound_receipt (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    receipt_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    note TEXT,
    confirmed_at TIMESTAMP,
    confirmed_by UUID,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE inbound_receipt_line (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES inbound_receipt(id),
    product_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_cost NUMERIC(19,4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE outbound_receipt (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    receipt_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    note TEXT,
    reason VARCHAR(255),
    confirmed_at TIMESTAMP,
    confirmed_by UUID,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE outbound_receipt_line (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES outbound_receipt(id),
    product_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);
