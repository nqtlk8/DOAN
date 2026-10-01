#!/bin/bash
# ============================================================================
# setup-replication.sh <chi nhánh> [--resync-master]      vd: scripts/setup-replication.sh tp1
#
#   --resync-master : chỉ dùng khi thiết lập LẠI chi nhánh có volume DB cũ (sau remove-branch.sh, hoặc
#                     slot ở HQ bị vô hiệu do tắt quá lâu). Xoá master data hiện có ở chi nhánh rồi
#                     copy lại toàn bộ từ HQ. Giao dịch của chi nhánh được GIỮ NGUYÊN (id master không
#                     đổi vì copy từ cùng nguồn HQ).
#
# Thiết lập (hoặc đồng bộ lại) logical replication hai chiều giữa HQ và một chi nhánh.
# Chạy lại nhiều lần an toàn (idempotent): lần sau chỉ cập nhật danh sách bảng.
#
#   HQ  --pub_hq_master-->  sub_<b>_from_hq  (chi nhánh)   : MASTER_TABLES
#   HQ  <--sub_hq_from_<b>--  pub_<b>_to_hq  (chi nhánh)   : TRANSACTION_TABLES + SNAPSHOT_TABLES (lọc branch_id)
#
# Danh sách bảng: scripts/replication-tables.conf
#
# Điều kiện:
#  - Container DB của HQ và chi nhánh đang chạy; app HQ và app chi nhánh đã khởi động ít nhất
#    một lần (Flyway đã tạo schema) và cùng danh sách version Flyway.
#  - HQ đã có dòng branch với code = <CHI NHÁNH> (db/migration-hq/R__reference_data.sql).
#  - Lần đầu: bảng master ở chi nhánh phải TRỐNG (dữ liệu master chỉ đến từ HQ, copy_data = true).
# ============================================================================
set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

B=$(branch_key "${1:-}")
RESYNC_MASTER=false
[ "${2:-}" = "--resync-master" ] && RESYNC_MASTER=true
CODE=$(branch_code "$B")
BR_SERVICE=$(branch_service "$B")
BR_DB=$(branch_db "$B")

HQ_PUB="pub_hq_master"
BR_SUB="sub_${B}_from_hq"
BR_PUB="pub_${B}_to_hq"
HQ_SUB="sub_hq_from_${B}"

# ---------------------------------------------------------------- 1. Kiểm tra kết nối
log "1. Kiểm tra kết nối DB"
db_ready "$HQ_SERVICE" "$HQ_DB" || die "Không kết nối được $HQ_SERVICE/$HQ_DB. Chạy: docker compose up -d"
db_ready "$BR_SERVICE" "$BR_DB" || die "Không kết nối được $BR_SERVICE/$BR_DB. Chạy: scripts/branch.sh on $B"

# ---------------------------------------------------------------- 2. Schema + role
log "2. So khớp schema Flyway HQ <-> $CODE"
HQ_VERSIONS=$(flyway_versions "$HQ_SERVICE" "$HQ_DB")
BR_VERSIONS=$(flyway_versions "$BR_SERVICE" "$BR_DB")
[ -n "$HQ_VERSIONS" ] || die "HQ chưa có schema (flyway_schema_history trống). Khởi động hq-app trước."
[ -n "$BR_VERSIONS" ] || die "$CODE chưa có schema. Khởi động branch-$B-app trước."
[ "$HQ_VERSIONS" = "$BR_VERSIONS" ] || die "Lệch version Flyway. HQ: [$HQ_VERSIONS] | $CODE: [$BR_VERSIONS]"
echo "    version: $HQ_VERSIONS"

for svc_db in "$HQ_SERVICE:$HQ_DB" "$BR_SERVICE:$BR_DB"; do
  svc="${svc_db%%:*}"; db="${svc_db#*:}"
  [ "$(sql_value "$svc" "$db" "SELECT count(*) FROM pg_roles WHERE rolname = '$REPL_DB_USER' AND rolreplication")" = "1" ] \
    || die "Thiếu role replication '$REPL_DB_USER' trên $svc. Volume DB được tạo trước khi có docker/postgres/init/01-roles.sh -> xem REPLICATION_RUNBOOK.md (tạo lại volume)."
  # Bảo đảm role replication đọc được mọi bảng hiện có (bảng mới đã có ALTER DEFAULT PRIVILEGES).
  psql_on "$svc" "$db" -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"$REPL_DB_USER\";"
done

BRANCH_ID=$(hq_value "SELECT id FROM branch WHERE upper(code) = '$CODE'")
[ -n "$BRANCH_ID" ] || die "HQ chưa có chi nhánh code = $CODE trong bảng branch."
echo "    $CODE có branch.id = $BRANCH_ID"

MASTER_LIST=$(join_by ", " "${MASTER_TABLES[@]}")
SNAPSHOT_PARTS=()
for t in "${SNAPSHOT_TABLES[@]}"; do SNAPSHOT_PARTS+=("$t WHERE (branch_id = $BRANCH_ID)"); done
BRANCH_LIST=$(join_by ", " "${TRANSACTION_TABLES[@]}" "${SNAPSHOT_PARTS[@]}")

# ---------------------------------------------------------------- 3. Publication
log "3. Publication $HQ_PUB (HQ) — master data"
if [ "$(hq_value "SELECT count(*) FROM pg_publication WHERE pubname = '$HQ_PUB'")" = "1" ]; then
  hq_sql "ALTER PUBLICATION $HQ_PUB SET TABLE $MASTER_LIST;"
else
  hq_sql "CREATE PUBLICATION $HQ_PUB FOR TABLE $MASTER_LIST;"
fi

