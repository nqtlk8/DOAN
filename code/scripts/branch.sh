#!/bin/bash
# ============================================================================
# branch.sh on|off|status <chi nhánh>      vd: scripts/branch.sh off tp2
#
# Bật/tắt một chi nhánh trong docker-compose KÈM subscription tương ứng ở HQ.
#
# Vì sao cần script: HQ có subscription sub_hq_from_<b> kéo giao dịch từ DB chi nhánh. Nếu chỉ
# dừng container chi nhánh, subscription đó vẫn bật và HQ log lỗi liên tục mỗi 5 giây:
#   could not connect to the publisher: could not translate host name "branch-tp2-db" ...
# "off" tắt subscription trước rồi mới dừng container; "on" làm ngược lại.
#
# Lưu ý khi tắt lâu: slot sub_<b>_from_hq trên HQ vẫn giữ WAL cho chi nhánh (để lúc bật lại
# không mất master data). Giới hạn bởi max_slot_wal_keep_size=2GB (docker-compose.yml); vượt
# ngưỡng thì slot bị vô hiệu và phải thiết lập lại chi nhánh (remove-branch.sh + setup-replication.sh).
# ============================================================================
set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

ACTION="${1:-}"
B=$(branch_key "${2:-}")
CODE=$(branch_code "$B")
HQ_SUB="sub_hq_from_${B}"
# shellcheck disable=SC2046
PROFILE_ARGS=$(branch_profile_args "$B")

hq_sub_exists() {
  db_ready "$HQ_SERVICE" "$HQ_DB" || return 1
  [ "$(hq_value "SELECT count(*) FROM pg_subscription WHERE subname = '$HQ_SUB'")" = "1" ]
}

case "$ACTION" in
  on)
    # Thứ tự: DB chi nhánh -> bật subscription ở HQ -> app + nginx. Bật subscription ngay khi DB
    # sẵn sàng để giao dịch được kéo về HQ kể cả khi app/nginx khởi động lỗi.
    log "Khởi động branch-$B-db"
    # shellcheck disable=SC2086
    $COMPOSE $PROFILE_ARGS up -d "branch-$B-db"
    waited=0
    until db_ready "$(branch_service "$B")" "$(branch_db "$B")"; do
      [ "$waited" -ge 120 ] && die "DB $CODE chưa sẵn sàng sau 120s"
      sleep 2; waited=$((waited + 2))
    done
    if hq_sub_exists; then
      log "Bật lại subscription $HQ_SUB ở HQ"
      hq_sql "ALTER SUBSCRIPTION $HQ_SUB ENABLE;"
    else
      warn "HQ chưa có $HQ_SUB. Lần đầu: đợi app $CODE khởi động xong (Flyway) rồi chạy scripts/setup-replication.sh $B"
    fi
    log "Khởi động branch-$B-app branch-$B-nginx"
    # shellcheck disable=SC2086
    $COMPOSE $PROFILE_ARGS up -d "branch-$B-app" "branch-$B-nginx"
    ;;
  off)
    [ "$B" = "tp1" ] && warn "TP1 là chi nhánh mặc định; vẫn tắt theo yêu cầu."
    if hq_sub_exists; then
      log "Tắt subscription $HQ_SUB ở HQ"
      hq_sql "ALTER SUBSCRIPTION $HQ_SUB DISABLE;"
    fi
    log "Dừng branch-$B-nginx branch-$B-app branch-$B-db"
    # shellcheck disable=SC2086
    $COMPOSE $PROFILE_ARGS stop "branch-$B-nginx" "branch-$B-app" "branch-$B-db"
    ;;
  status)
    hq_sql "SELECT s.subname, s.subenabled AS bat, st.pid IS NOT NULL AS dang_ket_noi, st.last_msg_receipt_time AS nhan_lan_cuoi FROM pg_subscription s LEFT JOIN pg_stat_subscription st ON st.subid = s.oid AND st.relid IS NULL ORDER BY 1;"
    hq_sql "SELECT slot_name, active, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS wal_dang_giu, wal_status FROM pg_replication_slots ORDER BY 1;"
    ;;
  *)
    die "Cách dùng: $0 on|off|status <chi nhánh>   (vd. $0 off tp2)"
    ;;
esac
