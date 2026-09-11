#!/bin/bash
set -euo pipefail

echo "=== REPLICATION E2E TEST & OWNERSHIP INVARIANT ==="

HQ_DOCKER="code-hq-db-1"
BRANCH_DOCKER="code-branch-tp1-db-1"

HQ_USER="erp_user"
HQ_DB="erp_hq"
BRANCH_USER="erp_user"
BRANCH_DB="erp_branch_tp1"

echo "1. HQ create product -> Branch receives product"
# HQ insert category then product
PRODUCT_CODE="PROD-$(date +%s)"
docker exec $HQ_DOCKER psql -U $HQ_USER -d $HQ_DB -c "INSERT INTO category (code, name, is_active, created_at, updated_at) VALUES ('CAT-$PRODUCT_CODE', 'Cat', true, now(), now());"
CAT_ID=$(docker exec $HQ_DOCKER psql -U $HQ_USER -d $HQ_DB -t -c "SELECT id FROM category WHERE code = 'CAT-$PRODUCT_CODE'" | xargs)

docker exec $HQ_DOCKER psql -U $HQ_USER -d $HQ_DB -c "INSERT INTO product (code, name, base_unit, is_active, category_id, created_at, updated_at) VALUES ('$PRODUCT_CODE', 'Test Product', 'PCS', true, $CAT_ID, now(), now());"
sleep 2

# Branch verify
BRANCH_HAS_PRODUCT=$(docker exec $BRANCH_DOCKER psql -U $BRANCH_USER -d $BRANCH_DB -t -c "SELECT count(*) FROM product WHERE code = '$PRODUCT_CODE'" | xargs)
if [ "$BRANCH_HAS_PRODUCT" != "1" ]; then
    echo "FAIL: Branch did not receive product from HQ"
    exit 1
fi
echo "PASS: Branch received product from HQ"

echo "2. Branch create sales invoice -> HQ receives invoice"
INVOICE_CODE="INV-$(date +%s)"
# We need a customer to create invoice. Let's insert a dummy customer in HQ first.
docker exec $HQ_DOCKER psql -U $HQ_USER -d $HQ_DB -c "INSERT INTO customer (id, customer_code, name, is_deleted, version, created_at, updated_at) VALUES (gen_random_uuid(), 'CUST-$PRODUCT_CODE', 'Test Customer', false, 0, now(), now());"
sleep 2
CUST_ID=$(docker exec $HQ_DOCKER psql -U $HQ_USER -d $HQ_DB -t -c "SELECT id FROM customer WHERE customer_code = 'CUST-$PRODUCT_CODE'" | xargs)

# Sales invoice needs an id (UUID). PostgreSQL gen_random_uuid()
docker exec $BRANCH_DOCKER psql -U $BRANCH_USER -d $BRANCH_DB -c "INSERT INTO sales_invoice (id, branch_id, customer_id, invoice_code, status, total_amount, previous_debt, remaining_debt, payment_method, is_deleted, version, created_at, updated_at) VALUES (gen_random_uuid(), 1, '$CUST_ID', '$INVOICE_CODE', 'DRAFT', 0, 0, 0, 'CASH', false, 0, now(), now());"
sleep 2

# HQ verify
HQ_HAS_INVOICE=$(docker exec $HQ_DOCKER psql -U $HQ_USER -d $HQ_DB -t -c "SELECT count(*) FROM sales_invoice WHERE invoice_code = '$INVOICE_CODE'" | xargs)
if [ "$HQ_HAS_INVOICE" != "1" ]; then
    echo "FAIL: HQ did not receive sales invoice from Branch"
    exit 1
fi
echo "PASS: HQ received sales invoice from Branch"


echo "=== E2E TESTS COMPLETED ==="
