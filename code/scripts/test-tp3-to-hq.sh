#!/bin/bash
INVOICE_CODE="INV-TP3-$(date +%s)"
# Insert dummy customer to HQ
PRODUCT_CODE="PROD-$(date +%s)"
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "INSERT INTO customer (id, customer_code, name, is_deleted, version, created_at, updated_at) VALUES (gen_random_uuid(), 'CUST-$PRODUCT_CODE', 'Test Customer', false, 0, now(), now());"
sleep 2
CUST_ID=$(docker exec code-hq-db-1 psql -U erp_user -d erp_hq -t -c "SELECT id FROM customer WHERE customer_code = 'CUST-$PRODUCT_CODE'" | xargs)

# Insert invoice in Branch TP3 (Wait, is TP3 branch_id = 3?)
# What is the branch_id of TP3?
# The `setup-replication.sh` script does not create the branch record.
# I will use branch_id = 1 for now, or just let it fail if foreign key constraint exists.
# Actually, let's insert branch 3 in HQ and wait for it to replicate.
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "INSERT INTO branch (id, code, name, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES (3, 'TP3', 'Branch TP3', true, now(), now()) ON CONFLICT DO NOTHING;"
sleep 2

docker exec code-branch-tp3-db-1 psql -U erp_user -d erp_branch_tp3 -c "INSERT INTO sales_invoice (id, branch_id, customer_id, invoice_code, status, total_amount, previous_debt, remaining_debt, payment_method, is_deleted, version, created_at, updated_at) VALUES (gen_random_uuid(), 3, '$CUST_ID', '$INVOICE_CODE', 'DRAFT', 0, 0, 0, 'CASH', false, 0, now(), now());"
sleep 2

docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "SELECT invoice_code, status FROM sales_invoice WHERE invoice_code = '$INVOICE_CODE';"
