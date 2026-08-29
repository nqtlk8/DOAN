# System Architecture — v5

## 1. Phạm vi

Hệ thống là ERP cho cửa hàng vật liệu xây dựng và thiết bị thông minh nhà, vận hành theo mô hình **1 HQ + N Branch**. Backend sử dụng một codebase Java/Spring Boot và được triển khai thành nhiều instance.

Trong phiên bản hiện tại, backend tập trung vào các miền:

- Identity
- Branch
- Catalog
- CRM
- Sales / Goods Return
- Inventory / Inbound
- Analytics
- Common / Security / AOP

Các chức năng Procurement, Customer Order, Stock Transfer, Outbound Receipt và RFQ không thuộc implementation hiện hành.

## 2. Nguyên tắc kiến trúc

### 2.1. Modular Monolith

Các module nghiệp vụ nằm trong cùng một Spring Boot application. Module được tách theo trách nhiệm nhưng không được triển khai thành các microservice độc lập.

### 2.2. N-instance deployment

Cùng một backend artifact được triển khai thành:

- 01 HQ instance.
- N Branch instances.

Mỗi Branch có database PostgreSQL riêng.

### 2.3. HQ và Branch

**HQ**:
- phát hành và refresh JWT;
- quản lý master data;
- quản lý branch;
- dashboard và analytics;
- giữ Redis;
- giữ private key để ký JWT.

**Branch**:
- xử lý nghiệp vụ cục bộ;
- truy cập PostgreSQL của chính branch;
- xác minh JWT bằng public key;
- không có private key;
- không chạy Redis theo cấu hình hiện tại.

## 3. Runtime topology hiện tại

```text
                          Client Browser
                               |
                               v
                    +----------------------+
                    |   erp-frontend       |
                    |   Nginx :80          |
                    +----------+-----------+
                               |
                         /api/* proxy
                               |
                               v
                    +----------------------+
                    |   hq-app              |
                    |   Spring Boot :8080   |
                    +----+-------------+----+
                         |             |
                         v             v
                 +-----------+    +---------+
                 | hq-db     |    | redis-hq|
                 | :5432     |    | :6379   |
                 +-----------+    +---------+

Branch TP1:
Client -> branch-tp1-nginx :81 -> branch-tp1-app :8080 -> branch-tp1-db :5432

Branch TP2:
Client -> branch-tp2-nginx :82 -> branch-tp2-app :8080 -> branch-tp2-db :5432
```

## 4. Docker port mapping

| Service | Host port | Container port | Vai trò |
|---|---:|---:|---|
| `erp-frontend` | 80 | 80 | ERP UI + reverse proxy API |
| `hq-app` | 8080 | 8080 | HQ Spring Boot |
| `branch-tp1-app` | 8081 | 8080 | Branch TP1 Spring Boot |
| `branch-tp2-app` | 8082 | 8080 | Branch TP2 Spring Boot |
| `branch-tp1-nginx` | 81 | 80 | Reverse proxy Branch TP1 |
| `branch-tp2-nginx` | 82 | 80 | Reverse proxy Branch TP2 |
| `hq-db` | 5432 | 5432 | PostgreSQL HQ |
| `branch-tp1-db` | 5433 | 5432 | PostgreSQL TP1 |
| `branch-tp2-db` | 5434 | 5432 | PostgreSQL TP2 |
| `redis-hq` | 6379 | 6379 | Redis HQ |
| `web-public` | 3000 | 3000 | Public web; không thuộc phạm vi backend docs chi tiết |

## 5. Container-to-container communication

Container giao tiếp bằng service name trên Docker network, không dùng `localhost` cho kết nối nội bộ.

Ví dụ:

```text
hq-app -> jdbc:postgresql://hq-db:5432/erp_hq
hq-app -> redis-hq:6379
branch-tp1-app -> jdbc:postgresql://branch-tp1-db:5432/erp_branch_tp1
```

Host port chỉ phục vụ truy cập từ máy phát triển hoặc mạng bên ngoài Docker.

## 6. Request path

Request từ browser đến backend đi theo:

```text
Browser
  -> erp-frontend Nginx
  -> /api/* proxy
  -> hq-app hoặc branch-app
  -> Spring Security Filter Chain
  -> Controller
  -> Application Service / Repository
  -> Hibernate / JPA
  -> PostgreSQL
```

Với endpoint protected, JWT được xác minh trước khi request đi vào lớp nghiệp vụ.

## 7. Frontend boundary

`erp-frontend` hiện là một ứng dụng React/Vite được Nginx phục vụ. Backend không phụ thuộc vào framework frontend; contract giữa hai bên là HTTP/JSON API.

Không mô tả chi tiết frontend trong tài liệu backend.
