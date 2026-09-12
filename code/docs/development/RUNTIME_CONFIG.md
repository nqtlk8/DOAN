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
- Flyway branch migrations;
- loại Redis auto-configuration.

## 5. Docker credential reality

Trong `docker-compose.yml`, các app containers hiện ghi đè:

```text
SPRING_DATASOURCE_USERNAME=erp_user
SPRING_DATASOURCE_PASSWORD=erp_password
```

Do đó runtime Docker khác default credentials trong YAML (`app_user` / `app_password`).

## 6. Database ports

```text
HQ        5432
Branch TP1 5433
Branch TP2 5434
```

Bên trong Docker, mọi PostgreSQL container vẫn nghe port 5432.
