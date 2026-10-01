#!/usr/bin/env python3
"""
seed-demo.py — Nạp dữ liệu DEMO cho hệ thống ERP đang chạy (docker-compose) qua đúng luồng nghiệp vụ.

    python scripts/seed-demo.py                    # HQ + TP1
    python scripts/seed-demo.py --branches tp1,tp2 # thêm giao dịch cho TP2 (TP2 phải đang chạy)

Nguyên tắc (để dữ liệu đúng cấu trúc code, không "bịa" id):
  * Không tự đặt id. UUID và id số do ứng dụng / sequence của Postgres cấp.
  * Mã chứng từ do backend sinh (hóa đơn HD..., trả hàng TH..., khách hàng KH...). Mã sản phẩm và
    nhà cung cấp do frontend đặt theo quy ước PRD-<epoch ms> / SUP-<epoch ms> — script dùng đúng quy ước đó.
  * Master data tạo ở HQ bằng tài khoản admin; replication đưa xuống chi nhánh.
    - Sản phẩm, nhà cung cấp, khách hàng: qua REST API của HQ (như màn hình quản trị).
    - Danh mục: có sẵn trong DB (db/migration-hq/R__reference_data.sql, cùng cây danh mục với web-public),
      script chỉ đọc qua API để gắn sản phẩm vào danh mục con.
    - Bảng giá, ngưỡng cảnh báo tồn: chưa có API ghi -> SQL tại HQ (psql trong container hq-db).
  * Giao dịch tạo ở chi nhánh bằng tài khoản staff qua REST API (gateway nginx của chi nhánh), kèm
    header Idempotency-Key như frontend: phiếu nhập (rồi xác nhận), hóa đơn bán, phiếu trả hàng (rồi xác nhận).
    Tồn kho, lô giá vốn FIFO, công nợ và sổ cái công nợ do backend tự tính.

Chỉ dùng thư viện chuẩn Python 3.8+. Chạy được trên Windows (python) và Linux/Git Bash (python3).
Từ chối chạy nếu HQ đã có sản phẩm (tránh nạp trùng) — dùng --force để bỏ qua kiểm tra.
"""
from __future__ import annotations

import argparse
import json
import os
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent


# --------------------------------------------------------------------------- dữ liệu demo

# Danh mục KHÔNG tạo ở đây: cây danh mục (giống website apps/web-public) có sẵn trong
# db/migration-hq/R__reference_data.sql. Script đọc danh mục qua GET /api/v1/catalog/categories
# và gắn sản phẩm vào danh mục CON theo code (slug của website).

# key nội bộ -> (tên, code danh mục con, đơn vị, giá TP1, giá TP2, giá nhập tham khảo)
# Tên và giá bán TP1 lấy theo sản phẩm mẫu của website (salePrice); các danh mục website chưa có
# sản phẩm mẫu được bổ sung sản phẩm tên chung.
PRODUCTS = {
    "toto_tc384":   ("Bồn cầu 1 khối TOTO CW166RB/TC384CVK",   "ban-cau",            "Bộ",  27941200, 28500000, 22000000),
    "toto_tcf333":  ("Bồn cầu 1 khối TOTO CW166RB/TCF33320GAA", "bon-cau-1-khoi",     "Bộ",  44787350, 45500000, 36000000),
    "toto_tcf343":  ("Bồn cầu 1 khối TOTO CW166RB/TCF34320GAA", "bon-cau-1-khoi",     "Bộ",  52063350, 53000000, 42000000),
    "bc2k":         ("Bồn cầu 2 khối men chống bám",           "bon-cau-2-khoi",     "Bộ",  4200000,  4300000,  3300000),
    "bctm":         ("Bồn cầu thông minh nắp rửa điện tử",     "bon-cau-thong-minh", "Bộ",  38500000, 39000000, 31000000),
    "lt4706":       ("Chậu rửa đặt bàn TOTO LT4706",           "chau-rua",           "Cái", 4000000,  4100000,  3100000),
    "lt548":        ("Chậu rửa âm bàn TOTO LT548",             "chau-rua",           "Cái", 1800000,  1850000,  1350000),
    "den_chum":     ("Đèn chùm pha lê cao cấp Luxury L500",    "den-trang-tri",      "Bộ",  4800000,  4900000,  3700000),
    "rem":          ("Rèm vải chống nắng châu Âu cao cấp",     "rem-cua",            "Bộ",  950000,   990000,   700000),
    "sofa":         ("Sofa da thật Italy hiện đại",            "sofa",               "Bộ",  35000000, 35500000, 28000000),
    "ban_tra":      ("Bàn trà mặt đá cẩm thạch",               "ban-tra",            "Cái", 3800000,  3900000,  2900000),
    "binh_hoa":     ("Bình hoa pha lê châu Âu nghệ thuật",     "do-trang-tri",       "Cái", 750000,   790000,   520000),
}

