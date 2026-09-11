#!/bin/bash
# check-schema-version.sh
# Compare Flyway schema versions between HQ and a Branch

BRANCH_NAME=$1
if [ -z "$BRANCH_NAME" ]; then
  echo "Usage: $0 <branch_name> (e.g. tp1, tp2, tp3)"
  exit 1
fi

HQ_DOCKER="code-hq-db-1"
HQ_DB="erp_hq"
HQ_USER="erp_user"

BRANCH_DOCKER="code-branch-${BRANCH_NAME}-db-1"
BRANCH_DB="erp_branch_${BRANCH_NAME}"
BRANCH_USER="erp_user"

echo "Checking HQ Schema Version..."
HQ_VERSION=$(docker exec "$HQ_DOCKER" psql -U "$HQ_USER" -d "$HQ_DB" -t -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;" | xargs)

echo "Checking Branch ($BRANCH_NAME) Schema Version..."
BRANCH_VERSION=$(docker exec "$BRANCH_DOCKER" psql -U "$BRANCH_USER" -d "$BRANCH_DB" -t -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;" | xargs)

echo "------------------------------------------------"
echo "HQ Version:     $HQ_VERSION"
echo "Branch Version: $BRANCH_VERSION"
echo "------------------------------------------------"

if [ "$HQ_VERSION" == "$BRANCH_VERSION" ]; then
    echo "PASS: Schema versions match."
    exit 0
else
    echo "FAIL: Schema versions mismatch!"
    exit 1
fi
