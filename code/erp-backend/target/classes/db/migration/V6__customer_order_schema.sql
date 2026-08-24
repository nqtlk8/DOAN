-- V6__customer_order_schema.sql

-- Customer Order
CREATE TABLE customer_order (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customer(id),
    order_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, CONFIRMED, INVOICED, CANCELLED
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    note TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

CREATE TABLE customer_order_line (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_order(id),
    product_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

-- Goods Return
CREATE TABLE goods_return (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customer(id),
    return_code VARCHAR(100) NOT NULL UNIQUE,
    invoice_id UUID, -- nullable, link to sales_invoice
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, CONFIRMED
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    reason TEXT,
    note TEXT,
    confirmed_at TIMESTAMP,
    confirmed_by UUID,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);

CREATE TABLE goods_return_line (
    id UUID PRIMARY KEY,
    return_id UUID NOT NULL REFERENCES goods_return(id),
    product_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP, created_by UUID,
    updated_at TIMESTAMP, updated_by UUID
);
