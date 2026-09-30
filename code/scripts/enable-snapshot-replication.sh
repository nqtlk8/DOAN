#!/bin/bash
set -euo pipefail

BRANCH_NAME=${1:-}
SKIP_CONFIRM=false

if [ "$#" -ge 2 ] && [ "$2" == "--yes" ]; then
    SKIP_CONFIRM=true
fi

if [ -z "$BRANCH_NAME" ]; then
  echo "Usage: $0 <branch_name> [--yes] (e.g. tp1, tp2, tp3)"
  exit 1
fi

BRANCH_CODE=$(echo "$BRANCH_NAME" | tr '[:lower:]' '[:upper:]')
BRANCH_ID=$(echo "$BRANCH_NAME" | tr '[:upper:]' '[:lower:]')

# Configuration
HQ_DOCKER="code-hq-db-1"
BRANCH_DOCKER="code-branch-${BRANCH_ID}-db-1"

HQ_DB="erp_hq"
HQ_USER="erp_user"

BRANCH_DB="erp_branch_${BRANCH_ID}"
BRANCH_USER="erp_user"

echo "1. Validating database connections..."
if ! docker exec "$HQ_DOCKER" pg_isready -U "$HQ_USER" -d "$HQ_DB" > /dev/null 2>&1; then
    echo "Failed to connect to HQ DB container $HQ_DOCKER."
    exit 1
fi

if ! docker exec "$BRANCH_DOCKER" pg_isready -U "$BRANCH_USER" -d "$BRANCH_DB" > /dev/null 2>&1; then
    echo "Failed to connect to Branch DB container $BRANCH_DOCKER."
    exit 1
fi

echo "2. Getting Branch Numeric ID from DB..."
BRANCH_NUM_ID=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT id FROM branch WHERE upper(code) = '$BRANCH_CODE'" | xargs)
if [ -z "$BRANCH_NUM_ID" ]; then
    echo "Error: Branch with code $BRANCH_CODE not found in the branch database."
    exit 1
fi
echo "Found Branch ID: $BRANCH_NUM_ID"

echo "3. Checking Replica Identity (V21 must be applied)..."
check_replica_identity() {
    local docker_container=$1
    local db_user=$2
    local db_name=$3
    
    local soh_replident=$(docker exec "$docker_container" psql -U "$db_user" -d "$db_name" -t -c "SELECT relreplident FROM pg_class WHERE relname = 'stock_on_hand'" | xargs)
    local rd_replident=$(docker exec "$docker_container" psql -U "$db_user" -d "$db_name" -t -c "SELECT relreplident FROM pg_class WHERE relname = 'receivable_debt'" | xargs)
    
    if [ "$soh_replident" != "i" ] || [ "$rd_replident" != "i" ]; then
        echo "Error: Replica identity is not set to 'i' (index) on $docker_container."
        echo "Please ensure V21 migration has been applied."
        exit 1
    fi
}

check_replica_identity "$HQ_DOCKER" "$HQ_USER" "$HQ_DB"
check_replica_identity "$BRANCH_DOCKER" "$BRANCH_USER" "$BRANCH_DB"

echo "4. Checking and updating Branch Publication..."
BRANCH_PUB_NAME="pub_${BRANCH_ID}_to_hq"

# Check if publication exists
PUB_EXISTS=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT 1 FROM pg_publication WHERE pubname = '$BRANCH_PUB_NAME'" | xargs)
if [ "$PUB_EXISTS" != "1" ]; then
    echo "Error: Publication $BRANCH_PUB_NAME does not exist. Please run setup-replication.sh first."
    exit 1
fi

SOH_IN_PUB=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT 1 FROM pg_publication_tables WHERE pubname = '$BRANCH_PUB_NAME' AND tablename = 'stock_on_hand'" | xargs)
if [ "$SOH_IN_PUB" != "1" ]; then
    echo "Adding snapshot tables with row filters to $BRANCH_PUB_NAME..."
    docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -c "ALTER PUBLICATION $BRANCH_PUB_NAME ADD TABLE stock_on_hand WHERE (branch_id = $BRANCH_NUM_ID), receivable_debt WHERE (branch_id = $BRANCH_NUM_ID);"
