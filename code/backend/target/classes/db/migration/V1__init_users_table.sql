CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ROLE_ADMIN', 'ROLE_SALES')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy cho Role Admin: Cho phép xem mọi dòng
CREATE POLICY admin_select_all ON users
    FOR SELECT
    USING (current_setting('app.current_role', true) = 'ROLE_ADMIN');

-- Policy cho Role Sales: Chỉ cho phép xem dòng có id trùng với id của người đang truy vấn
CREATE POLICY sales_select_own ON users
    FOR SELECT
    USING (
        current_setting('app.current_role', true) = 'ROLE_SALES' 
        AND id = current_setting('app.current_user_id', true)::uuid
    );

INSERT INTO users (id, username, password_hash, role) VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin', '$2a$10$OVFJ75ncSNjdwyNcYMeWke01RP8f9nt1N90EqI5H8xPMpxQDL.bke', 'ROLE_ADMIN'),
    ('22222222-2222-2222-2222-222222222222', 'sales', '$2a$10$dmNQPSYNTQNjb6fwdzDfTe5NdwEg5dYorIbyeEjc1I9WeoGahcy/y', 'ROLE_SALES');