# key -> payload SupplierCreateDto (code sinh theo quy ước SUP-<epoch ms>). Tên/thông tin là dữ liệu giả lập.
SUPPLIERS = {
    "tbvs":    {"name": "Nhà phân phối Thiết bị Vệ sinh Minh Phát", "phone": "0283 8123 456", "email": "kinhdoanh@minhphat-demo.vn",
                "address": "KCN Tân Tạo, Bình Tân, TP.HCM",         "taxCode": "0312345671"},
    "tbvs2":   {"name": "Công ty TNHH Thiết bị Vệ sinh Phú Thịnh",  "phone": "0274 3812 999", "email": "banhang@phuthinh-demo.vn",
                "address": "KCN Sóng Thần, Dĩ An, Bình Dương",      "taxCode": "3701234562"},
    "den":     {"name": "Công ty CP Đèn Trang Trí Ánh Sáng",        "phone": "0243 8566 120", "email": "sales@anhsang-demo.vn",
                "address": "Thanh Xuân, Hà Nội",                    "taxCode": "0109876544"},
    "noithat": {"name": "Xưởng Nội thất Hoàng Long",                "phone": "0283 9300 456", "email": "hoanglong@noithat-demo.vn",
                "address": "Q.7, TP.HCM",                           "taxCode": "0318765435"},
    "decor":   {"name": "Đại lý Rèm & Decor Mộc An",                "phone": "0287 1088 789", "email": "order@mocan-demo.vn",
                "address": "Q.10, TP.HCM",                          "taxCode": "0315678903"},
}

# key -> (payload CustomerCreateDto, chi nhánh riêng hoặc None = dùng chung). customerCode do backend sinh.
CUSTOMERS = {
    "anphu":    ({"name": "Công ty TNHH Xây dựng An Phú",   "phone": "0901 234 567", "address": "Thủ Đức, TP.HCM",  "taxCode": "0311112223"}, None),
    "tuan":     ({"name": "Anh Tuấn - Thầu xây dựng",       "phone": "0909 876 543", "address": "Gò Vấp, TP.HCM"}, None),
    "lan":      ({"name": "Chị Lan - Khách lẻ",              "phone": "0912 345 678"}, None),
    "hoanggia": ({"name": "Công ty CP Nội thất Hoàng Gia",  "phone": "0938 111 222", "address": "Q.3, TP.HCM",      "taxCode": "0314445556"}, None),
    "minh":     ({"name": "Anh Minh - Thợ lắp đặt",          "phone": "0977 333 444"}, "TP1"),
    "quan7":    ({"name": "Công ty TNHH Nội thất Nam Sài Gòn", "phone": "0283 7700 888", "address": "Q.7, TP.HCM",   "taxCode": "0319990001"}, "TP2"),
}

# Ngưỡng tồn tối thiểu theo (chi nhánh, sản phẩm) — sau giao dịch demo, dashboard có cảnh báo tồn thấp:
# TP1: Sofa còn 3 (< 4), TOTO TCF33320 còn 3 (< 5), lavabo LT548 còn 5 (< 10). TP2: đèn chùm còn 2 (< 3).
ALERT_THRESHOLDS = {
    "TP1": {"sofa": 4, "toto_tcf333": 5, "lt548": 10},
    "TP2": {"den_chum": 3, "binh_hoa": 5},
}

