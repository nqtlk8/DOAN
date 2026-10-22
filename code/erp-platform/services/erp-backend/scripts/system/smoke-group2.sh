#!/bin/bash
# ============================================================================
# smoke-group2.sh — Kiểm tra nhóm 2 (dữ liệu nền) trên stack Docker đang chạy (HQ + PostgreSQL thật).
#
# Chạy từ erp-backend:   scripts/test.sh smoke   (hoặc: bash scripts/system/smoke-group2.sh)
# Biến tùy chọn:  BASE (mặc định http://127.0.0.1:8080), ADMIN_USER/ADMIN_PASS (mặc định admin/password),
#                 CAT_ID (id danh mục con nếu tự dò không được), POSTGRES_USER (mặc định erp_user).
#
# Không xóa chi nhánh, không sửa chi nhánh có thật. Dữ liệu test (mã SMK-<thời điểm>) được tạo rồi
# xóa mềm ở cuối; mã của chúng vẫn bị chiếm (đúng thiết kế: mã là duy nhất kể cả khi đã xóa mềm).
# Mã SMK-xx khớp với test JUnit TC-xx trong báo cáo kiểm tra nhóm 2.
# ============================================================================
set -uo pipefail

# Ghi toàn bộ output vào target/system-tests/smoke-group2.log (để copy/gửi lại khi có FAIL)
. "$(dirname "${BASH_SOURCE[0]}")/common.sh"
mkdir -p "$BACKEND_DIR/target/system-tests"
LOG="$BACKEND_DIR/target/system-tests/smoke-group2.log"
exec > >(tee "$LOG") 2>&1

# Mọi lần thoát (kể cả khi lỗi đăng nhập ở đầu) đều giữ cửa sổ mở để đọc kết quả; Enter để đóng.
on_exit() {
    local rc=$?
    [ -n "${BODY_FILE:-}" ] && rm -f "$BODY_FILE"
    echo "Log đã lưu tại: $LOG (mã thoát: $rc)"
    if [ -t 0 ] && [ -z "${NO_PAUSE:-}" ]; then read -rp "Nhấn Enter để đóng..." _; fi
    exit "$rc"
}
trap on_exit EXIT

BASE="${BASE:-http://127.0.0.1:8080}"
BODY_FILE="$(mktemp)"
S="$(date +%s)"
PASS=0; FAIL=0
BODY=""; CODE=""

req() { # req METHOD PATH [JSON] [TOKEN|-]
    local method="$1" path="$2" data="${3:-}" token="${4:-$TOKEN}"
    local args=(-s -o "$BODY_FILE" -w "%{http_code}" -X "$method" "$BASE$path")
    [ "$token" != "-" ] && args+=(-H "Authorization: Bearer $token")
    # Gửi body qua stdin (--data-binary @-) thay vì tham số dòng lệnh: trên Windows (Git Bash)
    # tham số bị đổi sang code page hệ thống nên ký tự có dấu (vd "ù") thành byte không phải UTF-8.
    if [ -n "$data" ]; then
        args+=(-H "Content-Type: application/json; charset=utf-8" --data-binary @-)
        CODE=$(printf '%s' "$data" | curl "${args[@]}")
    else
        CODE=$(curl "${args[@]}")
    fi
    BODY=$(cat "$BODY_FILE")
}

check() { # check ID "mô tả" expected_status [chuỗi phải có] [chuỗi không được có]
    local id="$1" desc="$2" want="$3" must="${4:-}" mustnot="${5:-}" ok=1
    [ "$CODE" = "$want" ] || ok=0
    [ -n "$must" ] && ! grep -qF -- "$must" <<<"$BODY" && ok=0
    [ -n "$mustnot" ] && grep -qF -- "$mustnot" <<<"$BODY" && ok=0
    if [ $ok = 1 ]; then PASS=$((PASS+1)); echo "PASS $id $desc"
    else FAIL=$((FAIL+1)); echo "FAIL $id $desc -> HTTP $CODE (mong đợi $want) body: ${BODY:0:600}"; fi
}

json_field() { grep -o "\"$1\":\"\\?[^,\"}]*" <<<"$BODY" | head -1 | sed -E "s/\"$1\":\"?//"; }

# ---------------------------------------------------------------- đăng nhập
TOKEN="-"
req POST /api/v1/auth/login "{\"username\":\"${ADMIN_USER:-admin}\",\"password\":\"${ADMIN_PASS:-password}\"}" -
TOKEN=$(json_field accessToken)
if [ -z "$TOKEN" ]; then echo "Không đăng nhập được ($CODE): $BODY"; exit 2; fi

# Danh mục con đầu tiên (parentId khác null)
if [ -z "${CAT_ID:-}" ]; then
    req GET /api/v1/catalog/categories
    CAT_ID=$(grep -o '"id":[0-9]*,"code":"[^"]*","name":"[^"]*","parentId":[0-9]' <<<"$BODY" | head -1 | sed -E 's/"id":([0-9]+).*/\1/')
fi
[ -z "$CAT_ID" ] && { echo "Không tìm thấy danh mục con, đặt CAT_ID=<id>"; exit 2; }

# ---------------------------------------------------------------- Ràng buộc giá niêm yết trên PostgreSQL
PRICE_UK=$(docker compose exec -T hq-db psql -U "${POSTGRES_USER:-erp_user}" -d erp_hq -tAc \
    "SELECT count(*) FROM pg_constraint WHERE conname = 'uk_price_list_product_branch'" 2>/dev/null | tr -d '[:space:]')
