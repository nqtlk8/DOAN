# Kiểm thử backend

Tài liệu này là nguồn chuẩn về cách tổ chức, viết và chạy test của backend (`erp-backend`).
Mọi test và script kiểm thử backend nằm trong thư mục này.

## 1. Cấu trúc

```
erp-backend/
├── scripts/
│   ├── test.ps1 / test.sh        # Điểm vào duy nhất để chạy test
│   └── system/                   # Kiểm thử hệ thống trên stack Docker đang chạy
│       ├── common.sh             # Hàm dùng chung (psql, biến môi trường)
│       ├── smoke-group2.sh       # Smoke dữ liệu nền qua HTTP
│       └── replication-e2e.sh    # Replication hai chiều + quyền role ứng dụng
├── TESTING.md
└── src/test/
    ├── java/com/storename/erp/
    │   ├── <module>/api/          *Test   WebMvc slice (@WebMvcTest, MockMvc)
    │   ├── <module>/application/  *Test   unit (Mockito)
    │   ├── <module>/domain/       *Test   unit thuần, không Spring
    │   ├── <module>/integration/  *IT     Spring context + DB H2
    │   ├── profile/               *ProfileIT  ràng buộc vai trò HQ / Branch
    │   ├── postgres/              *IT     PostgreSQL thật (Testcontainers)
    │   ├── migration/             Flyway: migration và encoding
    │   ├── replication/           *IT     logical replication (Testcontainers)
    │   ├── contract/              OpenApiContractIT   sinh openapi.json
    │   ├── e2e/                   ReplicationE2EIT    cần docker-compose, bật bằng -Pe2e
    │   └── support/               lớp nền dùng chung (không phải test)
    └── resources/
        ├── application-postgres-it.yml
        ├── config/application.yml
        ├── docker-java.properties        # api.version của Docker cho Testcontainers
        └── replication-e2e/docker-compose.yml
```

Package con bên trong mỗi module (`api`, `application`, `domain`, `integration`) khớp với package mã nguồn chính.

## 2. Quy ước đặt tên và plugin

| Đuôi | Plugin Maven | Chạy khi | Ghi chú |
|---|---|---|---|
| `*Test` | Surefire | `mvn test` | Unit và WebMvc slice. Không khởi động Spring context đầy đủ. |
| `*IT` | Failsafe | `mvn verify` | Mọi test cần Spring context, DB, Testcontainers hoặc hạ tầng ngoài. |

Quy tắc:

1. **Không dùng `@Disabled` để bỏ test.** Test cần hạ tầng ngoài dùng điều kiện rõ ràng, ví dụ `@EnabledIfSystemProperty` (xem `ReplicationE2EIT`).
2. **Không bọc test trong `try/catch` nuốt lỗi.** Lỗi phải làm test đỏ.
3. **Test ghi dữ liệu không rollback** dùng `@DirtiesContext(classMode = AFTER_CLASS)` để lớp sau có DB sạch.
4. **Test PostgreSQL dùng chung một container cho cả JVM** (`PostgresIntegrationTest`, khởi động trong `static {}`). Vì vậy dùng `@Transactional` hoặc id/mã riêng; không so sánh số tuyệt đối trên dữ liệu dùng chung.
5. **Dữ liệu master** lấy từ `support/MasterDataFixtures` (id 9000+). Flyway không seed dữ liệu.
6. **SQL trong test phải điền đủ cột NOT NULL** của schema hiện hành (`db/migration`).
7. **Khi đổi hoặc xoá file migration**, chạy `mvn clean` để không còn file cũ trong `target/classes`.
8. **Cùng một lỗi test lặp lại 2 lần liên tiếp** thì dừng và xác định nguyên nhân gốc (`syntax`, `logic`, `config-env`, `data-state`) trước khi sửa tiếp. Không thử ngẫu nhiên.

## 3. Cách chạy

Chạy từ `erp-backend`, cả hai dạng `.ps1` (PowerShell) và `.sh` (Git Bash) đều được:

