#!/bin/bash
# ============================================================================
# add-branch.sh <chi nhánh>      vd: scripts/add-branch.sh tp3
#
# Thêm một chi nhánh mới vào hệ thống đang chạy. Các bước THỦ CÔNG cần làm trước:
#
#  1. HQ: tạo dòng branch (màn hình Quản trị > Chi nhánh, hoặc SQL tại HQ) với code = TP3.
#     Ghi lại id số của dòng đó (vd. 3) — app chi nhánh phải chạy với BRANCH_ID = id này.
#  2. docker-compose.yml: chép 3 service branch-tp2-db / branch-tp2-app / branch-tp2-nginx,
#     đổi tp2 -> tp3, BRANCH_ID, cổng (5435, 83), profiles ["tp3"], thêm volume branch_tp3_db_data.
#  3. Tạo nginx-branch-tp3.conf từ nginx-branch-tp2.conf (đổi upstream branch-tp3-app, dải IP).
#  4. Thêm tài khoản STAFF cho chi nhánh (user_account + user_branch_role tại HQ).
#
# Script này làm phần còn lại: kiểm tra điều kiện, bật container, chờ Flyway, thiết lập replication.
# ============================================================================
set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

B=$(branch_key "${1:-}")
CODE=$(branch_code "$B")
PROFILE_ARGS=$(branch_profile_args "$B")

log "Kiểm tra docker-compose.yml có service của $CODE"
# shellcheck disable=SC2086
DEFINED=$($COMPOSE $PROFILE_ARGS config --services)
for svc in "branch-$B-db" "branch-$B-app" "branch-$B-nginx"; do
  grep -qx "$svc" <<<"$DEFINED" || die "docker-compose.yml chưa có service $svc (xem bước 2 ở đầu file)."
done

db_ready "$HQ_SERVICE" "$HQ_DB" || die "HQ chưa chạy. Chạy: docker compose up -d"
BRANCH_ID=$(hq_value "SELECT id FROM branch WHERE upper(code) = '$CODE'")
[ -n "$BRANCH_ID" ] || die "HQ chưa có chi nhánh code = $CODE (xem bước 1 ở đầu file)."
log "$CODE có branch.id = $BRANCH_ID — kiểm tra BRANCH_ID của branch-$B-app khớp giá trị này."

"$SCRIPTS_DIR/branch.sh" on "$B"

log "Chờ Flyway của $CODE"
HQ_V=$(flyway_versions "$HQ_SERVICE" "$HQ_DB")
waited=0
until db_ready "$(branch_service "$B")" "$(branch_db "$B")" && [ "$(flyway_versions "$(branch_service "$B")" "$(branch_db "$B")")" = "$HQ_V" ]; do
  [ "$waited" -ge 300 ] && die "Flyway $CODE chưa xong sau 300s. Xem: docker compose logs branch-$B-app"
  sleep 3; waited=$((waited + 3))
done

"$SCRIPTS_DIR/setup-replication.sh" "$B"
log "Đã thêm chi nhánh $CODE"
