#!/bin/bash
PRODUCT_CODE="PROD-TP3-$(date +%s)"
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "INSERT INTO category (code, name, is_active, created_at, updated_at) VALUES ('CAT-$PRODUCT_CODE', 'Cat TP3', true, now(), now());"
CAT_ID=$(docker exec code-hq-db-1 psql -U erp_user -d erp_hq -t -c "SELECT id FROM category WHERE code = 'CAT-$PRODUCT_CODE'" | xargs)
docker exec code-hq-db-1 psql -U erp_user -d erp_hq -c "INSERT INTO product (code, name, base_unit, is_active, category_id, created_at, updated_at) VALUES ('$PRODUCT_CODE', 'Test Product TP3', 'PCS', true, $CAT_ID, now(), now());"
sleep 2
docker exec code-branch-tp3-db-1 psql -U erp_user -d erp_branch_tp3 -c "SELECT code, name, category_id FROM product WHERE code = '$PRODUCT_CODE';"