else
    echo "Snapshot tables already in publication."
fi

echo "5. Checking HQ Subscription..."
HQ_SUB_NAME="sub_hq_from_${BRANCH_ID}"
SUB_EXISTS=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT 1 FROM pg_subscription WHERE subname = '$HQ_SUB_NAME'" | xargs)
if [ "$SUB_EXISTS" != "1" ]; then
    echo "Error: Subscription $HQ_SUB_NAME does not exist on HQ. Please run setup-replication.sh first."
    exit 1
fi

SOH_IN_SUB=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT 1 FROM pg_subscription_rel sr JOIN pg_class c ON c.oid = sr.srrelid JOIN pg_subscription s ON s.oid = sr.srsubid WHERE s.subname = '$HQ_SUB_NAME' AND c.relname = 'stock_on_hand'" | xargs)

if [ "$SOH_IN_SUB" != "1" ]; then
    echo "Snapshot tables not found in HQ subscription."
    
    if [ "$SKIP_CONFIRM" = false ]; then
        echo "WARNING: To initialize data safely, we must DELETE existing snapshot rows for branch_id=$BRANCH_NUM_ID at HQ before copying."
        read -p "Proceed? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Operation cancelled."
            exit 1
        fi
    fi
    
    echo "Deleting existing branch data at HQ..."
    docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -c "DELETE FROM stock_on_hand WHERE branch_id = $BRANCH_NUM_ID;"
    docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -c "DELETE FROM receivable_debt WHERE branch_id = $BRANCH_NUM_ID;"
    
    echo "Refreshing publication with copy_data=true..."
    docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -c "ALTER SUBSCRIPTION $HQ_SUB_NAME REFRESH PUBLICATION WITH (copy_data = true);"
else
    echo "Snapshot tables already in subscription."
fi

echo "6. Waiting and verifying sync state..."
sleep 5

SOH_STATE=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT srsubstate FROM pg_subscription_rel sr JOIN pg_class c ON c.oid = sr.srrelid JOIN pg_subscription s ON s.oid = sr.srsubid WHERE s.subname = '$HQ_SUB_NAME' AND c.relname = 'stock_on_hand' LIMIT 1" | xargs)
RD_STATE=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT srsubstate FROM pg_subscription_rel sr JOIN pg_class c ON c.oid = sr.srrelid JOIN pg_subscription s ON s.oid = sr.srsubid WHERE s.subname = '$HQ_SUB_NAME' AND c.relname = 'receivable_debt' LIMIT 1" | xargs)

if [ "$SOH_STATE" != "r" ] || [ "$RD_STATE" != "r" ]; then
    echo "WARNING: Subscription state is not 'r' (ready). stock_on_hand: $SOH_STATE, receivable_debt: $RD_STATE"
fi

# Compare counts
BRANCH_SOH_COUNT=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT COUNT(*) FROM stock_on_hand WHERE branch_id = $BRANCH_NUM_ID" | xargs)
HQ_SOH_COUNT=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT COUNT(*) FROM stock_on_hand WHERE branch_id = $BRANCH_NUM_ID" | xargs)

BRANCH_RD_COUNT=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT COUNT(*) FROM receivable_debt WHERE branch_id = $BRANCH_NUM_ID" | xargs)
HQ_RD_COUNT=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT COUNT(*) FROM receivable_debt WHERE branch_id = $BRANCH_NUM_ID" | xargs)

echo "--- Replication Verification (Branch ID: $BRANCH_NUM_ID) ---"
echo "stock_on_hand:   Branch = $BRANCH_SOH_COUNT | HQ = $HQ_SOH_COUNT"
echo "receivable_debt: Branch = $BRANCH_RD_COUNT | HQ = $HQ_RD_COUNT"

echo "Snapshot replication setup successfully checked."
