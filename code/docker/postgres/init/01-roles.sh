#!/bin/bash
# ============================================================================
# Khởi tạo role DB — chạy MỘT lần khi volume Postgres được tạo mới
# (thư mục /docker-entrypoint-initdb.d của image postgres; mount cho cả HQ và mọi chi nhánh).
#
#   POSTGRES_USER (erp_user)  : superuser, chủ sở hữu bảng. Flyway và script replication dùng role này.
#   APP_DB_USER   (erp_app)   : role ứng dụng Spring Boot, KHÔNG phải superuser.
#                               HQ: đọc/ghi mọi bảng. Chi nhánh: R__branch_db_security.sql thu hẹp
#                               còn "chỉ đọc master data, ghi bảng giao dịch".
#   REPL_DB_USER  (erp_repl)  : role mà subscription dùng để kết nối tới publisher (chỉ SELECT).
#
# Quyền trên bảng được cấp qua ALTER DEFAULT PRIVILEGES: mọi bảng Flyway (POSTGRES_USER) tạo sau
# này tự có quyền tương ứng, không cần GRANT lại sau mỗi migration.
#
# Đổi mật khẩu: sửa file .env TRƯỚC lần chạy đầu tiên. Volume đã tạo rồi thì script không chạy lại.
# ============================================================================
set -euo pipefail

APP_DB_USER="${APP_DB_USER:-erp_app}"
APP_DB_PASSWORD="${APP_DB_PASSWORD:-erp_app_password}"
REPL_DB_USER="${REPL_DB_USER:-erp_repl}"
REPL_DB_PASSWORD="${REPL_DB_PASSWORD:-erp_repl_password}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v app_user="$APP_DB_USER" -v app_pass="$APP_DB_PASSWORD" \
     -v repl_user="$REPL_DB_USER" -v repl_pass="$REPL_DB_PASSWORD" \
     -v owner="$POSTGRES_USER" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user') \gexec

SELECT format('CREATE ROLE %I LOGIN REPLICATION PASSWORD %L', :'repl_user', :'repl_pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'repl_user') \gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO %I, %I', current_database(), :'app_user', :'repl_user') \gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I, %I', :'app_user', :'repl_user') \gexec

-- Ứng dụng: đọc/ghi dữ liệu (không DDL). Chi nhánh bị thu hẹp thêm bởi R__branch_db_security.sql.
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I', :'owner', :'app_user') \gexec
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO %I', :'owner', :'app_user') \gexec
-- Màn hình trạng thái replication (ReplicationStatusController) đọc pg_stat_replication đầy đủ cột.
SELECT format('GRANT pg_read_all_stats TO %I', :'app_user') \gexec

-- Replication: subscription cần SELECT để copy dữ liệu ban đầu (copy_data = true).
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT ON TABLES TO %I', :'owner', :'repl_user') \gexec
EOSQL

echo "01-roles.sh: đã tạo role $APP_DB_USER, $REPL_DB_USER trên database $POSTGRES_DB"