# Giao dịch theo chi nhánh. Phiếu nhập: (supplier, [(product, qty, unit_cost)], ghi chú)
# Hóa đơn: (customer, [(product, qty)], "full" | "none" | số tiền trả trước, ghi chú)
# Trả hàng: (chỉ số hóa đơn trong danh sách, customer, [(product, qty)], lý do)
TRANSACTIONS = {
    "TP1": {
        "inbound": [
            ("tbvs",    [("toto_tc384", 5, 22000000), ("toto_tcf333", 6, 36000000), ("toto_tcf343", 3, 42000000)], "Nhập bồn cầu TOTO"),
            ("tbvs2",   [("bc2k", 20, 3300000), ("bctm", 4, 31000000), ("lt4706", 15, 3100000), ("lt548", 30, 1350000)], "Nhập thiết bị vệ sinh"),
            ("den",     [("den_chum", 10, 3700000)], "Nhập đèn trang trí"),
            ("noithat", [("sofa", 5, 28000000), ("ban_tra", 12, 2900000)], "Nhập nội thất phòng khách"),
            ("decor",   [("rem", 40, 700000), ("binh_hoa", 25, 520000)], "Nhập rèm và đồ trang trí"),
            ("tbvs2",   [("lt548", 20, 1400000)], "Nhập bổ sung lavabo LT548 (lô 2, giá mới)"),
        ],
        "sales": [
            ("anphu",    [("toto_tcf333", 3), ("lt4706", 6)], "full", "Giao công trình An Phú đợt 1"),
            ("tuan",     [("bc2k", 8), ("lt548", 12)], "none", "Ghi nợ"),
            ("lan",      [("binh_hoa", 2), ("den_chum", 1)], "full", None),
            ("hoanggia", [("sofa", 2), ("ban_tra", 4), ("rem", 10)], 50000000, "Trả trước 50 triệu"),
            ("anphu",    [("lt548", 35)], 20000000, "Đợt 2 — lấy qua 2 lô giá vốn FIFO"),
            ("minh",     [("toto_tc384", 1), ("bctm", 1)], "full", None),
        ],
        "returns": [
            (1, "tuan", [("lt548", 2)], "Chậu bị nứt khi vận chuyển"),
        ],
    },
    "TP2": {
        "inbound": [
            ("den",   [("den_chum", 6, 3700000)], "Nhập đầu kỳ đèn trang trí"),
            ("decor", [("binh_hoa", 20, 520000), ("rem", 15, 700000)], "Nhập đầu kỳ rèm và đồ trang trí"),
        ],
        "sales": [
            ("quan7", [("den_chum", 4), ("rem", 6)], 10000000, "Công trình Q.7"),
            ("lan",   [("binh_hoa", 3)], "full", None),
        ],
        "returns": [],
    },
}


# --------------------------------------------------------------------------- tiện ích

def load_env() -> dict:
    """Đọc .env của docker-compose (nếu có) để lấy POSTGRES_USER."""
    env = {}
    path = PROJECT_DIR / ".env"
    if path.exists():
        for raw in path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip().strip('"').strip("'")
    return env


class ApiError(RuntimeError):
    pass


