CREATE TABLE customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    metadata JSONB,
    is_deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE distributor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    contact_info TEXT,
    metadata JSONB,
    is_deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(19,4) NOT NULL,
    metadata JSONB,
    is_deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE quotation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customer(id),
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    metadata JSONB,
    is_deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE quotation_line_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quotation_id UUID NOT NULL REFERENCES quotation(id),
    product_id UUID NOT NULL REFERENCES product(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    total_price NUMERIC(19,4) NOT NULL
);

CREATE TABLE sales_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customer(id),
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    metadata JSONB,
    is_deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE sales_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_order_id UUID NOT NULL REFERENCES sales_order(id),
    product_id UUID NOT NULL REFERENCES product(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    total_price NUMERIC(19,4) NOT NULL
);

CREATE TABLE purchase_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    distributor_id UUID REFERENCES distributor(id),
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    metadata JSONB,
    is_deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMP,
    updated_by UUID
);

CREATE TABLE purchase_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id UUID NOT NULL REFERENCES purchase_order(id),
    product_id UUID NOT NULL REFERENCES product(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    total_price NUMERIC(19,4) NOT NULL
);

CREATE TABLE inventory_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id),
    quantity INTEGER NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
