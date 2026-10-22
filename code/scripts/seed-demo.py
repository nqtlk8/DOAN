#!/usr/bin/env python3
"""
seed-demo.py — Nạp GIAO DỊCH demo cho hệ thống ERP đang chạy (docker-compose), qua đúng luồng nghiệp vụ.

    python scripts/seed-demo.py                    # giao dịch cho TP1
    python scripts/seed-demo.py --branches tp1,tp2 # giao dịch cho cả TP1 và TP2

Phạm vi: CHỈ giao dịch (phiếu nhập, hóa đơn bán, phiếu trả hàng) tại chi nhánh.
Master data demo (sản phẩm, NCC, khách hàng, bảng giá, ngưỡng cảnh báo) do
erp-backend/src/main/resources/db/migration-hq/R__seed_data.sql nạp khi HQ khởi động.
Script đọc lại master đó từ DB HQ theo TÊN trong bộ dữ liệu demo — không tự đặt id.

Nguyên tắc:
  * Không tự đặt id. Mã chứng từ (HD..., TH..., PN...) do backend sinh; id do ứng dụng/sequence cấp.
  * Giá bán và đơn vị tính đọc từ DB (price_list, product.base_unit) — không hard-code.
  * Giao dịch tạo ở chi nhánh bằng tài khoản staff qua REST API (gateway nginx của chi nhánh),
    kèm header Idempotency-Key như frontend. Phiếu nhập và phiếu trả: tạo nháp rồi xác nhận.
    Tồn kho, lô giá vốn FIFO, công nợ và sổ công nợ do backend tự tính.
  * Chi nhánh là bên ghi duy nhất các bảng giao dịch (single-writer): script KHÔNG ghi các bảng đó tại HQ.

Chỉ dùng thư viện chuẩn Python 3.8+. Chạy được trên Windows (python) và Linux/Git Bash (python3).
Yêu cầu: hq-app và app chi nhánh đã chạy, replication master đã xuống chi nhánh (scripts/branch.sh status).
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


# --------------------------------------------------------------------------- khóa dữ liệu demo
# Tên phải khớp đúng R__seed_data.sql (tra theo tên để lấy id/mã/đơn vị/giá từ DB).

# key nội bộ -> tên sản phẩm
DEMO_PRODUCTS = {
    "toto_tc384":  "Bồn cầu 1 khối TOTO CW166RB/TC384CVK",
    "toto_tcf333": "Bồn cầu 1 khối TOTO CW166RB/TCF33320GAA",
    "toto_tcf343": "Bồn cầu 1 khối TOTO CW166RB/TCF34320GAA",
    "bc2k":        "Bồn cầu 2 khối men chống bám",
    "bctm":        "Bồn cầu thông minh nắp rửa điện tử",
    "lt4706":      "Chậu rửa đặt bàn TOTO LT4706",
    "lt548":       "Chậu rửa âm bàn TOTO LT548",
    "den_chum":    "Đèn chùm pha lê cao cấp Luxury L500",
    "rem":         "Rèm vải chống nắng châu Âu cao cấp",
    "sofa":        "Sofa da thật Italy hiện đại",
    "ban_tra":     "Bàn trà mặt đá cẩm thạch",
    "binh_hoa":    "Bình hoa pha lê châu Âu nghệ thuật",
}

# key nội bộ -> tên nhà cung cấp
DEMO_SUPPLIERS = {
    "tbvs":    "Nhà phân phối Thiết bị Vệ sinh Minh Phát",
    "tbvs2":   "Công ty TNHH Thiết bị Vệ sinh Phú Thịnh",
    "den":     "Công ty CP Đèn Trang Trí Ánh Sáng",
    "noithat": "Xưởng Nội thất Hoàng Long",
    "decor":   "Đại lý Rèm & Decor Mộc An",
}

# key nội bộ -> (tên khách hàng, chi nhánh riêng hoặc None = khách dùng chung)
DEMO_CUSTOMERS = {
    "anphu":    ("Công ty TNHH Xây dựng An Phú", None),
    "tuan":     ("Anh Tuấn - Thầu xây dựng", None),
    "lan":      ("Chị Lan - Khách lẻ", None),
    "hoanggia": ("Công ty CP Nội thất Hoàng Gia", None),
    "minh":     ("Anh Minh - Thợ lắp đặt", "TP1"),
    "quan7":    ("Công ty TNHH Nội thất Nam Sài Gòn", "TP2"),
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


# --------------------------------------------------------------------------- các bước

def load_master(sql: HqSql) -> dict:
    """Đọc master data demo đã được R__seed_data.sql nạp tại HQ. Tra theo TÊN — id/mã/giá lấy từ DB."""
    print("==> Đọc master data demo tại HQ (R__seed_data.sql)")
    by_name = {row[0]: row for row in sql.run("SELECT name, id, code, base_unit FROM product")}
    products = {}
    for key, name in DEMO_PRODUCTS.items():
        if name not in by_name:
            raise SystemExit(f"HQ không có sản phẩm demo '{name}'. Kiểm tra hq-app đã chạy migration R__seed_data.")
        _n, pid, code, unit = by_name[name]
        products[key] = {"id": int(pid), "code": code, "name": name, "unit": unit}

    sup_by_name = {row[0]: row[1] for row in sql.run("SELECT name, id FROM supplier")}
    suppliers = {}
    for key, name in DEMO_SUPPLIERS.items():
        if name not in sup_by_name:
            raise SystemExit(f"HQ không có nhà cung cấp demo '{name}'. Kiểm tra R__seed_data.sql.")
        suppliers[key] = sup_by_name[name]

    branch_ids = {row[0]: int(row[1]) for row in sql.run("SELECT code, id FROM branch")}
    code_by_id = {v: k for k, v in branch_ids.items()}
    cust_by_name = {row[0]: row for row in sql.run("SELECT name, id, coalesce(branch_id::text, '') FROM customer")}
    customers = {}
    for key, (name, _branch) in DEMO_CUSTOMERS.items():
        if name not in cust_by_name:
            raise SystemExit(f"HQ không có khách hàng demo '{name}'. Kiểm tra R__seed_data.sql.")
        _n, cid, bid = cust_by_name[name]
        customers[key] = {"id": cid, "branch": code_by_id.get(int(bid)) if bid else None}

    # Giá bán theo chi nhánh: prices[<mã chi nhánh>][<key sản phẩm>]
    prices: dict = {}
    name_to_key = {name: key for key, name in DEMO_PRODUCTS.items()}
    for name, branch_code, price in sql.run(
            "SELECT p.name, b.code, pl.price FROM price_list pl "
            "JOIN product p ON p.id = pl.product_id JOIN branch b ON b.id = pl.branch_id"):
        if name in name_to_key:
            prices.setdefault(branch_code, {})[name_to_key[name]] = float(price)

    return {"products": products, "suppliers": suppliers, "customers": customers,
            "branch_ids": branch_ids, "prices": prices}


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
    prices = master["prices"].get(code, {})
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
            "customerId": customers[cust_key]["id"],
            "advancePayment": paid, "note": note, "lines": body_lines,
        }, idempotent=True)
        # Hóa đơn bán hai bước: lưu nháp rồi xác nhận (trừ kho, ghi công nợ).
        staff.call("POST", f"/api/v1/sales-invoices/{created['id']}/confirm", idempotent=True)
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
    ap = argparse.ArgumentParser(description="Nạp giao dịch demo qua API chi nhánh (master data do R__seed_data.sql nạp ở HQ).")
    ap.add_argument("--branches", default="tp1", help="Danh sách chi nhánh tạo giao dịch, vd. tp1,tp2")
    ap.add_argument("--hq-url", default="http://localhost", help="Gateway HQ (mặc định http://localhost)")
    ap.add_argument("--branch-url", action="append", default=[],
                    help="Gateway chi nhánh dạng tp1=http://localhost:81 (mặc định TP<n> -> cổng 80+n)")
    ap.add_argument("--admin", default="admin")
    ap.add_argument("--password", default="password", help="Mật khẩu chung của admin và staff_<chi nhánh>")
    ap.add_argument("--hq-psql", default=f"docker compose exec -T hq-db psql -U {pg_user} -d erp_hq",
                    help="Lệnh psql tới DB HQ (đọc master demo và kiểm tra giao dịch đã lên HQ)")
    args = ap.parse_args()

    branches = [b.strip().upper() for b in args.branches.split(",") if b.strip()]
    urls = {}
    for item in args.branch_url:
        k, v = item.split("=", 1)
        urls[k.strip().upper()] = v.strip()
    for b in branches:
        urls.setdefault(b, f"http://localhost:{80 + int(b[2:])}" if b[2:].isdigit() else None)

    sql = HqSql(args.hq_psql)
    master = load_master(sql)

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

    print("==> Xong giao dịch demo")


if __name__ == "__main__":
    try:
        main()
    except ApiError as e:
        sys.exit(f"LỖI API: {e}")
