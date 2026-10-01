#!/bin/bash
# ============================================================================
# test-replication-e2e.sh [chi nhánh]     vd: scripts/test-replication-e2e.sh tp1
#
# Kiểm thử nhanh trên hệ thống docker-compose đang chạy (sau bootstrap.sh):
#   1. HQ tạo nhà cung cấp            -> chi nhánh nhận được (HQ -> Branch)
#   2. Chi nhánh tạo stock_movement   -> HQ nhận được        (Branch -> HQ)
#   3. Role ứng dụng ở chi nhánh KHÔNG ghi được bảng master (supplier)
#   4. Role ứng dụng ở chi nhánh vẫn ghi được bảng giao dịch
# Dữ liệu test được xoá ở cuối (việc xoá cũng đi qua replication).
# ============================================================================
set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

B=$(branch_key "${1:-tp1}")
CODE=$(branch_code "$B")
BR_SERVICE=$(branch_service "$B")
BR_DB=$(branch_db "$B")
APP_DB_PASSWORD="${APP_DB_PASSWORD:-erp_app_password}"
TAG="E2E-$(date +%s)"
FAILED=0

pass() { printf '\033[1;32mPASS\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mFAIL\033[0m %s\n' "$*"; FAILED=1; }

# await <service> <db> <sql> <giá trị mong đợi>: chờ tối đa 20s
await() {
  local i v
  for i in $(seq 1 20); do
    v=$(sql_value "$1" "$2" "$3")
    [ "$v" = "$4" ] && return 0
    sleep 1
  done
  return 1
}

# app_psql <sql>: chạy bằng role ứng dụng ở chi nhánh (như Spring Boot), trả về exit code của psql.
app_psql() {
  MSYS_NO_PATHCONV=1 $COMPOSE exec -T -e PGPASSWORD="$APP_DB_PASSWORD" "$BR_SERVICE" \
    psql -h 127.0.0.1 -U "$APP_DB_USER" -d "$BR_DB" -v ON_ERROR_STOP=1 -X -q -c "$1" >/dev/null 2>&1
}

BRANCH_ID=$(hq_value "SELECT id FROM branch WHERE upper(code) = '$CODE'")
[ -n "$BRANCH_ID" ] || die "HQ chưa có chi nhánh $CODE"

log "1. HQ -> $CODE: tạo supplier $TAG ở HQ"
hq_sql "INSERT INTO supplier (id, code, name, is_active, created_at, updated_at) VALUES (gen_random_uuid(), '$TAG', 'NCC kiểm thử $TAG', true, now(), now());"
if await "$BR_SERVICE" "$BR_DB" "SELECT count(*) FROM supplier WHERE code = '$TAG'" 1; then pass "$CODE nhận supplier từ HQ"; else fail "$CODE KHÔNG nhận supplier"; fi

log "2. $CODE -> HQ: tạo stock_movement ở $CODE"
MOVE_ID=$(sql_value "$BR_SERVICE" "$BR_DB" "SELECT gen_random_uuid()")
PRODUCT_ID=$(sql_value "$BR_SERVICE" "$BR_DB" "SELECT min(id) FROM product")
if [ -z "$PRODUCT_ID" ]; then
  warn "Chưa có sản phẩm nào (chạy scripts/seed-demo.py trước) — bỏ qua bước 2 và 4"
else
  psql_on "$BR_SERVICE" "$BR_DB" -c "INSERT INTO stock_movement (id, product_id, branch_id, movement_type, quantity, ref_type, ref_id) VALUES ('$MOVE_ID', $PRODUCT_ID, $BRANCH_ID, 'ADJUSTMENT', 0, 'e2e_test', '$TAG');"
  if await "$HQ_SERVICE" "$HQ_DB" "SELECT count(*) FROM stock_movement WHERE id = '$MOVE_ID'" 1; then pass "HQ nhận stock_movement từ $CODE"; else fail "HQ KHÔNG nhận stock_movement"; fi
fi

log "3. Role $APP_DB_USER ở $CODE không được ghi master data"
if app_psql "UPDATE supplier SET name = name WHERE code = '$TAG'"; then fail "$APP_DB_USER GHI ĐƯỢC bảng supplier ở $CODE"; else pass "$APP_DB_USER bị chặn ghi supplier"; fi

if [ -n "$PRODUCT_ID" ]; then
  log "4. Role $APP_DB_USER ở $CODE vẫn ghi được bảng giao dịch"
  if app_psql "UPDATE stock_movement SET quantity = 0 WHERE id = '$MOVE_ID'"; then pass "$APP_DB_USER ghi được stock_movement"; else fail "$APP_DB_USER KHÔNG ghi được stock_movement"; fi
  psql_on "$BR_SERVICE" "$BR_DB" -c "DELETE FROM stock_movement WHERE id = '$MOVE_ID';"
fi

log "Dọn dữ liệu test"
hq_sql "DELETE FROM supplier WHERE code = '$TAG';"

[ "$FAILED" = 0 ] && log "TẤT CẢ PASS" || die "Có bước FAIL"
