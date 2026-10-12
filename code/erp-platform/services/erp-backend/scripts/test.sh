#!/usr/bin/env bash
# ============================================================================
# Chạy kiểm thử backend. Có thể gọi từ bất kỳ thư mục nào.
#
#   scripts/test.sh unit          Unit + WebMvc (*Test)                 -> mvn test
#   scripts/test.sh it            Tích hợp Spring/DB (*IT), H2 + PostgreSQL -> mvn verify (chỉ failsafe)
#   scripts/test.sh verify        Toàn bộ: unit + *IT                   -> mvn verify
#   scripts/test.sh e2e           Replication E2E trên 2 PostgreSQL     -> dựng docker-compose, tự dọn
#   scripts/test.sh smoke         Smoke nhóm 2 trên stack đang chạy     -> system/smoke-group2.sh
#   scripts/test.sh replication   Replication 2 chiều trên stack đang chạy -> system/replication-e2e.sh
#
# Tham số sau tên chế độ được chuyển nguyên cho Maven, vd: scripts/test.sh unit -Dtest=AuthServiceTest
# Yêu cầu: JDK 21, Docker (it/e2e dùng Testcontainers hoặc docker-compose).
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_DIR"

E2E_COMPOSE="src/test/resources/replication-e2e/docker-compose.yml"
# Không chạy test nào ở surefire khi chỉ muốn chạy nhóm failsafe (*IT)
NO_UNIT_TESTS=(-Dtest=__none__ -Dsurefire.failIfNoSpecifiedTests=false)

usage() {
  awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "$0"
  exit "${1:-1}"
}

maven() { sh ./mvnw "$@"; }
dc()    { docker compose -f "$E2E_COMPOSE" "$@"; }

# Chờ subscription đồng bộ xong bảng category (bảng duy nhất được replicate).
wait_subscription() {
  local i state=""
  for i in $(seq 1 60); do
    state=$(dc exec -T pg-replica psql -U erp_user -d erp_db -At \
      -c "SELECT count(*) FILTER (WHERE srsubstate = 'r')::text || '|' || count(*)::text FROM pg_subscription_rel" \
      | tr -d '\r')
    if [ "$state" = "1|1" ]; then
      echo "==> Subscription sẵn sàng"
      return 0
    fi
    sleep 1
  done
  echo "LỖI: subscription chưa sẵn sàng sau 60s (trạng thái: $state)" >&2
  return 1
}

run_e2e() {
  trap 'dc down -v >/dev/null 2>&1 || true' EXIT
  dc down -v >/dev/null 2>&1 || true
  echo "==> Dựng 2 PostgreSQL (master + replica)"
  dc up -d --wait

  echo "==> Tạo bảng và logical replication (master -> replica)"
  local ddl="CREATE TABLE IF NOT EXISTS category (id serial PRIMARY KEY, code varchar(50) UNIQUE NOT NULL, name varchar(200) NOT NULL, is_active boolean NOT NULL)"
  dc exec -T pg-master  psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q -c "$ddl"
  dc exec -T pg-replica psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q -c "$ddl"
  dc exec -T pg-master  psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q \
    -c "DROP PUBLICATION IF EXISTS erp_pub" -c "CREATE PUBLICATION erp_pub FOR TABLE category"
  dc exec -T pg-replica psql -U erp_user -d erp_db -v ON_ERROR_STOP=1 -X -q \
    -c "DROP SUBSCRIPTION IF EXISTS erp_sub" \
    -c "CREATE SUBSCRIPTION erp_sub CONNECTION 'host=pg-master port=5432 user=erp_user password=erp_password dbname=erp_db' PUBLICATION erp_pub"
  wait_subscription

  echo "==> Chạy ReplicationE2EIT"
  maven verify -Pe2e "${NO_UNIT_TESTS[@]}" -Dit.test=ReplicationE2EIT -Dfailsafe.failIfNoSpecifiedTests=false "$@"
}

[ $# -ge 1 ] || usage 1
MODE="$1"; shift

case "$MODE" in
  unit)        maven test "$@" ;;
  it)          maven verify "${NO_UNIT_TESTS[@]}" "$@" ;;
  verify)      maven verify "$@" ;;
  e2e)         run_e2e "$@" ;;
  smoke)       bash "$SCRIPT_DIR/system/smoke-group2.sh" "$@" ;;
  replication) bash "$SCRIPT_DIR/system/replication-e2e.sh" "$@" ;;
  -h|--help|help) usage 0 ;;
  *) echo "Chế độ không hợp lệ: $MODE" >&2; usage 1 ;;
esac
