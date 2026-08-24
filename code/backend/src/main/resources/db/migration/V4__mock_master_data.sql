-- Mock Master Data

-- Insert 3 customers
INSERT INTO customer (id, name, email, phone, address, is_deleted, version) VALUES
('11111111-1111-1111-1111-111111111111', 'Acme Corp', 'contact@acme.com', '+123456789', '123 Acme St', FALSE, 0),
('22222222-2222-2222-2222-222222222222', 'Globex Inc', 'info@globex.com', '+987654321', '456 Globex Ave', FALSE, 0),
('33333333-3333-3333-3333-333333333333', 'Soylent Corp', 'hello@soylent.com', '+1122334455', '789 Soylent Blvd', FALSE, 0);

-- Insert 5 products
INSERT INTO product (id, code, name, price, is_deleted, version) VALUES
('44444444-4444-4444-4444-444444444444', 'PROD-1001', 'Ergonomic Chair', 150.0000, FALSE, 0),
('55555555-5555-5555-5555-555555555555', 'PROD-1002', 'Standing Desk', 350.0000, FALSE, 0),
('66666666-6666-6666-6666-666666666666', 'PROD-1003', 'Monitor Arm', 85.0000, FALSE, 0),
('77777777-7777-7777-7777-777777777777', 'PROD-1004', 'Mechanical Keyboard', 120.0000, FALSE, 0),
('88888888-8888-8888-8888-888888888888', 'PROD-1005', 'Wireless Mouse', 60.0000, FALSE, 0);

-- Insert initial stock for products
INSERT INTO inventory_transaction (id, product_id, quantity, transaction_type) VALUES
(gen_random_uuid(), '44444444-4444-4444-4444-444444444444', 45, 'IN'),
(gen_random_uuid(), '55555555-5555-5555-5555-555555555555', 20, 'IN'),
(gen_random_uuid(), '66666666-6666-6666-6666-666666666666', 100, 'IN'),
(gen_random_uuid(), '77777777-7777-7777-7777-777777777777', 50, 'IN'),
(gen_random_uuid(), '88888888-8888-8888-8888-888888888888', 80, 'IN');
