#!/bin/bash
set -euo pipefail

BRANCH_ID=${1:-}
if [ -z "$BRANCH_ID" ]; then
    echo "Usage: $0 <branch_id>"
    echo "Example: $0 tp1"
    exit 1
fi

BRANCH_ID=$(echo "$BRANCH_ID" | tr '[:upper:]' '[:lower:]')

# Configuration
HQ_DOCKER="code-hq-db-1"
BRANCH_DOCKER="code-branch-${BRANCH_ID}-db-1"

HQ_DB="erp_hq"
HQ_USER="erp_user"

BRANCH_DB="erp_branch_${BRANCH_ID}"
BRANCH_USER="erp_user"

REPL_USER="erp_repl"
REPL_PASSWORD="erp_repl_password"

echo "1. Validating database connections..."
if ! docker exec "$HQ_DOCKER" pg_isready -U "$HQ_USER" -d "$HQ_DB" > /dev/null 2>&1; then
    echo "Failed to connect to HQ DB container $HQ_DOCKER."
    exit 1
fi

if ! docker exec "$BRANCH_DOCKER" pg_isready -U "$BRANCH_USER" -d "$BRANCH_DB" > /dev/null 2>&1; then
    echo "Failed to connect to Branch DB container $BRANCH_DOCKER."
    exit 1
fi

echo "2. Checking Flyway schema version equality..."
HQ_VERSION=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1" | xargs)
BRANCH_VERSION=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1" | xargs)

if [ -z "$HQ_VERSION" ] || [ -z "$BRANCH_VERSION" ]; then
    echo "Could not fetch Flyway versions. Are migrations applied?"
    exit 1
fi

if [ "$HQ_VERSION" != "$BRANCH_VERSION" ]; then
    echo "Flyway schema version mismatch! HQ: $HQ_VERSION, Branch: $BRANCH_VERSION"
    exit 1
fi
echo "Schema version matched: $HQ_VERSION"

echo "3. Creating replication roles..."
# Create replication role on HQ
ROLE_EXISTS=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT 1 FROM pg_roles WHERE rolname = '$REPL_USER'" | xargs)
if [ "$ROLE_EXISTS" != "1" ]; then
  docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -c "CREATE ROLE $REPL_USER WITH REPLICATION LOGIN PASSWORD '$REPL_PASSWORD';"
fi
docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO $REPL_USER;"

# Create replication role on Branch
ROLE_EXISTS=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT 1 FROM pg_roles WHERE rolname = '$REPL_USER'" | xargs)
if [ "$ROLE_EXISTS" != "1" ]; then
  docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -c "CREATE ROLE $REPL_USER WITH REPLICATION LOGIN PASSWORD '$REPL_PASSWORD';"
fi
docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO $REPL_USER;"

echo "4. Setting up publications..."
HQ_PUB_NAME="pub_hq_to_${BRANCH_ID}"
BRANCH_PUB_NAME="pub_${BRANCH_ID}_to_hq"

MASTER_TABLES="branch, category, product, supplier, price_list, customer, role, permission, role_permission, user_account, user_branch_role, inventory_alert_config, dim_date"
TRANSACTION_TABLES="sales_invoice, sales_invoice_line, goods_return, goods_return_line, inbound_receipt, inbound_receipt_line, stock_movement, cost_layer"

# Create HQ Publication
PUB_EXISTS=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT 1 FROM pg_publication WHERE pubname = '$HQ_PUB_NAME'" | xargs)
if [ "$PUB_EXISTS" != "1" ]; then
  docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -c "CREATE PUBLICATION $HQ_PUB_NAME FOR TABLE $MASTER_TABLES;"
fi

# Create Branch Publication
PUB_EXISTS=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT 1 FROM pg_publication WHERE pubname = '$BRANCH_PUB_NAME'" | xargs)
if [ "$PUB_EXISTS" != "1" ]; then
  docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -c "CREATE PUBLICATION $BRANCH_PUB_NAME FOR TABLE $TRANSACTION_TABLES;"
fi

echo "5. Setting up subscriptions..."
HQ_SUB_NAME="sub_hq_from_${BRANCH_ID}"
BRANCH_SUB_NAME="sub_${BRANCH_ID}_from_hq"

HQ_SERVICE_HOST="hq-db"
BRANCH_SERVICE_HOST="branch-${BRANCH_ID}-db"

# Create Branch Subscription
SUB_EXISTS=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT 1 FROM pg_subscription WHERE subname = '$BRANCH_SUB_NAME'" | xargs)
if [ "$SUB_EXISTS" != "1" ]; then
  docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -c "CREATE SUBSCRIPTION $BRANCH_SUB_NAME CONNECTION 'host=$HQ_SERVICE_HOST port=5432 user=$REPL_USER password=$REPL_PASSWORD dbname=$HQ_DB' PUBLICATION $HQ_PUB_NAME WITH (copy_data = false);"
fi

# Create HQ Subscription
SUB_EXISTS=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT 1 FROM pg_subscription WHERE subname = '$HQ_SUB_NAME'" | xargs)
if [ "$SUB_EXISTS" != "1" ]; then
  docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -c "CREATE SUBSCRIPTION $HQ_SUB_NAME CONNECTION 'host=$BRANCH_SERVICE_HOST port=5432 user=$REPL_USER password=$REPL_PASSWORD dbname=$BRANCH_DB' PUBLICATION $BRANCH_PUB_NAME WITH (copy_data = false);"
fi

echo "6. Waiting for initial sync..."
sleep 5

echo "7. Verifying replication state..."
# Check HQ replication
HQ_REPL_STATE=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT state FROM pg_stat_replication WHERE application_name = '$BRANCH_SUB_NAME'" | xargs)
if [ "$HQ_REPL_STATE" != "streaming" ]; then
    echo "WARNING: HQ to Branch replication is not streaming. State: $HQ_REPL_STATE"
fi

HQ_SUB_STATE=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT srsubstate FROM pg_subscription_rel sr JOIN pg_subscription s ON s.oid = sr.srsubid WHERE s.subname = '$HQ_SUB_NAME' LIMIT 1" | xargs)
if [[ "$HQ_SUB_STATE" != "r" && -n "$HQ_SUB_STATE" ]]; then
     echo "WARNING: HQ subscription state is not 'r' (ready). State: $HQ_SUB_STATE"
fi

# Check Branch replication
BRANCH_REPL_STATE=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT state FROM pg_stat_replication WHERE application_name = '$HQ_SUB_NAME'" | xargs)
if [ "$BRANCH_REPL_STATE" != "streaming" ]; then
    echo "WARNING: Branch to HQ replication is not streaming. State: $BRANCH_REPL_STATE"
fi

BRANCH_SUB_STATE=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT srsubstate FROM pg_subscription_rel sr JOIN pg_subscription s ON s.oid = sr.srsubid WHERE s.subname = '$BRANCH_SUB_NAME' LIMIT 1" | xargs)
if [[ "$BRANCH_SUB_STATE" != "r" && -n "$BRANCH_SUB_STATE" ]]; then
     echo "WARNING: Branch subscription state is not 'r' (ready). State: $BRANCH_SUB_STATE"
fi

echo "Replication setup completed successfully!"
