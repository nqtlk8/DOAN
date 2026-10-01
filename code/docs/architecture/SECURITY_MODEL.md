# Security Model — v5

## 1. Authentication

Hệ thống sử dụng JWT với RSA/RS256.

- HQ giữ private key để ký.
- HQ phát hành token.
- Branch chỉ giữ public key và xác minh token offline, độc lập hoàn toàn.
- **Key Externalization (Từ Sprint 4)**: Cặp khóa RSA không còn bị hardcode trong source code. Chúng được sinh ra bởi script `scripts/generate-jwt-keys.sh` và đặt trong thư mục `secrets/` (nằm ngoài codebase). `docker-compose.yml` sẽ mount thư mục này vào containers qua biến môi trường `JWT_PRIVATE_KEY_PATH` và `JWT_PUBLIC_KEY_PATH`.

**Role System**: Hệ thống chỉ sử dụng 2 role người dùng là `ADMIN` và `STAFF` (các role cũ như HQ, STORE_MANAGER, SALES_STAFF, v.v. đã bị loại bỏ).

JWT claims được dùng hiện tại:

```text
sub / subject   = username
role            = role code (ADMIN or STAFF)
branchId        = branch scope
 tokenId        = token identifier
type            = access / refresh
iat             = issued time
exp             = expiration
```

## 2. Stateless Security

`SecurityConfig` cấu hình:

```java
SessionCreationPolicy.STATELESS
```

Do đó server không duy trì session HTTP cho người dùng.

## 3. Public endpoints

SecurityConfig permitAll cho:

```text
/api/v1/auth/**
/api/v1/public/**
/api/health
/swagger-ui/**
/v3/api-docs/**
```

Các request khác yêu cầu authentication ở SecurityFilterChain.

## 4. JWT filter

`JwtAuthenticationFilter`:

- đọc `Authorization` header;
- loại bỏ prefix `Bearer `;
- verify chữ ký;
- kiểm tra `type=access`;
- tạo authority từ `role`;
- đưa authentication vào SecurityContext.

## 5. Branch Scope

Việc kiểm tra phạm vi branch được thực hiện tập trung tại `JwtAuthenticationFilter` (thay thế cho annotation `@BranchScoped` cũ).

**Cơ chế:**
Khi instance đóng vai trò là `BRANCH` (cấu hình `instance.role=BRANCH`), filter sẽ kiểm tra claim `branchId` trong JWT:
- Nếu JWT có `branchId` và khác với ID của instance (`branch-id` cấu hình trong file yaml/env), request sẽ bị chặn lập tức (HTTP 403 Forbidden).
- Nếu JWT không có `branchId` (trường hợp token của ADMIN) hoặc nếu instance là `HQ`, filter cho phép request đi tiếp. Các lớp bảo vệ tiếp theo (như `@PreAuthorize`) sẽ tự quyết định quyền truy cập nếu cần.

(Lưu ý: `@BranchScoped` và `BranchScopedAspect` hiện đã được đánh dấu `@Deprecated` và sẽ được xóa bỏ trong tương lai).

## 6. Master data protection

Thiết kế bảo vệ master data có ba lớp:

### Tầng ứng dụng (Application Level Invariants)

- **HQ Protection**: Các Controller ghi master data như `ProductWriteController`, `CategoryController` được gắn `@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")`.
- **Branch Protection**: Các Controller ghi transaction data như `SalesInvoiceController`, `GoodsReturnController`, `InboundReceiptController` được gắn `@ConditionalOnProperty(name = "instance.role", havingValue = "BRANCH")`.

Điều này đảm bảo về mặt vật lý, các API này sẽ trả về HTTP 404 (Not Found) nếu gọi sai instance (gọi Transaction API lên HQ, hoặc gọi Master API lên Branch), tuân thủ tuyệt đối Single-Writer Invariant.

### Tầng mạng

Branch Nginx có whitelist IP nội bộ/VPN trước khi proxy vào Branch app.

### Tầng database

Role database (tạo bởi `docker/postgres/init/01-roles.sh` khi volume mới tạo):

| Role | Dùng cho | Ghi chú |
|---|---|---|
| `erp_user` (`POSTGRES_USER`) | Flyway, script replication | superuser, chủ sở hữu bảng |
| `erp_app` (`APP_DB_USER`) | datasource của Spring Boot (HQ và chi nhánh) | không phải superuser; DML qua `ALTER DEFAULT PRIVILEGES` |
| `erp_repl` (`REPL_DB_USER`) | chuỗi kết nối subscription | REPLICATION + SELECT |

Ở chi nhánh, `db/migration-branch/R__branch_db_security.sql` (chạy lại sau mỗi lần migrate) áp nguyên tắc **mặc định chỉ đọc**:
REVOKE INSERT/UPDATE/DELETE/TRUNCATE trên mọi bảng khỏi `erp_app`, rồi chỉ GRANT lại cho bảng chi nhánh sở hữu (giao dịch, tồn, công nợ, idempotency). Bảng thêm sau này mặc định chỉ đọc. Nếu datasource là superuser/chủ bảng, migration in WARNING vì REVOKE không có tác dụng với role đó.

Trước 2026-10-01, app chạy bằng `erp_user` (superuser) nên REVOKE trong `V9__branch_db_security.sql` cũ không có tác dụng.

## 7. Password

Password được mã hóa bằng BCrypt thông qua Spring Security `PasswordEncoder`.

## 8. Idempotency

`IdempotencyAspect`:

- đọc `Idempotency-Key`;
- tạo request hash từ HTTP method + URI + serialized arguments;
- tìm record đã tồn tại;
- nếu hash khác -> Conflict;
- nếu giống -> trả response snapshot;
- nếu chưa có -> chạy nghiệp vụ và lưu snapshot.

## 9. Security issues cần theo dõi

1. Một số endpoint Branch scope cần được rà soát authority và role theo runtime thực tế.
