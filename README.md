# Hệ thống ERP – Quản lý chuỗi cửa hàng thiết bị vệ sinh & nội thất

Hệ thống quản lý tổng hợp (ERP) cho mô hình **Trụ sở (HQ) + nhiều Chi nhánh**, sử dụng **PostgreSQL Logical Replication** để đồng bộ dữ liệu hai chiều giữa HQ và các chi nhánh.

---

## Mục lục

- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Hướng dẫn chạy dự án](#hướng-dẫn-chạy-dự-án)
- [Tài khoản đăng nhập](#tài-khoản-đăng-nhập)
- [Truy cập hệ thống](#truy-cập-hệ-thống)
- [Các script tiện ích](#các-script-tiện-ích)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)

---

## Kiến trúc tổng quan

```
                    ┌─────────────────────┐
                    │   erp-frontend      │  :80  (Admin HQ)
                    │   (React + Vite)    │
                    └────────┬────────────┘
                             │
                    ┌────────▼────────────┐
                    │   hq-app            │  :8080 (Spring Boot, profile=hq)
                    │   + Redis           │
                    └────────┬────────────┘
                             │
                    ┌────────▼────────────┐
                    │   hq-db             │  :5432 (PostgreSQL 15)
                    │   erp_hq            │
                    └────────┬────────────┘
                             │  Logical Replication ↕
                    ┌────────▼────────────┐
                    │  branch-tp1-db      │  :5433
                    │  erp_branch_tp1     │
                    └────────┬────────────┘
                             │
                    ┌────────▼────────────┐
                    │  branch-tp1-app     │  (Spring Boot, profile=branch)
                    └────────┬────────────┘
                             │
                    ┌────────▼────────────┐
                    │  branch-tp1-nginx   │  :81  (Staff TP1)
                    │  (Frontend + Proxy) │
                    └─────────────────────┘
```

- **Master data** (sản phẩm, bảng giá, NCC, khách hàng…) quản lý tại HQ, replicate xuống chi nhánh.
- **Giao dịch** (phiếu nhập, hóa đơn bán, phiếu trả hàng…) tạo tại chi nhánh, replicate lên HQ.
- Chi nhánh TP2 là profile tùy chọn, bật/tắt bằng script.

---

## Cấu trúc thư mục

```
DOAN/
└── code/                          # ← Toàn bộ source code
    ├── docker-compose.yml         # Định nghĩa toàn bộ services (HQ + chi nhánh)
    ├── .env.example               # Mẫu biến môi trường (copy thành .env)
    ├── nginx-branch-tp1.conf      # Nginx config gateway chi nhánh TP1
    ├── nginx-branch-tp2.conf      # Nginx config gateway chi nhánh TP2
    ├── nginx-hq.conf              # Nginx config HQ
    │
    ├── docker/
    │   └── postgres/init/         # Script khởi tạo role DB (chạy khi volume mới)
    │
    ├── secrets/                   # JWT keys (private_key.pem, public_key.pem)
    │
    ├── scripts/                   # Các script vận hành (xem mục bên dưới)
    │
    ├── docs/                      # Script build báo cáo
    │
    └── erp-platform/              # Monorepo Node.js + Java
        ├── package.json           # Workspace root
        │
        ├── apps/
        │   ├── erp-frontend/      # 🖥️ FRONTEND – React 18 + Vite + Redux Toolkit
        │   │   └── src/
        │   │       ├── components/    # UI theo module (admin, auth, catalog, inventory, sales, crm)
        │   │       ├── api/           # API client (fetch wrapper)
        │   │       ├── state/         # Redux slices
        │   │       ├── hooks/         # Custom hooks
        │   │       └── shared/        # Components & utilities dùng chung
        │   │
        │   └── web-public/        # 🌐 Website công khai (Next.js) – port 3000
        │
        ├── packages/
        │   └── api-contract/      # 📋 TypeScript types & API contract dùng chung
        │
        └── services/
            └── erp-backend/       # ⚙️ BACKEND – Spring Boot 3 + Java 17 + Maven
                ├── src/main/java/com/storename/erp/
                │   ├── analytics/     # Dashboard, báo cáo, thống kê
                │   ├── branch/        # Quản lý chi nhánh
                │   ├── catalog/       # Sản phẩm, danh mục, bảng giá
                │   ├── common/        # Shared (config, security, JWT, exceptions)
                │   ├── crm/           # Khách hàng, nhà cung cấp
                │   ├── identity/      # Đăng nhập, phân quyền, user
                │   ├── inventory/     # Kho, phiếu nhập, tồn kho, cảnh báo
                │   └── order/         # Hóa đơn bán, phiếu trả, công nợ, phiếu thu
                └── src/main/resources/
                    └── db/            # Flyway migrations (migration-hq, migration-branch)
```

---

## Yêu cầu hệ thống

| Phần mềm        | Phiên bản tối thiểu |
|------------------|---------------------|
| Docker           | 20.10+              |
| Docker Compose   | v2.x                |
| Git Bash / WSL   | (Windows cần shell bash để chạy script) |
| Python 3.8+      | (chỉ cần nếu chạy seed-demo)           |

---

## Hướng dẫn chạy dự án

### 1. Clone repository

```bash
git clone https://github.com/nqtlk8/DOAN.git
cd DOAN/code
```

### 2. Tạo file môi trường

```bash
cp .env.example .env
```

> Có thể giữ nguyên giá trị mặc định trong `.env.example`. Chỉ cần đổi nếu muốn tùy chỉnh mật khẩu DB.

### 3. Tạo JWT keys (nếu chưa có)

```bash
bash scripts/generate-jwt-keys.sh
```

### 4. Khởi động toàn bộ hệ thống

**Cách nhanh nhất** – dùng script bootstrap (build + khởi tạo DB + thiết lập replication):

```bash
# HQ + chi nhánh TP1 (mặc định)
bash scripts/bootstrap.sh

# HQ + TP1 + TP2, kèm dữ liệu demo
bash scripts/bootstrap.sh tp2 --seed
```

**Hoặc chạy thủ công từng bước:**

```bash
# Bước 1: Build và khởi động containers
docker compose up -d --build

# Bước 2: Chờ Flyway migrate xong (xem log)
docker compose logs -f hq-app branch-tp1-app

# Bước 3: Thiết lập replication HQ ↔ TP1
bash scripts/setup-replication.sh tp1

# Bước 4 (tuỳ chọn): Nạp dữ liệu demo
python scripts/seed-demo.py
```

### 5. Dừng hệ thống

```bash
# Dừng tất cả
docker compose down

# Dừng và XOÁ toàn bộ dữ liệu (làm lại từ đầu)
docker compose --profile tp2 down -v
```

---

## Tài khoản đăng nhập

Sau khi hệ thống khởi động, Flyway tự tạo các tài khoản mặc định:

| Tài khoản       | Mật khẩu   | Vai trò               | Đăng nhập tại         |
|------------------|------------|------------------------|------------------------|
| `admin`          | `password` | Quản trị viên HQ       | http://localhost (HQ)  |
| `staff_tp1`      | `password` | Nhân viên chi nhánh TP1| http://localhost:81    |
| `staff_tp2`      | `password` | Nhân viên chi nhánh TP2| http://localhost:82    |

> **Lưu ý:** `staff_tp1` đăng nhập qua gateway chi nhánh TP1 (port 81). Xác thực JWT do HQ cấp, gateway tự chuyển request `/api/v1/auth` về HQ.

---

## Truy cập hệ thống

| Dịch vụ                  | URL                         | Mô tả                              |
|---------------------------|-----------------------------|--------------------------------------|
| **Frontend HQ (Admin)**   | http://localhost             | Quản lý master data, xem dashboard  |
| **Frontend TP1 (Staff)**  | http://localhost:81          | Tạo phiếu nhập, bán hàng, trả hàng |
| **Frontend TP2 (Staff)**  | http://localhost:82          | Chi nhánh TP2 (cần bật profile tp2) |
| **Web công khai**         | http://localhost:3000        | Website giới thiệu (Next.js)        |
| **API HQ**                | http://localhost:8080/api/v1 | REST API backend HQ                 |
| **DB HQ (Postgres)**      | localhost:5432               | Database `erp_hq`                   |
| **DB TP1 (Postgres)**     | localhost:5433               | Database `erp_branch_tp1`           |
| **DB TP2 (Postgres)**     | localhost:5434               | Database `erp_branch_tp2`           |

---

## Các script tiện ích

Tất cả nằm trong `code/scripts/`. Chạy từ thư mục `code/`:

| Script                      | Công dụng                                                            |
|-----------------------------|----------------------------------------------------------------------|
| `bootstrap.sh`              | Khởi tạo toàn bộ hệ thống từ đầu (build, migrate, replication)      |
| `setup-replication.sh <cn>` | Thiết lập logical replication HQ ↔ chi nhánh                        |
| `branch.sh on\|off\|status <cn>` | Bật/tắt/kiểm tra trạng thái một chi nhánh (VD: `branch.sh on tp2`) |
| `add-branch.sh <cn>`        | Thêm chi nhánh mới vào hệ thống                                    |
| `remove-branch.sh <cn>`     | Gỡ bỏ chi nhánh (xoá subscription, slot)                           |
| `generate-jwt-keys.sh`      | Tạo cặp RSA key cho JWT                                             |
| `seed-demo.py`              | Nạp dữ liệu giao dịch demo qua API (cần Python 3.8+)               |
| `generate_api_contract.py`  | Sinh TypeScript types từ OpenAPI spec                                |
| `check-schema-version.sh`   | Kiểm tra version Flyway giữa HQ và chi nhánh                       |

---

## Công nghệ sử dụng

| Layer       | Công nghệ                                               |
|-------------|----------------------------------------------------------|
| **Backend** | Java 17, Spring Boot 3, Maven, Flyway, JWT (RSA)         |
| **Frontend**| React 18, TypeScript, Vite, Redux Toolkit, Recharts      |
| **Database**| PostgreSQL 15 (Logical Replication), Redis                |
| **Infra**   | Docker, Docker Compose, Nginx (reverse proxy + gateway)  |
| **Web**     | Next.js (website công khai)                              |
| **Testing** | JUnit 5, Testcontainers (backend), Vitest (frontend)     |
