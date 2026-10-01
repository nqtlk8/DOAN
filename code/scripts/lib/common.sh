#!/bin/bash
# ============================================================================
# Hàm dùng chung cho các script vận hành (source file này, không chạy trực tiếp).
#  - Chạy từ bất kỳ đâu: tự cd về thư mục chứa docker-compose.yml.
#  - Đọc .env (nếu có) để lấy user/mật khẩu DB giống docker-compose.
#  - Gọi psql qua "docker compose exec" theo TÊN SERVICE, không phụ thuộc tên container
#    (code-hq-db-1 ...) hay tên thư mục dự án.
# ============================================================================

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_DIR="$(cd "$SCRIPTS_DIR/.." && pwd)"
cd "$PROJECT_DIR"

if [ -f "$PROJECT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . <(tr -d '\r' < "$PROJECT_DIR/.env")
  set +a
fi

POSTGRES_USER="${POSTGRES_USER:-erp_user}"
REPL_DB_USER="${REPL_DB_USER:-erp_repl}"
REPL_DB_PASSWORD="${REPL_DB_PASSWORD:-erp_repl_password}"
APP_DB_USER="${APP_DB_USER:-erp_app}"

# shellcheck disable=SC1091
. "$SCRIPTS_DIR/replication-tables.conf"

HQ_SERVICE="hq-db"
HQ_DB="erp_hq"

# Cho phép thay lệnh compose (vd. COMPOSE="docker compose -p erp") khi cần.
COMPOSE="${COMPOSE:-docker compose}"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mWARN:\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31mLỖI:\033[0m %s\n' "$*" >&2; exit 1; }

# Chuẩn hoá tên chi nhánh: "TP1" | "tp1" -> tp1
branch_key() {
  local b="${1:-}"
  [ -n "$b" ] || die "Thiếu tên chi nhánh (vd. tp1, tp2)"
  echo "$b" | tr '[:upper:]' '[:lower:]'
}
branch_code()    { echo "$1" | tr '[:lower:]' '[:upper:]'; }
branch_service() { echo "branch-$1-db"; }
branch_db()      { echo "erp_branch_$1"; }
branch_profile_args() {
  # TP1 chạy mặc định; các chi nhánh khác nằm trong compose profile cùng tên.
  [ "$1" = "tp1" ] && return 0
  echo "--profile $1"
}

# psql_on <service> <database> [psql args...] — chạy bằng POSTGRES_USER (superuser) trong container.
psql_on() {
  local service="$1" db="$2"; shift 2
  # MSYS_NO_PATHCONV: Git Bash trên Windows không tự đổi đối số dạng /path.
  MSYS_NO_PATHCONV=1 $COMPOSE exec -T "$service" psql -U "$POSTGRES_USER" -d "$db" -v ON_ERROR_STOP=1 -X -q "$@"
}
# sql_value <service> <database> <sql> — trả về 1 giá trị (đã trim).
sql_value() {
  psql_on "$1" "$2" -At -c "$3" | tr -d '\r' | head -n1 | xargs
}
hq_sql()   { psql_on "$HQ_SERVICE" "$HQ_DB" -c "$1"; }
hq_value() { sql_value "$HQ_SERVICE" "$HQ_DB" "$1"; }

# db_ready <service> <database>
db_ready() {
  MSYS_NO_PATHCONV=1 $COMPOSE exec -T "$1" pg_isready -U "$POSTGRES_USER" -d "$2" >/dev/null 2>&1
}

# join_by <sep> <items...>
join_by() { local IFS="$1"; shift; echo "$*"; }

# Danh sách version Flyway (versioned) đã chạy thành công, nối bằng dấu phẩy.
flyway_versions() {
  sql_value "$1" "$2" "SELECT coalesce(string_agg(version, ',' ORDER BY installed_rank), '') FROM flyway_schema_history WHERE version IS NOT NULL AND success" 2>/dev/null || true
}

# wait_subscription_ready <service> <database> <subscription> [timeout giây]
# Chờ tới khi mọi bảng của subscription ở trạng thái 'r' (ready) và in bảng trạng thái.
wait_subscription_ready() {
  local service="$1" db="$2" sub="$3" timeout="${4:-180}" waited=0 pending
  while :; do
    pending=$(sql_value "$service" "$db" "SELECT count(*) FROM pg_subscription_rel sr JOIN pg_subscription s ON s.oid = sr.srsubid WHERE s.subname = '$sub' AND sr.srsubstate <> 'r'")
    [ "$pending" = "0" ] && break
    if [ "$waited" -ge "$timeout" ]; then
      warn "Subscription $sub còn $pending bảng chưa sẵn sàng sau ${timeout}s:"
      psql_on "$service" "$db" -c "SELECT c.relname AS bang, sr.srsubstate AS trang_thai FROM pg_subscription_rel sr JOIN pg_subscription s ON s.oid = sr.srsubid JOIN pg_class c ON c.oid = sr.srrelid WHERE s.subname = '$sub' AND sr.srsubstate <> 'r' ORDER BY 1"
      return 1
    fi
    sleep 2; waited=$((waited + 2))
  done
  log "Subscription $sub: mọi bảng đã đồng bộ (trạng thái r)"
}
