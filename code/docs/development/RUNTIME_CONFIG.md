# Runtime Configuration — v5

## 1. Profiles

Các file cấu hình chính:

```text
application.yml
application-hq.yml
application-branch.yml
application-local.yml
application-test.yml
```

## 2. Base

```yaml
server:
  port: 8080
```

Profile mặc định trong environment nếu không ghi đè là `hq`.

## 3. HQ

`application-hq.yml`:

```text
instance.role = HQ
```

HQ cấu hình:

- PostgreSQL HQ;
- Redis;
- Cặp khóa bảo mật `JWT_PRIVATE_KEY_PATH` và `JWT_PUBLIC_KEY_PATH` (thường được trỏ tới file volume `/app/secrets/`);
- access token expiration 30 phút theo property hiện tại.

## 4. Branch

`application-branch.yml`:

```text
instance.role = BRANCH
branch-id = ${BRANCH_ID:HCM01}
```

Branch cấu hình:

- PostgreSQL riêng;
- Khóa `JWT_PUBLIC_KEY_PATH` để xác minh Token (không giữ private key);
- Flyway `db/migration` + `db/migration-branch` (placeholder `app_db_user` = datasource username);
- loại Redis auto-configuration.

## 5. Docker credential

User/mật khẩu lấy từ `.env` (mẫu `.env.example`); không có `.env` thì dùng mặc định trong `docker-compose.yml`:

```text
SPRING_DATASOURCE_USERNAME=${APP_DB_USER:-erp_app}      # role ứng dụng, không superuser
SPRING_FLYWAY_USER=${POSTGRES_USER:-erp_user}           # chủ bảng, chạy migration
```

HQ Flyway: `db/migration` + `db/migration-hq`. Chi nhánh: `db/migration` + `db/migration-branch`.

Compose profile: HQ + TP1 chạy mặc định; TP2 nằm trong profile `tp2` — bật/tắt bằng `scripts/branch.sh on|off tp2` (xem `docs/architecture/REPLICATION_RUNBOOK.md`).

Cổng mở ra host: HQ app `127.0.0.1:8080`; app chi nhánh không mở cổng (đi qua nginx 81/82 để giữ whitelist IP); DB chỉ mở trên `127.0.0.1` (5432/5433/5434); Redis không mở.

## 6. Database ports

```text
HQ        5432
Branch TP1 5433
Branch TP2 5434
```

Bên trong Docker, mọi PostgreSQL container vẫn nghe port 5432.
