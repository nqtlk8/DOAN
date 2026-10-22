#!/bin/bash
# ============================================================================
# Hàm dùng chung cho kiểm thử hệ thống (source file này, không chạy trực tiếp).
#  - Stack Docker (code/docker-compose.yml) được điều khiển từ thư mục đó.
#  - Đọc .env của stack (nếu có) để lấy user/mật khẩu DB.
#  - psql chạy qua "docker compose exec" theo TÊN SERVICE.
# ============================================================================

SYSTEM_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SYSTEM_DIR/../.." && pwd)"
STACK_DIR="$(cd "$BACKEND_DIR/../../.." && pwd)"
cd "$STACK_DIR"

if [ -f "$STACK_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . <(tr -d '\r' < "$STACK_DIR/.env")
  set +a
fi

POSTGRES_USER="${POSTGRES_USER:-erp_user}"
APP_DB_USER="${APP_DB_USER:-erp_app}"
APP_DB_PASSWORD="${APP_DB_PASSWORD:-erp_app_password}"
HQ_SERVICE="hq-db"
HQ_DB="erp_hq"
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
