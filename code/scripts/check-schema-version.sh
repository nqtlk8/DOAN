#!/bin/bash
# ============================================================================
# check-schema-version.sh <chi nhánh>   vd: scripts/check-schema-version.sh tp1
#
# So sánh DANH SÁCH version Flyway (migration V__ trong db/migration) giữa HQ và chi nhánh.
# Hai bên phải giống hệt nhau trước khi thiết lập/chạy replication (DDL không được replicate).
# Migration lặp lại (R__) khác nhau giữa HQ và chi nhánh nên không được so.
# ============================================================================
set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

B=$(branch_key "${1:-}")
HQ_V=$(flyway_versions "$HQ_SERVICE" "$HQ_DB")
BR_V=$(flyway_versions "$(branch_service "$B")" "$(branch_db "$B")")

echo "------------------------------------------------"
echo "HQ:              [$HQ_V]"
echo "$(branch_code "$B"):             [$BR_V]"
echo "------------------------------------------------"
if [ -n "$HQ_V" ] && [ "$HQ_V" = "$BR_V" ]; then
  echo "PASS: version Flyway khớp."
else
  echo "FAIL: version Flyway lệch hoặc chưa migrate."
  exit 1
fi
