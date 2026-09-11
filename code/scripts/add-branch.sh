#!/bin/bash
set -euo pipefail

BRANCH_NAME=$1
if [ -z "$BRANCH_NAME" ]; then
  echo "Usage: $0 <branch_name> (e.g. tp4)"
  exit 1
fi

BRANCH_ID=$(echo "$BRANCH_NAME" | tr '[:lower:]' '[:upper:]')
LOWER_BRANCH=$(echo "$BRANCH_NAME" | tr '[:upper:]' '[:lower:]')

echo "=== PROVISIONING NEW BRANCH: $BRANCH_ID ==="

# 1. We assume docker-compose.yml has been updated with branch-$LOWER_BRANCH-db and branch-$LOWER_BRANCH-app
# Let's ensure the DB is up
docker compose up -d branch-${LOWER_BRANCH}-db
sleep 5

# 2. Run Flyway schema initialization using Maven
echo "Running Flyway migration for Branch $BRANCH_ID..."
mvn flyway:migrate "-Dflyway.url=jdbc:postgresql://localhost:5435/erp_branch_${LOWER_BRANCH}" "-Dflyway.user=erp_user" "-Dflyway.password=erp_password" -f ../erp-platform/services/erp-backend/pom.xml

# 3. Check schema version parity
echo "Verifying schema parity with HQ..."
./check-schema-version.sh $LOWER_BRANCH

# 4. Truncate Master Data (to prepare for replication initial sync)
echo "Truncating dummy seed data in Branch $BRANCH_ID to prepare for replication sync..."
docker exec code-branch-${LOWER_BRANCH}-db-1 psql -U erp_user -d erp_branch_${LOWER_BRANCH} -c "TRUNCATE TABLE branch, category, customer, dim_date, inventory_alert_config, permission, price_list, product, role, role_permission, supplier, user_account, user_branch_role CASCADE;"

# 5. Setup logical replication with copy_data=true
echo "Setting up Logical Replication with copy_data=true..."
./setup-replication.sh $LOWER_BRANCH true

# 6. Start the App container
echo "Starting Branch App Container..."
docker compose up -d branch-${LOWER_BRANCH}-app

echo "=== BRANCH $BRANCH_ID PROVISIONED SUCCESSFULLY ==="