class Api:
    """Client REST tối giản (urllib) theo chuẩn ApiResponse {success, data, message}."""

    def __init__(self, base_url: str, label: str):
        self.base = base_url.rstrip("/")
        self.label = label
        self.token = None

    def call(self, method: str, path: str, body=None, idempotent: bool = False):
        data = json.dumps(body).encode("utf-8") if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method)
        req.add_header("Content-Type", "application/json")
        req.add_header("Accept", "application/json")
        if self.token:
            req.add_header("Authorization", "Bearer " + self.token)
        if idempotent:
            req.add_header("Idempotency-Key", str(uuid.uuid4()))
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                payload = json.loads(resp.read().decode("utf-8") or "{}")
        except urllib.error.HTTPError as e:
            text = e.read().decode("utf-8", errors="replace")
            raise ApiError(f"[{self.label}] {method} {path} -> HTTP {e.code}: {text[:500]}") from None
        except urllib.error.URLError as e:
            raise ApiError(f"[{self.label}] {method} {path} -> không kết nối được {self.base}: {e.reason}") from None
        except OSError as e:  # ConnectionResetError, timeout...: app chưa sẵn sàng hoặc bị dừng giữa chừng
            raise ApiError(f"[{self.label}] {method} {path} -> lỗi kết nối {self.base}: {e}") from None
        if isinstance(payload, dict) and payload.get("success") is False:
            raise ApiError(f"[{self.label}] {method} {path} -> {payload.get('message')} {payload.get('errors') or ''}")
        return payload.get("data") if isinstance(payload, dict) else payload

    def login(self, username: str, password: str):
        data = self.call("POST", "/api/v1/auth/login", {"username": username, "password": password})
        self.token = data["accessToken"]
        return data


class HqSql:
    """Chạy SQL tại DB HQ bằng psql (mặc định trong container hq-db qua docker compose)."""

    def __init__(self, command: str):
        self.cmd = shlex.split(command)

    def run(self, sql: str) -> list[list[str]]:
        proc = subprocess.run(
            self.cmd + ["-v", "ON_ERROR_STOP=1", "-X", "-q", "-At", "-F", "\t", "-c", sql],
            cwd=PROJECT_DIR, capture_output=True, text=True, encoding="utf-8",
            env={**os.environ, "MSYS_NO_PATHCONV": "1"},
        )
        if proc.returncode != 0:
            raise RuntimeError(f"SQL lỗi tại HQ: {proc.stderr.strip()}\nSQL: {sql[:300]}")
        return [line.split("\t") for line in proc.stdout.strip().splitlines() if line.strip()]

    def value(self, sql: str) -> str | None:
        rows = self.run(sql)
        return rows[0][0] if rows else None


def lit(s: str) -> str:
    """Literal chuỗi SQL an toàn (dữ liệu demo cố định, vẫn escape dấu nháy)."""
    return "'" + s.replace("'", "''") + "'"


def wait_until(label: str, predicate, timeout: int = 120, interval: float = 2.0):
    start = time.time()
    while True:
        try:
            if predicate():
                return
        except ApiError:
            pass
        if time.time() - start > timeout:
            raise SystemExit(f"Hết thời gian chờ: {label}. Kiểm tra replication: scripts/branch.sh status <chi nhánh>")
        time.sleep(interval)


_last_ms = 0


def frontend_code(prefix: str) -> str:
    """Mã theo quy ước frontend: <PREFIX>-<epoch ms> (đảm bảo không trùng khi tạo liên tiếp)."""
    global _last_ms
    ms = max(int(time.time() * 1000), _last_ms + 1)
    _last_ms = ms
    return f"{prefix}-{ms}"


# --------------------------------------------------------------------------- các bước

