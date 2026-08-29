# Database Replication — v5

## 1. Mục tiêu

Replication phục vụ mô hình nhiều database của kiến trúc HQ/Branch. HQ và mỗi Branch giữ dữ liệu cục bộ, sau đó trao đổi dữ liệu thông qua PostgreSQL Logical Replication.

## 2. Vai trò

```text
HQ PostgreSQL
     |
     | logical replication
     v
Branch PostgreSQL
```

Trong thiết kế nghiệp vụ, HQ là nguồn quản lý master data; Branch là nơi phát sinh giao dịch cục bộ.

## 3. PostgreSQL configuration

HQ trong Docker Compose được khởi động với:

```text
wal_level=logical
max_replication_slots=10
max_wal_senders=10
```

Branch được khởi động với:

```text
wal_level=logical
```

## 4. Important implementation fact

`docker-compose.yml` hiện **chưa tự động tạo** `PUBLICATION` và `SUBSCRIPTION`. Compose chỉ bật các PostgreSQL containers với cấu hình cần thiết.

Replication E2E test là test thủ công (`@Disabled`) và yêu cầu tạo publication/subscription bằng SQL trước khi chạy.

Ví dụ trong test:

```sql
CREATE PUBLICATION erp_pub FOR ALL TABLES;
```

và:

```sql
CREATE SUBSCRIPTION erp_sub
CONNECTION 'host=pg-master port=5432 user=erp_user password=erp_password dbname=erp_db'
PUBLICATION erp_pub;
```

## 5. Runtime status API

Backend cung cấp:

```text
GET /api/v1/admin/system/replication-status
```

`ReplicationStatusController` kiểm tra PostgreSQL system views:

- `pg_stat_replication` — trạng thái phía master;
- `pg_stat_subscription` — trạng thái phía subscriber.

## 6. Consistency

Logical Replication là bất đồng bộ; do đó HQ và Branch có thể tồn tại replication lag. Hệ thống phải coi dữ liệu giữa các instance là eventual consistency, không phải strong consistency tức thời.

## 7. Không đồng nhất giữa tài liệu cũ và hiện tại

Các tài liệu cũ từng mô tả một bảng mẫu tên `products`. Đây chỉ là PoC lịch sử. Schema v5 dùng `product` và các bảng thực tế trong `V1__init_schema.sql`.
