# PostgreSQL Logical Replication PoC (Hub-and-Spoke)

## Overview
Dự án sử dụng kiến trúc dữ liệu Hub-and-Spoke với PostgreSQL Logical Replication.
- **HQ Primary DB (Hub):** Chứa toàn bộ dữ liệu tập trung.
- **Branch DB (Spoke):** Chứa dữ liệu theo chi nhánh để giảm tải cho HQ và tăng tốc độ truy cập tại chi nhánh.

## Replication Setup (PoC)

Để cấu hình Replication cho 1 bảng (ví dụ: `products`), thực hiện theo các bước sau.

### 1. Trên HQ Primary DB (Port 5432)
```sql
-- Đăng nhập vào HQ DB
-- psql -h localhost -p 5432 -U erp_user -d erp_hq

-- Tạo bảng mẫu
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL
);

-- Tạo Publication cho bảng products
CREATE PUBLICATION hq_pub FOR TABLE products;
```

### 2. Trên Branch DB (Port 5433)
```sql
-- Đăng nhập vào Branch DB
-- psql -h localhost -p 5433 -U erp_user -d erp_branch_hcm01

-- Tạo cấu trúc bảng giống hệt HQ
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL
);

-- Tạo Subscription kết nối đến HQ
CREATE SUBSCRIPTION branch_hcm01_sub
CONNECTION 'host=hq-db port=5432 dbname=erp_hq user=erp_user password=erp_password'
PUBLICATION hq_pub;
```

### 3. Verify
Insert một record vào `products` trên HQ và kiểm tra xem nó đã xuất hiện trên Branch DB chưa.