| Lệnh | Nội dung | Yêu cầu |
|---|---|---|
| `scripts/test.ps1 unit` | `*Test`: unit và WebMvc | JDK 21 |
| `scripts/test.ps1 it` | `*IT`: tích hợp H2 và PostgreSQL | JDK 21, Docker |
| `scripts/test.ps1 verify` | Toàn bộ, tương đương `mvn verify` | JDK 21, Docker |
| `scripts/test.ps1 e2e` | Replication E2E: tự dựng 2 PostgreSQL, tạo publication/subscription, chạy test, tự dọn | Docker |
| `scripts/test.ps1 smoke` | Smoke dữ liệu nền trên stack đang chạy | Stack `code/docker-compose.yml` đang chạy, Git Bash |
| `scripts/test.ps1 replication` | Replication hai chiều trên stack đang chạy | Stack đang chạy, Git Bash |

Tham số thêm được chuyển nguyên cho Maven, ví dụ `scripts/test.ps1 unit -Dtest=AuthServiceTest`.

Có thể chạy trực tiếp Maven: `./mvnw test`, `./mvnw verify`, `./mvnw verify -Pe2e -Dit.test=ReplicationE2EIT -Dtest=__none__ -Dsurefire.failIfNoSpecifiedTests=false`.

## 4. Kiểm thử hệ thống (`scripts/system`)

Các script này kiểm tra stack Docker đang chạy (HQ và chi nhánh), không phải test Maven.

- **`smoke-group2.sh`**: gọi HTTP tới HQ, kiểm tra dữ liệu nền (sản phẩm, nhà cung cấp, khách hàng, chi nhánh) và mã trùng trả về 409. Dữ liệu tạo ra có mã `SMK-<thời điểm>` và được xoá mềm ở cuối. Log ghi tại `target/system-tests/smoke-group2.log`. Payload JSON chỉ dùng ASCII (ký tự có dấu được escape `\uXXXX`) để không phụ thuộc code page của client Windows.
- **`replication-e2e.sh <chi nhánh>`**: kiểm tra replication HQ→chi nhánh và chi nhánh→HQ, đồng thời kiểm tra role `erp_app` ở chi nhánh không ghi được bảng master nhưng ghi được bảng giao dịch.

## 5. Hợp đồng API với frontend

`OpenApiContractIT` sinh `packages/api-contract/openapi.json` mỗi lần chạy test. File `src/generated/api.d.ts` của frontend phải được sinh lại và kiểm tra lệch:

```
npm run generate --workspace=packages/api-contract
npm run check --workspace=packages/api-contract
```

Chạy `check` sau mỗi lần đổi DTO hoặc controller.

## 6. Phạm vi và chưa làm

- Coverage (JaCoCo) chưa được cấu hình. Sẽ bổ sung sau khi có số liệu baseline từ một lần `scripts/test.ps1 verify` đầy đủ.
- Test frontend (Vitest, Playwright) nằm ngoài backend, xem `apps/erp-frontend`.
- Kết quả chạy thực tế không được ghi cứng trong tài liệu này. Ghi kết quả vào báo cáo của lần chạy tương ứng.

## 7. Phạm vi theo module

Bảng đếm số file test theo module, tạo từ cấu trúc thư mục hiện tại. Đây chỉ là số lượng file, không phải kết quả chạy. Khi thêm module hoặc test mới, cập nhật lại bảng này.

| Module | `*Test` (unit / WebMvc) | `*IT` (Spring / DB / E2E) |
|---|---:|---:|
| `analytics` | 4 | 2 |
| `branch` | 1 | 2 |
| `catalog` | 6 | 3 |
| `common` | 7 | 1 |
| `crm` | 7 | 4 |
| `identity` | 1 | 1 |
| `inventory` | 9 | 4 |
| `order` | 6 | 8 |
| `profile` | 0 | 2 |
| `postgres` | 0 | 5 |
| `replication` | 0 | 1 |
| `migration` | 1 | 1 |
| `contract` | 0 | 1 |
| `e2e` | 0 | 1 |
| **Tổng** | **42** | **36** |
