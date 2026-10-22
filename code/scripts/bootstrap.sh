#!/bin/bash
# ============================================================================
# bootstrap.sh [chi nhánh bổ sung...] [--seed] [--no-build]
#
#   scripts/bootstrap.sh                 # HQ + TP1
#   scripts/bootstrap.sh tp2 --seed      # HQ + TP1 + TP2, rồi nạp dữ liệu demo
#   --no-build                           # dùng image đã build, không build lại
#
# Dựng toàn bộ hệ thống theo đúng thứ tự:
#   1. docker compose up (build image, tạo DB; role DB do docker/postgres/init tạo khi volume mới)
#   2. chờ Flyway của từng app chạy xong (HQ: schema + dữ liệu hệ thống; chi nhánh: schema + quyền)
#   3. setup-replication.sh cho TP1 và từng chi nhánh được liệt kê
#   4. (--seed) scripts/seed-demo.py — nạp master data ở HQ + giao dịch ở chi nhánh qua API
#
# Làm lại từ đầu (XOÁ dữ liệu): docker compose --profile tp2 down -v  rồi chạy lại script này.
# ============================================================================
set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

BRANCHES=(tp1)
SEED=false
BUILD_ARG="--build"
for arg in "$@"; do
  case "$arg" in
    --seed) SEED=true ;;
    --no-build) BUILD_ARG="" ;;
    *) b=$(branch_key "$arg"); [ "$b" = "tp1" ] || BRANCHES+=("$b") ;;
  esac
done

PROFILE_ARGS=""
for b in "${BRANCHES[@]}"; do PROFILE_ARGS="$PROFILE_ARGS $(branch_profile_args "$b")"; done

log "1. docker compose up (${BRANCHES[*]})"
# shellcheck disable=SC2086
$COMPOSE $PROFILE_ARGS up -d $BUILD_ARG

# wait_flyway <service> <db> <nhãn>: chờ tới khi app đã migrate (có version và không còn migration lỗi).
wait_flyway() {
  local service="$1" db="$2" label="$3" waited=0 versions=""
  until db_ready "$service" "$db"; do sleep 2; done
  while :; do
    versions=$(flyway_versions "$service" "$db")
    if [ -n "$versions" ] && [ "$versions" = "${HQ_VERSIONS:-$versions}" ]; then
      echo "    $label: Flyway [$versions]"
      return 0
    fi
    [ "$waited" -ge 300 ] && die "$label: Flyway chưa xong sau 300s. Xem log: docker compose logs ${service%-db}-app"
    sleep 3; waited=$((waited + 3))
  done
}

log "2. Chờ Flyway"
HQ_VERSIONS=""
wait_flyway "$HQ_SERVICE" "$HQ_DB" "HQ"
HQ_VERSIONS=$(flyway_versions "$HQ_SERVICE" "$HQ_DB")
until [ "$(hq_value "SELECT count(*) FROM flyway_schema_history WHERE version IS NULL AND description = 'reference data' AND success")" = "1" ]; do sleep 2; done
for b in "${BRANCHES[@]}"; do
  wait_flyway "$(branch_service "$b")" "$(branch_db "$b")" "$(branch_code "$b")"
done

log "3. Thiết lập replication"
for b in "${BRANCHES[@]}"; do
  "$SCRIPTS_DIR/setup-replication.sh" "$b"
done

if [ "$SEED" = true ]; then
  log "4. Nạp dữ liệu demo"
  PY=$(command -v python3 || command -v python || true)
  [ -n "$PY" ] || die "Không tìm thấy python để chạy scripts/seed-demo.py"
  "$PY" "$SCRIPTS_DIR/seed-demo.py" --branches "$(join_by , "${BRANCHES[@]}")"
fi

log "Xong. HQ: http://localhost  |  TP1: http://localhost:81$([[ " ${BRANCHES[*]} " == *" tp2 "* ]] && echo '  |  TP2: http://localhost:82')"
