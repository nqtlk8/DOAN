#!/bin/bash
# ============================================================================
# remove-branch.sh <chi nhánh> [--purge-hq-data]
#
# Gỡ replication giữa HQ và một chi nhánh (dùng khi bỏ chi nhánh, khi slot HQ bị vô hiệu do
# tắt quá lâu, hoặc trước khi thiết lập lại chi nhánh từ volume DB mới).
#
#  - HQ: xoá subscription sub_hq_from_<b> và slot sub_<b>_from_hq (giải phóng WAL đang giữ).
#  - Chi nhánh (nếu đang chạy): xoá subscription sub_<b>_from_hq và publication pub_<b>_to_hq.
#  - --purge-hq-data: xoá luôn dữ liệu giao dịch/snapshot của chi nhánh đã replicate lên HQ
#    (cần khi setup-replication báo "HQ đã có ... dòng giao dịch của chi nhánh").
#
# Không xoá dòng chi nhánh trong bảng branch và không xoá volume DB chi nhánh.
# ============================================================================
set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

B=$(branch_key "${1:-}")
PURGE="${2:-}"
CODE=$(branch_code "$B")
BR_SERVICE=$(branch_service "$B")
BR_DB=$(branch_db "$B")
HQ_SUB="sub_hq_from_${B}"
BR_SUB="sub_${B}_from_hq"
BR_PUB="pub_${B}_to_hq"

db_ready "$HQ_SERVICE" "$HQ_DB" || die "HQ DB chưa chạy"

if [ "$(hq_value "SELECT count(*) FROM pg_subscription WHERE subname = '$HQ_SUB'")" = "1" ]; then
  log "HQ: xoá subscription $HQ_SUB"
  if db_ready "$BR_SERVICE" "$BR_DB"; then
    hq_sql "DROP SUBSCRIPTION $HQ_SUB;"
  else
    # Chi nhánh không chạy -> không xoá được slot phía chi nhánh; tách slot khỏi subscription rồi xoá.
    hq_sql "ALTER SUBSCRIPTION $HQ_SUB DISABLE;"
    hq_sql "ALTER SUBSCRIPTION $HQ_SUB SET (slot_name = NONE);"
    hq_sql "DROP SUBSCRIPTION $HQ_SUB;"
    warn "Slot $HQ_SUB trên DB $CODE chưa được xoá (DB không chạy) — nếu dùng lại volume đó, xoá bằng pg_drop_replication_slot."
  fi
fi

if db_ready "$BR_SERVICE" "$BR_DB"; then
  if [ "$(sql_value "$BR_SERVICE" "$BR_DB" "SELECT count(*) FROM pg_subscription WHERE subname = '$BR_SUB'")" = "1" ]; then
    log "$CODE: xoá subscription $BR_SUB (đồng thời xoá slot trên HQ)"
    psql_on "$BR_SERVICE" "$BR_DB" -c "DROP SUBSCRIPTION $BR_SUB;"
  fi
  psql_on "$BR_SERVICE" "$BR_DB" -c "DROP PUBLICATION IF EXISTS $BR_PUB;"
fi

# Slot của chi nhánh trên HQ còn sót (vd. chi nhánh đã mất volume) -> xoá để HQ không giữ WAL.
if [ "$(hq_value "SELECT count(*) FROM pg_replication_slots WHERE slot_name = '$BR_SUB' AND NOT active")" = "1" ]; then
  log "HQ: xoá slot $BR_SUB"
  hq_sql "SELECT pg_drop_replication_slot('$BR_SUB');"
fi

if [ "$PURGE" = "--purge-hq-data" ]; then
  BRANCH_ID=$(hq_value "SELECT id FROM branch WHERE upper(code) = '$CODE'")
  [ -n "$BRANCH_ID" ] || die "Không tìm thấy chi nhánh $CODE ở HQ"
  log "HQ: xoá dữ liệu giao dịch/snapshot của $CODE (branch_id = $BRANCH_ID)"
  hq_sql "BEGIN;
    DELETE FROM sales_invoice_line WHERE invoice_id IN (SELECT id FROM sales_invoice WHERE branch_id = $BRANCH_ID);
    DELETE FROM sales_invoice WHERE branch_id = $BRANCH_ID;
    DELETE FROM goods_return_line WHERE return_id IN (SELECT id FROM goods_return WHERE branch_id = $BRANCH_ID);
    DELETE FROM goods_return WHERE branch_id = $BRANCH_ID;
    DELETE FROM inbound_receipt_line WHERE receipt_id IN (SELECT id FROM inbound_receipt WHERE branch_id = $BRANCH_ID);
    DELETE FROM inbound_receipt WHERE branch_id = $BRANCH_ID;
    DELETE FROM cost_layer WHERE branch_id = $BRANCH_ID;
    DELETE FROM stock_movement WHERE branch_id = $BRANCH_ID;
    DELETE FROM receivable_debt_movement WHERE branch_id = $BRANCH_ID;
    DELETE FROM stock_on_hand WHERE branch_id = $BRANCH_ID;
    DELETE FROM receivable_debt WHERE branch_id = $BRANCH_ID;
    COMMIT;"
fi

log "Đã gỡ replication HQ <-> $CODE"