CODE="$PRICE_UK"; BODY=""
check SMK-00 "migration V2 tạo ràng buộc uk_price_list_product_branch" "1"

# ---------------------------------------------------------------- sản phẩm (TC-P01..P04)
P="SMK-P-$S"
req POST /api/v1/catalog/products "{\"code\":\"$P\",\"name\":\"Smoke $P\",\"categoryId\":$CAT_ID,\"baseUnit\":\"cai\"}"
check SMK-01a "tạo sản phẩm mới" 201
PID=$(json_field id)
req POST /api/v1/catalog/products "{\"code\":\"$P\",\"name\":\"Smoke tr\u00f9ng\",\"categoryId\":$CAT_ID,\"baseUnit\":\"cai\"}"
check SMK-01b "tạo sản phẩm trùng mã -> 409" 409 PRODUCT_CODE_EXISTS

req DELETE "/api/v1/catalog/products/$PID"
check SMK-02a "ngừng bán (xóa mềm) sản phẩm" 200
req GET "/api/v1/catalog/products/$PID"
check SMK-02b "JSON sản phẩm dùng khóa isActive" 200 '"isActive":false' '"active":'
req POST /api/v1/catalog/products "{\"code\":\"$P\",\"name\":\"Smoke l\u1ea1i\",\"categoryId\":$CAT_ID,\"baseUnit\":\"cai\"}"
check SMK-02c "tạo lại mã của sản phẩm đã ngừng bán -> 409" 409 PRODUCT_CODE_EXISTS

req GET "/api/v1/catalog/products/987654321?withBranchPrice=true"
check SMK-03 "sản phẩm kèm giá, id không tồn tại -> 404" 404 NOT_FOUND

# ---------------------------------------------------------------- công khai (TC-P10, P11)
req GET "/api/v1/public/catalog/products/$PID" "" -
check SMK-04a "chi tiết công khai sản phẩm ngừng bán -> 404" 404
req GET "/api/v1/public/catalog/products?size=0" "" -
check SMK-04b "danh sách công khai size=0 -> 400" 400
req GET "/api/v1/public/catalog/products?page=0&size=1" "" -
N=$(grep -o '"baseUnit"' <<<"$BODY" | wc -l | tr -d ' ')
[ "$N" -gt 1 ] && CODE="size=1 nhưng trả $N sản phẩm"
check SMK-04c "danh sách công khai size=1 trả tối đa 1 sản phẩm" 200 "" '"isActive":false'

# ---------------------------------------------------------------- nhà cung cấp (TC-S01, S02)
SP="SMK-S-$S"; TAX="9$S"
req POST /api/v1/suppliers "{\"code\":\"$SP\",\"name\":\"NCC smoke\",\"taxCode\":\"$TAX\"}"
check SMK-05a "tạo NCC mới" 200
SID=$(json_field id)
req POST /api/v1/suppliers "{\"code\":\"$SP\",\"name\":\"NCC tr\u00f9ng m\u00e3\"}"
check SMK-05b "tạo NCC trùng mã -> 409" 409 SUPPLIER_CODE_EXISTS
req POST /api/v1/suppliers "{\"code\":\"$SP-2\",\"name\":\"NCC tr\u00f9ng MST\",\"taxCode\":\"$TAX\"}"
check SMK-05c "tạo NCC trùng mã số thuế -> 409" 409 SUPPLIER_TAX_CODE_EXISTS

# ---------------------------------------------------------------- khách hàng (TC-C01, C03, C06)
C="SMK-C-$S"
req POST /api/v1/customers "{\"customerCode\":\"$C\",\"name\":\"Khach smoke\"}"
check SMK-06a "tạo khách mới, JSON dùng khóa isDeleted" 201 '"isDeleted":false' '"deleted":'
CID=$(json_field id)
req POST /api/v1/customers "{\"customerCode\":\"$C\",\"name\":\"Khach trung\"}"
check SMK-06b "tạo khách trùng mã -> 409" 409 CUSTOMER_CODE_EXISTS
req POST /api/v1/customers "{\"customerCode\":\"$C-2\",\"name\":\"Khach sai CN\",\"branchId\":987654}"
check SMK-07 "tạo khách với branchId không tồn tại -> 400" 400 INVALID_ARGUMENT

# ---------------------------------------------------------------- chi nhánh (TC-B01, B02, B07) — không đổi dữ liệu thật
req DELETE /api/v1/branches/1
check SMK-08a "DELETE chi nhánh -> 405 (không có API xóa)" 405 METHOD_NOT_ALLOWED
req GET /api/v1/branches/1
check SMK-08b "chi nhánh 1 vẫn còn" 200 '"isActive":' '"active":'
req POST /api/v1/branches '{"code":"TP1","name":"Tr\u00f9ng m\u00e3 TP1"}'
check SMK-09 "tạo chi nhánh trùng mã TP1 -> 409" 409 BRANCH_CODE_EXISTS
req PUT /api/v1/branches/1 '{"internalUrl":"ftp://localhost:81"}'
check SMK-10 "URL cổng không phải http(s) -> 400, không đổi dữ liệu" 400 INVALID_ARGUMENT

# ---------------------------------------------------------------- dọn dẹp (xóa mềm)
[ -n "${SID:-}" ] && req DELETE "/api/v1/suppliers/$SID"
[ -n "${CID:-}" ] && req DELETE "/api/v1/customers/$CID"

echo "----"
echo "Kết quả: $PASS PASS, $FAIL FAIL"
[ "$FAIL" = 0 ]