def seed_master(hq: Api, sql: HqSql) -> dict:
    print("==> Master data tại HQ")
    # Danh mục có sẵn (R__reference_data.sql) — đọc qua API như màn hình tạo sản phẩm.
    cat_ids = {c["code"]: c["id"] for c in hq.call("GET", "/api/v1/catalog/categories") or []}
    missing = sorted({cat for (_n, cat, *_r) in PRODUCTS.values()} - set(cat_ids))
    if missing:
        raise SystemExit(f"HQ thiếu danh mục {missing}. Danh mục được tạo bởi db/migration-hq/R__reference_data.sql "
                         "khi hq-app khởi động — kiểm tra hq-app đã chạy bản mới nhất.")
    print(f"    danh mục có sẵn: {len(cat_ids)} (dùng {len({c for (_n, c, *_r) in PRODUCTS.values()})} danh mục con)")

    products = {}
    for key, (name, cat, unit, *_rest) in PRODUCTS.items():
        dto = hq.call("POST", "/api/v1/catalog/products", {
            "code": frontend_code("PRD"), "name": name, "categoryId": cat_ids[cat],
            "baseUnit": unit, "isActive": True,
        })
        products[key] = {"id": dto["id"], "code": dto["code"], "name": name, "unit": unit}
    print(f"    sản phẩm: {len(products)} (API)")

    branch_ids = {row[0]: int(row[1]) for row in sql.run("SELECT code, id FROM branch")}
    price_rows = []
    for key, (_n, _c, _u, p_tp1, p_tp2, _cost) in PRODUCTS.items():
        for code, price in (("TP1", p_tp1), ("TP2", p_tp2)):
            if code in branch_ids:
                price_rows.append(f"({products[key]['id']}, {branch_ids[code]}, {price}, now(), now())")
    # Mỗi (sản phẩm, chi nhánh) đúng 1 giá: ProductReader gom giá bằng Collectors.toMap.
    sql.run("INSERT INTO price_list (product_id, branch_id, price, effective_date, created_at) VALUES " + ", ".join(price_rows))
    print(f"    bảng giá: {len(price_rows)} dòng")

    alert_rows = []
    for code, rules in ALERT_THRESHOLDS.items():
        if code in branch_ids:
            for key, threshold in rules.items():
                alert_rows.append(f"({products[key]['id']}, {branch_ids[code]}, {threshold}, true, now(), now())")
    if alert_rows:
        sql.run("INSERT INTO inventory_alert_config (product_id, branch_id, min_quantity_threshold, is_active, created_at, updated_at) "
                "VALUES " + ", ".join(alert_rows) + " ON CONFLICT (product_id, branch_id) DO NOTHING")
    print(f"    ngưỡng cảnh báo tồn: {len(alert_rows)}")

    suppliers = {}
    for key, payload in SUPPLIERS.items():
        dto = hq.call("POST", "/api/v1/suppliers", {"code": frontend_code("SUP"), **payload})
        suppliers[key] = dto["id"]
    print(f"    nhà cung cấp: {len(suppliers)} (API)")

    customers = {}
    for key, (payload, branch_code) in CUSTOMERS.items():
        body = dict(payload)
        if branch_code:
            if branch_code not in branch_ids:
                continue
            body["branchId"] = branch_ids[branch_code]
        dto = hq.call("POST", "/api/v1/customers", body)
        customers[key] = {"id": dto["id"], "branch": branch_code}
    print(f"    khách hàng: {len(customers)} (API, mã do backend sinh)")
    return {"products": products, "suppliers": suppliers, "customers": customers, "branch_ids": branch_ids}


def load_master(sql: HqSql) -> dict:
    """Đọc lại master data demo đã nạp trước đó tại HQ (dùng với --transactions-only).

    Tra theo TÊN trong bộ dữ liệu demo ở trên — id/mã lấy từ DB, không suy đoán.
    """
    print("==> Đọc master data demo đã có tại HQ")
    by_name = {row[0]: row for row in sql.run("SELECT name, id, code, base_unit FROM product")}
    products = {}
    for key, (name, *_rest) in PRODUCTS.items():
        if name not in by_name:
            raise SystemExit(f"HQ không có sản phẩm demo '{name}' — chạy seed đầy đủ (không có --transactions-only) trước.")
        _n, pid, code, unit = by_name[name]
        products[key] = {"id": int(pid), "code": code, "name": name, "unit": unit}
    sup_by_name = {row[0]: row[1] for row in sql.run("SELECT name, id FROM supplier")}
    suppliers = {key: sup_by_name[p["name"]] for key, p in SUPPLIERS.items() if p["name"] in sup_by_name}
    branch_ids = {row[0]: int(row[1]) for row in sql.run("SELECT code, id FROM branch")}
    code_by_id = {v: k for k, v in branch_ids.items()}
    cust_rows = {row[0]: row for row in sql.run("SELECT name, id, coalesce(branch_id::text, '') FROM customer")}
    customers = {}
    for key, (payload, _branch) in CUSTOMERS.items():
        row = cust_rows.get(payload["name"])
        if row:
            customers[key] = {"id": row[1], "branch": code_by_id.get(int(row[2])) if row[2] else None}
    return {"products": products, "suppliers": suppliers, "customers": customers, "branch_ids": branch_ids}


