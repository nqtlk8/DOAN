# ERP Platform - Comprehensive Test Strategy

Tài liệu này định nghĩa chiến lược kiểm thử tiêu chuẩn cho toàn bộ hệ thống ERP, tuân thủ chặt chẽ kiến trúc 4 Level để đảm bảo tính module hóa, tốc độ thực thi, và độ bao phủ (coverage).

## 1. Phân Lớp Kiểm Thử (Testing Pyramid)

Hệ thống được kiểm thử dựa trên 4 cấp độ (Level) từ dưới lên:

### Level 1: Unit Tests (Fast & Isolated)
- **Mục đích**: Kiểm tra business logic của Domain Entities, Services, và Utility classes mà không khởi động Spring Context.
- **Công cụ**: JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`).
- **Quy tắc**:
  - KHÔNG load Spring Context.
  - Sử dụng `@Mock` và `@InjectMocks` cho các dependencies.
  - Test phải thực thi siêu nhanh (< 100ms/test).
- **Ví dụ**: `JwtTokenProviderTest`, `InventoryFacadeImplTest`, `StockOnHandTest`.

### Level 2: Slice Tests / Web Layer (Routing & Security)
- **Mục đích**: Kiểm thử các Web layer (Controllers), Authentication (401), Authorization (403), Input Validation (400), và HTTP Status.
- **Công cụ**: Spring `MockMvc`, `@WebMvcTest`.
- **Quy tắc**:
  - CHỈ khởi tạo các Beans liên quan đến Controller và Security. KHÔNG khởi tạo JPA (`@EnableJpaAuditing` phải được tách ra `JpaAuditingConfig`).
  - Phải `@MockBean` JwtAuthenticationFilter hoặc cấu hình `doAnswer()` để filter chain hoạt động chính xác.
  - Mọi service gọi từ Controller đều phải được `@MockBean`.
- **Ví dụ**: `DashboardControllerTest`, `BranchControllerTest`, `StockControllerTest`.

### Level 3: Backend Integration Tests (Database & Context)
- **Mục đích**: Đảm bảo các layer liên kết với nhau đúng đắn (Services -> Database), test native queries, test `@ConditionalOnProperty` (ContextHQ vs ContextBranch).
- **Công cụ**: `@SpringBootTest`, `@DataJpaTest`, H2 (PostgreSQL mode) hoặc Testcontainers.
- **Quy tắc**:
  - Dùng môi trường DB riêng (in-memory H2 PostgreSQL mode hoặc Testcontainers).
  - Dùng để test các query SQL Native phức tạp (ví dụ: Analytics, Dashboard).
  - Test đảm bảo các Beans cấu hình theo Profile hoạt động đúng.
- **Ví dụ**: `AnalyticsDataAdapterTest`, `ArchitectureV3HqTest`, `ArchitectureV3BranchTest`.

### Level 4: E2E Frontend Tests (User Flows)
- **Mục đích**: Mô phỏng thao tác của người dùng trên UI (App Shell), kiểm tra luồng end-to-end từ giao diện đến API giả lập hoặc thực tế.
- **Công cụ**: Playwright.
- **Quy tắc**:
  - Setup interceptors (`page.route`) trước khi `page.goto('/')` để tránh timeout/proxy error.
  - Test phải bao phủ các use case thực tế như: Tạo phiếu nhập, Bán hàng, Trả hàng, Phân quyền màn hình.
- **Ví dụ**: `inbound-receipt.spec.ts`, `dashboard.spec.ts`, `customer.spec.ts`.

## 2. Quy Tắc "Circuit Breaker" & Gỡ Lỗi (Debugging)
- Nếu một lỗi test lặp lại **2 lần liên tiếp**, kỹ sư / AI Agent phải **DỪNG** ngay lập tức và tiến hành Root Cause Analysis (RCA).
- Phải xác định lỗi thuộc lớp nào: `syntax`, `logic`, `config-env`, `data-state`.
- Cấm dùng "try and error" mù quáng (ví dụ: liên tục thay đổi annotation Spring Security mà không hiểu luồng Filter Chain).

## 3. Quy tắc cho test tích hợp (Level 3)

Cập nhật 2026-10-01.

### 3.1. Hai loại test tích hợp

| Loại | Nhận biết | DB | Chạy bằng |
|---|---|---|---|
| Postgres IT | kế thừa `test/PostgresIntegrationTest`, profile `postgres-it` | PostgreSQL thật (Testcontainers `postgres:16-alpine`), Flyway `db/migration` | `mvn verify` (failsafe, file `*IT.java`) |
| H2 IT | `@SpringBootTest(properties = "instance.role=ALL")` + profile `test` | H2 in-memory, schema do Hibernate tạo (`ddl-auto: create-drop`), không chạy Flyway | `mvn verify` |
| Test thường | các file `*Test.java` | H2 hoặc mock | `mvn test` (surefire) |

`SnapshotReplicationPostgresIT` tự dựng 2 container PostgreSQL 15 (HQ + chi nhánh) để kiểm logical replication có row filter.

### 3.2. Quy tắc bắt buộc

1. **Không có dữ liệu seed.** Flyway chỉ tạo schema. Test cần sản phẩm/danh mục dùng `test/MasterDataFixtures` (id 9000+). Test cần dữ liệu khác thì tự chèn, và câu SQL phải có **đủ cột NOT NULL** của schema hiện hành (`db/migration/V1..V8`).
2. **Postgres IT dùng một container cho cả JVM** (`PostgresIntegrationTest` khởi động trong `static {}`, không gắn `@Container`). Lý do: Spring cache ApplicationContext giữa các lớp test. Nếu container dừng sau mỗi lớp, các lớp sau báo "Failed to obtain JDBC Connection". Vì DB dùng chung, test nên chạy trong `@Transactional` (rollback) hoặc dùng mã/id riêng. Truy vấn "tất cả chi nhánh" phải so chênh lệch trước/sau thay vì số tuyệt đối.
3. **H2 IT ghi dữ liệu thật (không rollback) phải gắn `@DirtiesContext(classMode = AFTER_CLASS)`** để lớp sau có DB H2 mới (`testdb_${random.uuid}`). Lý do: `GoodsReturnConcurrencyIT` và `IdempotencyConcurrencyIT` cùng chèn tồn (sản phẩm 1, chi nhánh 1); dùng chung DB thì lớp chạy sau lỗi trùng `uk_stock_product_branch`.
4. **Không bọc test trong `try/catch` nuốt lỗi.** `ReceivableDebtLedgerPostgresIT.testIdempotencyKeyUniqueConstraint` từng "xanh giả": lệnh chèn đầu tiên lỗi, khối `catch` nuốt mất nên test không kiểm tra gì.
5. **Kiểm lỗi ràng buộc DB:**
   - Gọi qua `JdbcTemplate` hoặc repository của Spring Data thì nhận `DataIntegrityViolationException`.
   - Gọi `EntityManager.flush()` thì nhận `jakarta.persistence.PersistenceException` (Spring không dịch). Khi đó kiểm tra tên ràng buộc trong nguyên nhân gốc, ví dụ `ReceivableDebtMovementRepositoryIT`.
   - `ReceivableDebtMovementRepository` chỉ có `save` (append-only), không có `saveAndFlush`.
6. **Không dùng hàm của extension chưa cài.** Schema dùng `gen_random_uuid()` có sẵn từ PostgreSQL 13; không có extension `uuid-ossp`.
7. `mvn clean` sau khi đổi/xoá file migration. Nếu không, file cũ còn trong `target/classes` và Flyway báo trùng version.

### 3.3. Hợp đồng API cho frontend (`packages/api-contract`)

- `openapi.json` do `OpenApiGeneratorTest` sinh lại mỗi lần chạy test backend.
- `src/generated/api.d.ts` phải sinh tay: `npm run generate --workspace=packages/api-contract`.
- Kiểm tra lệch: `npm run check --workspace=packages/api-contract` (`scripts/check-generated.mjs`, thoát mã 1 nếu `api.d.ts` khác bản sinh từ `openapi.json`). Chạy lệnh này sau mỗi lần đổi DTO/controller.

## 4. Tình Trạng Hiện Tại (Coverage Status)

Kết quả chạy ngày 2026-10-01, lần cuối sau khi thêm danh mục sản phẩm (`mvn clean verify`, `npm run typecheck`, `vitest run`):

| Bộ test | Kết quả |
|---|---|
| Backend surefire (`*Test`) | 158 test, 0 lỗi, 1 bỏ qua (`ReplicationE2ETest` có `@Disabled`, cần hệ thống docker-compose đang chạy) |
| Backend failsafe (`*IT`) | 26/26 qua |
| Frontend typecheck | 0 lỗi |
| Frontend unit (Vitest) | 12 file, 52 test qua |
| Frontend Playwright | không chạy trong đợt này |
| Hợp đồng API | `npm run check` báo khớp |

Postgres IT được xác minh trên PostgreSQL 16. Tính năng dùng tới (row filter của publication, `REPLICA IDENTITY USING INDEX`, `max_slot_wal_keep_size`) có từ PostgreSQL 15, là bản docker-compose dùng.

Kiểm thử hệ thống chạy thật (ngoài Maven): `scripts/test-replication-e2e.sh <chi nhánh>` kiểm tra replication hai chiều và quyền ghi của role `erp_app` (xem `docs/architecture/REPLICATION_RUNBOOK.md`).

Phạm vi theo level:
- [x] Level 1 (Unit): Mọi module (Catalog, CRM, Inventory, Order, Auth) đều có Unit Test chuẩn Mockito.
- [x] Level 2 (WebMvc): Đã chuẩn hóa toàn bộ Controller Tests (sửa lỗi JPA Metamodel, mock JWT Filter, test mã 403 Forbidden).
- [x] Level 3 (Integration): Context HQ/Branch, native SQL trên PostgreSQL thật, sổ cái công nợ (`ReceivableDebtFinancialPostgresIT`: tổng sổ cái = số dư), luồng nhập–bán–thu–trả (`FullE2EFlowIT`), chống xác nhận trùng khi gọi đồng thời (`*ConcurrencyIT`), replication có row filter, dữ liệu hệ thống chỉ-HQ và cây danh mục (`ReferenceDataPostgresIT`: chạy Flyway như profile HQ trong schema riêng, chạy 2 lần để kiểm tra tính lặp lại).
- [x] Level 4 (Playwright): Đã thiết lập các kịch bản chính cho Dashboard, Khách hàng, Trả hàng, Nhập hàng, Bán hàng, và Login.

## 5. Hành Động Tiếp Theo (Roadmap)
- Liên tục cập nhật và bổ sung E2E testing cho các tính năng mới bằng Playwright.
- Chạy automation pipelines tích hợp 4 mức test trên CI/CD (gồm `mvn verify` và `npm run check --workspace=packages/api-contract`).