log "    Publication $BR_PUB ($CODE) — giao dịch + snapshot lọc branch_id = $BRANCH_ID"
if [ "$(sql_value "$BR_SERVICE" "$BR_DB" "SELECT count(*) FROM pg_publication WHERE pubname = '$BR_PUB'")" = "1" ]; then
  psql_on "$BR_SERVICE" "$BR_DB" -c "ALTER PUBLICATION $BR_PUB SET TABLE $BRANCH_LIST;"
else
  psql_on "$BR_SERVICE" "$BR_DB" -c "CREATE PUBLICATION $BR_PUB FOR TABLE $BRANCH_LIST;"
fi

# ---------------------------------------------------------------- 4. Subscription ở chi nhánh (nhận master data)
log "4. Subscription $BR_SUB ($CODE <- HQ)"
HQ_CONN="host=$HQ_SERVICE port=5432 dbname=$HQ_DB user=$REPL_DB_USER password=$REPL_DB_PASSWORD"
if [ "$(sql_value "$BR_SERVICE" "$BR_DB" "SELECT count(*) FROM pg_subscription WHERE subname = '$BR_SUB'")" = "1" ]; then
  psql_on "$BR_SERVICE" "$BR_DB" -c "ALTER SUBSCRIPTION $BR_SUB REFRESH PUBLICATION WITH (copy_data = true);"
else
  # copy_data = true: master data của chi nhánh đến 100% từ HQ. Nếu bảng đã có dữ liệu
  # (vd. DB chi nhánh cũ còn seed) lần copy sẽ lỗi trùng khóa -> chặn từ đầu.
  NON_EMPTY=()
  for t in "${MASTER_TABLES[@]}"; do
    [ "$(sql_value "$BR_SERVICE" "$BR_DB" "SELECT EXISTS (SELECT 1 FROM $t)")" = "t" ] && NON_EMPTY+=("$t")
  done
  if [ ${#NON_EMPTY[@]} -gt 0 ]; then
    [ "$RESYNC_MASTER" = true ] || die "Bảng master ở $CODE đã có dữ liệu: ${NON_EMPTY[*]}. Master data chỉ được đến từ HQ. Thiết lập lại chi nhánh cũ: thêm --resync-master; DB mới thì tạo lại volume (xem REPLICATION_RUNBOOK.md)."
    log "    --resync-master: xoá master data cũ ở $CODE để copy lại từ HQ (giữ nguyên giao dịch)"
    # session_replication_role = replica: không kích hoạt FK -> giao dịch đang tham chiếu product/customer...
    # vẫn giữ nguyên; các dòng master sẽ được copy lại với đúng id cũ từ HQ.
    DELETES=""
    for t in "${MASTER_TABLES[@]}"; do DELETES="$DELETES DELETE FROM $t;"; done
    psql_on "$BR_SERVICE" "$BR_DB" -c "BEGIN; SET LOCAL session_replication_role = replica; $DELETES COMMIT;"
  fi
  psql_on "$BR_SERVICE" "$BR_DB" -c "CREATE SUBSCRIPTION $BR_SUB CONNECTION '$HQ_CONN' PUBLICATION $HQ_PUB WITH (copy_data = true);"
fi

# ---------------------------------------------------------------- 5. Subscription ở HQ (nhận giao dịch)
log "5. Subscription $HQ_SUB (HQ <- $CODE)"
BR_CONN="host=$BR_SERVICE port=5432 dbname=$BR_DB user=$REPL_DB_USER password=$REPL_DB_PASSWORD"
if [ "$(hq_value "SELECT count(*) FROM pg_subscription WHERE subname = '$HQ_SUB'")" = "1" ]; then
  hq_sql "ALTER SUBSCRIPTION $HQ_SUB ENABLE;"
  hq_sql "ALTER SUBSCRIPTION $HQ_SUB REFRESH PUBLICATION WITH (copy_data = true);"
else
  # HQ không bao giờ tự ghi dữ liệu giao dịch của chi nhánh. Nếu HQ đã có dữ liệu của chi nhánh này
  # (lần setup trước) thì copy ban đầu sẽ trùng khóa -> dừng, tránh trộn hai nguồn.
  EXISTING=$(hq_value "SELECT (SELECT count(*) FROM sales_invoice WHERE branch_id = $BRANCH_ID) + (SELECT count(*) FROM stock_movement WHERE branch_id = $BRANCH_ID) + (SELECT count(*) FROM stock_on_hand WHERE branch_id = $BRANCH_ID) + (SELECT count(*) FROM receivable_debt_movement WHERE branch_id = $BRANCH_ID)")
  [ "$EXISTING" = "0" ] || die "HQ đã có $EXISTING dòng giao dịch của $CODE từ lần thiết lập trước. Chạy scripts/remove-branch.sh $B --purge-hq-data rồi chạy lại."
  hq_sql "CREATE SUBSCRIPTION $HQ_SUB CONNECTION '$BR_CONN' PUBLICATION $BR_PUB WITH (copy_data = true);"
fi

# ---------------------------------------------------------------- 6. Chờ đồng bộ ban đầu
log "6. Chờ copy dữ liệu ban đầu"
wait_subscription_ready "$BR_SERVICE" "$BR_DB" "$BR_SUB"
wait_subscription_ready "$HQ_SERVICE" "$HQ_DB" "$HQ_SUB"

log "7. Trạng thái"
psql_on "$BR_SERVICE" "$BR_DB" -c "SELECT subname, subenabled AS bat FROM pg_subscription;"
hq_sql "SELECT subname, subenabled AS bat FROM pg_subscription ORDER BY 1;"
hq_sql "SELECT slot_name, active, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS wal_dang_giu FROM pg_replication_slots ORDER BY 1;"
log "Hoàn tất replication HQ <-> $CODE"