def wait_branch_master(staff: Api, code: str, master: dict):
    print(f"==> Chờ replication master data xuống {code}")
    want_products = {p["id"] for p in master["products"].values()}
    want_suppliers = set(master["suppliers"].values())
    want_customers = {c["id"] for c in master["customers"].values() if c["branch"] in (None, code)}

    def ready():
        prods = staff.call("GET", "/api/v1/catalog/products?withBranchPrice=true") or []
        priced = {p["id"] for p in prods if p.get("price") is not None}
        sups = {s["id"] for s in (staff.call("GET", "/api/v1/suppliers") or [])}
        custs = {c["id"] for c in (staff.call("GET", "/api/v1/customers") or [])}
        return want_products <= priced and want_suppliers <= sups and want_customers <= custs

    wait_until(f"master data ở {code}", ready)
    print(f"    {code} thấy đủ {len(want_products)} sản phẩm (có giá), {len(want_suppliers)} NCC, {len(want_customers)} khách hàng")


def seed_transactions(staff: Api, code: str, master: dict) -> dict:
    plan = TRANSACTIONS.get(code)
    if not plan:
        return {}
    products, suppliers, customers = master["products"], master["suppliers"], master["customers"]
    prices = {key: (PRODUCTS[key][3] if code == "TP1" else PRODUCTS[key][4]) for key in PRODUCTS}
    print(f"==> Giao dịch tại {code}")

    for sup_key, lines, note in plan["inbound"]:
        created = staff.call("POST", "/api/v1/inventory/inbound", {
            "supplierId": suppliers[sup_key], "note": note,
            "lines": [{"productId": products[p]["id"], "quantity": q, "unitCost": cost,
                       "unitOfMeasure": products[p]["unit"]} for p, q, cost in lines],
        }, idempotent=True)
        staff.call("POST", f"/api/v1/inventory/inbound/{created['id']}/confirm", idempotent=True)
    print(f"    phiếu nhập (đã xác nhận): {len(plan['inbound'])}")

    invoices = []
    for cust_key, lines, advance, note in plan["sales"]:
        body_lines = [{"productId": products[p]["id"], "productName": products[p]["name"], "quantity": q,
                       "unitPrice": prices[p], "unitOfMeasure": products[p]["unit"]} for p, q in lines]
        total = sum(prices[p] * q for p, q in lines)
        paid = total if advance == "full" else 0 if advance == "none" else advance
        created = staff.call("POST", "/api/v1/sales-invoices", {
            "customerId": customers[cust_key]["id"], "paymentMethod": "CASH",
            "advancePayment": paid, "note": note, "lines": body_lines,
        }, idempotent=True)
        invoices.append(created)
    print(f"    hóa đơn bán (tạo + xác nhận): {len(invoices)}")

    for inv_index, cust_key, lines, reason in plan["returns"]:
        return_id = staff.call("POST", "/api/v1/goods-returns", {
            "customerId": customers[cust_key]["id"], "invoiceId": invoices[inv_index]["id"], "reason": reason,
            "lines": [{"productId": products[p]["id"], "quantity": q, "unitPrice": prices[p],
                       "unitOfMeasure": products[p]["unit"]} for p, q in lines],
        }, idempotent=True)
        staff.call("POST", f"/api/v1/goods-returns/{return_id}/confirm", idempotent=True)
    print(f"    phiếu trả hàng (đã xác nhận): {len(plan['returns'])}")
    return {"invoices": len(invoices)}


