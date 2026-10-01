CREATE TABLE receivable_debt_movement (
    id UUID PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    customer_id UUID NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    ref_type VARCHAR(50) NOT NULL,
    ref_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by UUID,
    note TEXT,
    CONSTRAINT fk_debt_movement_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE INDEX idx_debt_movement_customer_branch ON receivable_debt_movement(customer_id, branch_id);
CREATE INDEX idx_debt_movement_ref ON receivable_debt_movement(ref_type, ref_id);