def wait_hq_transactions(sql: HqSql, code: str, branch_id: int, expected_invoices: int):
    print(f"==> Chờ giao dịch {code} replicate lên HQ")
    wait_until(f"hóa đơn {code} ở HQ",
               lambda: int(sql.value(f"SELECT count(*) FROM sales_invoice WHERE branch_id = {branch_id}") or 0) >= expected_invoices)
    row = sql.run(
        f"SELECT (SELECT count(*) FROM sales_invoice WHERE branch_id = {branch_id}), "
        f"(SELECT count(*) FROM stock_movement WHERE branch_id = {branch_id}), "
        f"(SELECT count(*) FROM stock_on_hand WHERE branch_id = {branch_id}), "
        f"(SELECT count(*) FROM receivable_debt_movement WHERE branch_id = {branch_id}), "
        f"(SELECT coalesce(sum(total_debt), 0) FROM receivable_debt WHERE branch_id = {branch_id})")[0]
    print(f"    HQ đã có: {row[0]} hóa đơn, {row[1]} dòng sổ kho, {row[2]} dòng tồn, "
          f"{row[3]} dòng sổ công nợ, tổng nợ {float(row[4]):,.0f} đ")


# --------------------------------------------------------------------------- main

def main():
    # Git Bash/PowerShell trên Windows có thể dùng code page không phải UTF-8 -> tránh lỗi in tiếng Việt.
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8")
        except (AttributeError, ValueError):
            pass
    env = load_env()
    pg_user = env.get("POSTGRES_USER", "erp_user")
    ap = argparse.ArgumentParser(description="Nạp dữ liệu demo qua API (HQ + chi nhánh).")
    ap.add_argument("--branches", default="tp1", help="Danh sách chi nhánh tạo giao dịch, vd. tp1,tp2")
    ap.add_argument("--hq-url", default="http://localhost", help="Gateway HQ (mặc định http://localhost)")
    ap.add_argument("--branch-url", action="append", default=[],
                    help="Gateway chi nhánh dạng tp1=http://localhost:81 (mặc định TP<n> -> cổng 80+n)")
    ap.add_argument("--admin", default="admin")
    ap.add_argument("--password", default="password", help="Mật khẩu chung của admin và staff_<chi nhánh>")
    ap.add_argument("--hq-psql", default=f"docker compose exec -T hq-db psql -U {pg_user} -d erp_hq",
                    help="Lệnh psql tới DB HQ (dùng cho danh mục, bảng giá, ngưỡng cảnh báo)")
    ap.add_argument("--force", action="store_true", help="Bỏ qua kiểm tra 'HQ đã có sản phẩm'")
    ap.add_argument("--transactions-only", action="store_true",
                    help="Không tạo master data; dùng master demo đã có ở HQ và chỉ tạo giao dịch cho --branches "
                         "(vd. bật TP2 sau: --branches tp2 --transactions-only)")
    args = ap.parse_args()

    branches = [b.strip().upper() for b in args.branches.split(",") if b.strip()]
    urls = {}
    for item in args.branch_url:
        k, v = item.split("=", 1)
        urls[k.strip().upper()] = v.strip()
    for b in branches:
        urls.setdefault(b, f"http://localhost:{80 + int(b[2:])}" if b[2:].isdigit() else None)

    sql = HqSql(args.hq_psql)
    if args.transactions_only:
        master = load_master(sql)
    else:
        existing = int(sql.value("SELECT count(*) FROM product") or 0)
        if existing and not args.force:
            raise SystemExit(f"HQ đã có {existing} sản phẩm — dữ liệu demo có thể đã được nạp. "
                             "Dùng --transactions-only để chỉ tạo giao dịch, hoặc --force để nạp thêm master data.")
        hq = Api(args.hq_url, "HQ")
        hq.login(args.admin, args.password)
        master = seed_master(hq, sql)

    for code in branches:
        if code not in master["branch_ids"]:
            print(f"!! Bỏ qua {code}: HQ không có chi nhánh này")
            continue
        staff = Api(urls[code], code)
        # JWT luôn do HQ cấp (gateway chi nhánh cũng chuyển /api/v1/auth về HQ) -> đăng nhập qua HQ.
        staff.token = Api(args.hq_url, "HQ").login(f"staff_{code.lower()}", args.password)["accessToken"]
        wait_branch_master(staff, code, master)
        result = seed_transactions(staff, code, master)
        if result:
            wait_hq_transactions(sql, code, master["branch_ids"][code], result["invoices"])

    print("==> Xong dữ liệu demo")


if __name__ == "__main__":
    try:
        main()
    except ApiError as e:
        sys.exit(f"LỖI API: {e}")
