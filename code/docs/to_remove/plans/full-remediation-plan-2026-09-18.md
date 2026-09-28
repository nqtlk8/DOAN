# KẾ HOẠCH SỬA TOÀN BỘ VẤN ĐỀ SAU CODE REVIEW — HƯỚNG DẪN CHO GEMINI

> Tài liệu này **tự chứa**: Gemini không cần đọc lịch sử hội thoại nào khác.
> Nguồn gốc: `code/docs/audit/code-review-2026-09-18.md` (báo cáo review). Mọi mã lỗi (A1, B4, …) dưới đây tham chiếu báo cáo đó.
> Ngày lập: 2026-09-18.

---

## PHẦN 0 — BỐI CẢNH BẮT BUỘC ĐỌC TRƯỚC KHI SỬA

### 0.1 Dự án là gì
Hệ thống ERP bán vật liệu xây dựng, kiến trúc **Modular Monolith** triển khai 2 vai trò từ **cùng một artifact**:
- **HQ** (`instance.role: HQ`, profile `hq`, DB `erp_hq`): master data (branch, product, customer, supplier, user), cấp JWT. Người dùng: **ADMIN**.
- **Branch** (`instance.role: BRANCH`, profile `branch`, DB `erp_branch_tp1`/`tp2`): dữ liệu giao dịch (hoá đơn bán, nhập kho, trả hàng, tồn kho, công nợ). Người dùng: **STAFF**.
- Có profile `local` (`instance.role: ALL`) chạy 1 process cho dev.
- Controller bật/tắt theo vai trò bằng `@ConditionalOnExpression("'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")` (và bản `'HQ' or 'ALL'`).

### 0.2 Cây thư mục (đường dẫn tương đối từ gốc repo)
```
code/
  docker-compose.yml                  # hq-app:8080, branch-tp1-app:8081, branch-tp2-app, frontend:80, branch nginx:81/82
  nginx-branch-tp1.conf, nginx-branch-tp2.conf
  scripts/setup-replication.sh
  docs/                               # tài liệu (audit/, plans/, sprints/, architecture/, development/, testing/)
  erp-platform/
    apps/erp-frontend/                # React 19 + Vite + TanStack Query + Tailwind
      src/api/{ApiService.ts,axiosInstance.ts}
      src/components/{sales,catalog,inventory,crm,layout,common,auth,admin,returns}
      src/context/{AuthContext.tsx,TabContext.tsx}
      src/hooks/use*.ts
      tests/*.spec.ts                 # Playwright
    packages/api-contract/            # openapi.json + src/generated/api.d.ts (type dùng chung)
    services/erp-backend/             # Java 21, Spring Boot 3.4.0, JPA, Flyway, JWT RS256
      src/main/java/com/storename/erp/{analytics,branch,catalog,common,crm,identity,inventory,order,system}
      src/main/resources/{application.yml,application-hq.yml,application-branch.yml,application-local.yml,application-test.yml,db/migration,db/migration-branch,certs,keys}
      src/test/java/... , src/test/resources/...
```
Mỗi domain chia 4 lớp: `api/` (controller + dto), `application/` (service + dto), `domain/` (entity), `infrastructure/` (repository). Giao tiếp giữa domain qua `*Facade` (vd `CrmFacade`, `CatalogFacade`, `InventoryFacade`, `BranchFacade`) — **không** gọi repository của domain khác.

### 0.3 Lệnh chạy / build
```bash
# Backend (cần JDK 21)
cd code/erp-platform/services/erp-backend && .\mvnw test          # Windows
cd code/erp-platform/services/erp-backend && ./mvnw test           # Linux/macOS

# Frontend
cd code/erp-platform/apps/erp-frontend && npm run lint && npx vitest run
cd code/erp-platform/apps/erp-frontend && npx playwright test

# Toàn hệ thống
cd code && docker compose up -d --build
bash scripts/setup-replication.sh tp1     # bật replication HQ <-> TP1 (chạy sau khi các container healthy)
# ADMIN:  http://localhost      (tài khoản admin)
# STAFF:  http://localhost:81   (tài khoản staff_tp1)   /  http://localhost:82 (staff_tp2)
```

### 0.4 QUY TẮC BẮT BUỘC (vi phạm = làm lại)
1. **Không revert, không "dọn dẹp" các thay đổi không nằm trong kế hoạch này.** `git status` sẽ hiện ~300 file `modified` chỉ vì khác ký tự xuống dòng CRLF/LF — **kệ chúng** (Step 19.5 sẽ xử lý).
2. **Không dùng `git add -A` / `git add .`** — luôn `git add <đường dẫn cụ thể>`.
3. **Debug theo circuit breaker** (`.agents/rules/strict-debugging-circuit-breaker.md`): nếu cùng một lỗi build/test lặp lại từ lần sửa thứ 2, phải DỪNG, viết ra: (1) nguyên văn lỗi, (2) các giả thuyết đã thử và vì sao sai, (3) phân loại lỗi (`syntax`/`logic`/`dependency-version`/`config-env`/`data-state`/`race-condition`/`unknown`), (4) bằng chứng cho giả thuyết mới, (5) kế hoạch sửa **chỉ 1 thay đổi**. Tối đa 3 vòng, sau đó dừng và báo người dùng.
4. **Document-Driven** (`.agents/AGENTS.md`): với thay đổi làm đổi hợp đồng API hoặc schema, cập nhật tài liệu trong `code/docs` **cùng commit**.
5. **Mọi Service/Controller sửa đổi phải giữ `@Slf4j`** và log `info` cho hành động nghiệp vụ, `error` cho lỗi.
6. Migration mới: UTF-8 **không BOM**, đặt trong `services/erp-backend/src/main/resources/db/migration/`, số phiên bản tiếp theo bắt đầu từ **V21** (hiện có tới V20).
7. Commit theo từng Step, message tiếng Việt **không dấu** (theo lịch sử repo), ví dụ: `fix(security): go private key khoi git index va rotate khoa JWT`.
8. Sau mỗi PHẦN, chạy lại toàn bộ test backend + frontend trước khi sang phần sau.

### 0.5 Thứ tự thực hiện
PHẦN I (Step 1–4) → PHẦN II (Step 5–14) → PHẦN III (Step 15–20) → PHẦN IV (Step 21–22).
Không đảo thứ tự PHẦN I: Step 2 là điều kiện để chạy được test.

---

# PHẦN I — NGHIÊM TRỌNG

## Step 1 — [A1] Gỡ private key khỏi git và xoay khoá JWT

**Vấn đề:** `code/erp-platform/services/erp-backend/src/main/resources/keys/private.pem` đang được git theo dõi (khoá ký JWT của profile `local`), dù `.gitignore` có `*.pem` — file đã vào index trước khi có rule.

**Thao tác:**
1. Kiểm tra xác nhận:
   ```bash
   cd code/erp-platform/services/erp-backend/src/main/resources
   git ls-files keys certs
   # kỳ vọng thấy: keys/private.pem, keys/public.pem, certs/public_key.pem
   ```
2. Gỡ khỏi index (giữ file trên đĩa để dev vẫn chạy được):
   ```bash
   git rm --cached keys/private.pem keys/public.pem
   ```
3. Sinh cặp khoá **mới** cho profile `local` (khoá cũ coi như đã lộ):
   ```bash
   cd code/erp-platform/services/erp-backend/src/main/resources/keys
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
   openssl rsa -pubout -in private.pem -out public.pem
   ```
   (Windows không có openssl: dùng `docker run --rm -v "%cd%":/w -w /w alpine/openssl ...` với cùng 2 lệnh trên.)
4. Sinh lại cặp khoá dùng cho docker (`code/secrets/`), vì khoá này từng nằm chung repo:
   ```bash
   cd code/secrets
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
   openssl rsa -pubout -in private_key.pem -out public_key.pem
   ```
5. `code/erp-platform/services/erp-backend/src/main/resources/certs/public_key.pem` đang được commit và là **public key** → giữ nguyên, nhưng phải khớp với private key mới của `code/secrets`. Copy đè:
   ```bash
   cp code/secrets/public_key.pem code/erp-platform/services/erp-backend/src/main/resources/certs/public_key.pem
   ```
6. Tạo file `code/erp-platform/services/erp-backend/src/main/resources/keys/README.md`:
   ```markdown
   # Khoá JWT cho profile `local`
   Hai file `private.pem` / `public.pem` **KHÔNG được commit** (xem .gitignore).
   Sinh lại bằng:
       openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
       openssl rsa -pubout -in private.pem -out public.pem
   Docker dùng cặp khoá riêng ở `code/secrets/` (cũng không commit).
   ```
7. Xác nhận `.gitignore` gốc repo có `*.pem` và `code/.gitignore` có `secrets/` (đã có sẵn — chỉ kiểm tra, không sửa).

**Acceptance:** `git ls-files | grep -i "\.pem$"` chỉ còn `certs/public_key.pem`. `docker compose up -d --build` đăng nhập được bằng khoá mới.

**Commit:** `fix(security): go private key khoi git index, rotate khoa JWT`

---

## Step 2 — [E1] Làm cho test chạy được trên máy sạch

**Vấn đề:** `src/main/resources/application-test.yml` trỏ `jwt.private-key: classpath:certs/private_key.pem`. File `src/test/resources/certs/private_key.pem` tồn tại trên máy dev nhưng **chưa từng được commit** (`git ls-files src/test/resources` chỉ trả về `application-postgres-it.yml` và `replication-e2e/docker-compose.yml`). `JwtTokenProvider` lặng lẽ để `privateKey = null` khi file không tồn tại → mọi test gọi `generateToken` ném `SigningKeyNotConfiguredException` sau khi clone mới (`InboundReceiptIdempotencyTest`, `SupplierBranchScopeTest`, `ArchitectureV3BranchTest`, `BranchRbacIntegrationTest`, `StockMovementApiTest`…).

**Cách sửa (chọn phương án A — khoá test cố định, commit vào repo):**
1. Sinh cặp khoá **chỉ dùng cho test**:
   ```bash
   cd code/erp-platform/services/erp-backend/src/test/resources/certs
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
   openssl rsa -pubout -in private_key.pem -out public_key.pem
   ```
2. Vì `.gitignore` chặn `*.pem`, phải ép thêm vào index:
   ```bash
   git add -f code/erp-platform/services/erp-backend/src/test/resources/certs/private_key.pem \
              code/erp-platform/services/erp-backend/src/test/resources/certs/public_key.pem
   ```
3. Thêm ghi chú ngay cạnh, file `src/test/resources/certs/README.md`:
   ```markdown
   # Khoá CHỈ dùng cho unit/integration test
   Được commit có chủ đích để test chạy được ngay sau khi clone.
   TUYỆT ĐỐI không dùng cặp khoá này cho môi trường thật.
   ```
4. Sửa `src/main/resources/application-test.yml` cho tường minh (giữ nguyên đường dẫn, chỉ thêm comment):
   ```yaml
   jwt:
     # Khoá test nằm ở src/test/resources/certs (được commit có chủ đích) — xem README ở đó.
     private-key: classpath:certs/private_key.pem
     public-key: classpath:certs/public_key.pem
     expiration-ms: 1800000
   ```
5. **Chống tái phát**: sửa `src/main/java/com/storename/erp/common/security/JwtTokenProvider.java` — hiện constructor im lặng khi thiếu khoá:
   ```java
   if (privateKeyResource != null && privateKeyResource.exists()) {
       this.privateKey = loadPrivateKey(privateKeyResource);
   } else {
       log.warn("JWT private key khong ton tai ({}) - instance nay se KHONG the cap token",
               privateKeyResource == null ? "khong cau hinh" : privateKeyResource.getDescription());
   }
   ```
   (Thêm `@Slf4j` của Lombok vào class nếu chưa có.) Làm tương tự cho public key. **Không** ném exception ở constructor: Branch cố tình không có private key.

**Test:**
```bash
cd code/erp-platform/services/erp-backend && ./mvnw test -Dtest="JwtTokenProviderTest,AuthIntegrationTest,InboundReceiptIdempotencyTest"
```
**Acceptance:** clone repo vào thư mục mới → `./mvnw test` chạy được, không có `SigningKeyNotConfiguredException`.

**Commit:** `test: them cap khoa JWT danh rieng cho test vao repo`

---

## Step 3 — [B1] Bật `@EnableScheduling`

**Vấn đề:** `src/main/java/com/storename/erp/ErpApplication.java` chỉ có `@SpringBootApplication` + `@EnableRetry`. Hai job `@Scheduled` không bao giờ chạy: `analytics/application/AnalyticsEtlJob.java:20` và `analytics/application/LowStockAlertJob.java:30` (job cảnh báo tồn kho — có logic thật).

**Thao tác:**
1. Sửa `ErpApplication.java`:
   ```java
   package com.storename.erp;

   import org.springframework.boot.SpringApplication;
   import org.springframework.boot.autoconfigure.SpringBootApplication;
   import org.springframework.retry.annotation.EnableRetry;
   import org.springframework.scheduling.annotation.EnableScheduling;

   @SpringBootApplication
   @EnableRetry
   @EnableScheduling
   public class ErpApplication {
       public static void main(String[] args) {
           SpringApplication.run(ErpApplication.class, args);
       }
   }
   ```
2. Job chỉ nên chạy ở đúng vai trò. Thêm điều kiện cho từng job:
   - `LowStockAlertJob`: cảnh báo tồn kho chi nhánh → thêm trên class
     ```java
     @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
         "'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")
     ```
   - `AnalyticsEtlJob`: báo cáo tổng hợp ở HQ → thêm
     ```java
     @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
         "'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
     ```
3. Tắt scheduler khi chạy test để test không bị nhiễu: thêm vào `src/main/resources/application-test.yml`:
   ```yaml
   spring:
     task:
       scheduling:
         pool:
           size: 1
   app:
     scheduling:
       enabled: false
   ```
   và đổi annotation trên 2 job thành có thêm điều kiện bật/tắt:
   ```java
   @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
       name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
   ```
4. `LowStockAlertJob.sendAlertEmail` hiện không gửi mail thật (JavaMailSender đang bị comment). Kiểm tra lại thân hàm: nếu nó chỉ `log.info` thì giữ nguyên và bổ sung comment `// TODO: bật JavaMailSender khi có SMTP`; nếu nó gọi mail thật thì bọc `try/catch` để lỗi SMTP không làm hỏng transaction.

**Test:** thêm `src/test/java/com/storename/erp/analytics/application/LowStockAlertJobTest.java` (nếu đã có thì bổ sung case) kiểm tra: config active + tồn kho dưới ngưỡng → tạo đúng 1 `InventoryAlertLog` trạng thái `ACTIVE`; gọi lần 2 không tạo thêm bản ghi.

**Acceptance:** khởi động branch-tp1-app, trong log 15 phút/lần có dòng `[ALERT] Starting low stock check...`.

**Commit:** `fix(scheduling): bat @EnableScheduling va gioi han job theo instance role`

---

## Step 4 — [B2] Bật `@EnableCaching` (hoặc gỡ annotation cache)

**Vấn đề:** `catalog/application/ProductReader.java:30,39` có `@Cacheable`, `catalog/application/ProductWriter.java:32,61,117` có `@CacheEvict`, nhưng **không nơi nào** có `@EnableCaching` → cache không tồn tại, gây hiểu nhầm.

**Thao tác:**
1. Tạo `src/main/java/com/storename/erp/common/config/CacheConfig.java`:
   ```java
   package com.storename.erp.common.config;

   import com.github.benmanes.caffeine.cache.Caffeine;
   import org.springframework.cache.CacheManager;
   import org.springframework.cache.annotation.EnableCaching;
   import org.springframework.cache.caffeine.CaffeineCacheManager;
   import org.springframework.context.annotation.Bean;
   import org.springframework.context.annotation.Configuration;

   import java.util.concurrent.TimeUnit;

   /**
    * Cache in-memory cho master data doc nhieu (product). Dung Caffeine de chay duoc
    * ca o HQ lan Branch (Branch khong co Redis - xem application-branch.yml).
    */
   @Configuration
   @EnableCaching
   public class CacheConfig {

       @Bean
       public CacheManager cacheManager() {
           CaffeineCacheManager manager = new CaffeineCacheManager("products");
           manager.setCaffeine(Caffeine.newBuilder()
                   .expireAfterWrite(5, TimeUnit.MINUTES)
                   .maximumSize(1000));
           return manager;
       }
   }
   ```
2. Thêm dependency vào `services/erp-backend/pom.xml` (trong `<dependencies>`):
   ```xml
   <dependency>
     <groupId>com.github.ben-manes.caffeine</groupId>
     <artifactId>caffeine</artifactId>
   </dependency>
   ```
   (Spring Boot parent 3.4.0 đã quản lý version, không cần `<version>`.)
3. **Quan trọng — bẫy tiềm ẩn:** `ProductReader.getAllProducts()` đang `@Cacheable(value="products")` và trả `List<ProductResponseDto>`; `getAllProductsWithBranchPrice()` **không** cache (đúng, vì giá phụ thuộc chi nhánh). Kiểm tra lại và **không** thêm cache cho hàm có giá theo chi nhánh. Nếu `getProductById` cache theo `#id` thì `@CacheEvict(allEntries = true)` ở `ProductWriter` đã xử lý đúng.
4. Tắt cache trong test để tránh nhiễu: thêm vào `application-test.yml`:
   ```yaml
   spring:
     cache:
       type: none
   ```

**Test:** `./mvnw test -Dtest="ProductReaderTest"` phải xanh. Thêm case: gọi `getAllProducts()` 2 lần → `productRepository.findAll()` chỉ bị gọi 1 lần (dùng `@SpringBootTest` với cache bật, hoặc bỏ qua nếu khó).

**Acceptance:** không còn annotation cache "chết".

**Commit:** `fix(cache): bat @EnableCaching voi Caffeine cho ca HQ va Branch`

---

# PHẦN II — MỨC CAO

## Step 5 — [B4] Chuẩn hoá ánh xạ lỗi → HTTP status

**Vấn đề:** `common/exception/GlobalExceptionHandler.java` thiếu handler cho `IllegalStateException`, `ObjectOptimisticLockingFailureException`, `DataIntegrityViolationException` → rơi vào handler `Exception` chung và trả **500 "Internal server error"**. Ngoài ra 3 chỗ dùng `RuntimeException("Unauthorized")` lẽ ra là 403.

**Thao tác A — thêm handler.** Mở `GlobalExceptionHandler.java`, thêm 3 handler (đặt trước handler `Exception.class` ở cuối file):
```java
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        // Vi pham rang buoc trang thai nghiep vu (vd: "Invoice is not DRAFT") -> 409, khong phai 500
        log.warn("Business state conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), java.util.List.of("STATE_CONFLICT")));
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Du lieu vua bi nguoi khac thay doi, vui long thu lai",
                        java.util.List.of("CONCURRENT_MODIFICATION")));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Du lieu vi pham rang buoc (trung khoa hoac thieu tham chieu)",
                        java.util.List.of("DATA_INTEGRITY")));
    }
```
Nếu class chưa có logger, thêm `@Slf4j` (Lombok) lên class.

**Thao tác B — thay `RuntimeException("Unauthorized")` bằng `SecurityException`** (đã có handler trả 403). Sửa đúng 3 chỗ:
- `order/application/SalesInvoiceService.java:187` (trong `getInvoice`)
- `order/application/GoodsReturnService.java:154` (trong `getReturnDto`)
- `inventory/application/InboundReceiptService.java:103`
```java
throw new SecurityException("Khong co quyen truy cap chung tu cua chi nhanh khac");
```
Đồng thời đổi `throw new RuntimeException("Invoice not found")` / `"Return not found"` trong cùng các hàm thành
`throw new com.storename.erp.common.exception.ResourceNotFoundException("...")` (đã có handler 404).

**Thao tác C — `ReceivableDebtService.java:77`**: `throw new RuntimeException("Cannot set opening balance because movements already exist for this customer.")` → đổi thành `IllegalStateException` với message tiếng Việt.

**Test:** bổ sung vào `src/test/java/com/storename/erp/common/exception/GlobalExceptionHandlerTest.java` 3 case mới (thêm endpoint giả vào `TestController` bên trong file test, giống các case sẵn có):
- `IllegalStateException` → 409
- `ObjectOptimisticLockingFailureException` → 409
- `DataIntegrityViolationException` → 409

**Acceptance:** không còn lỗi nghiệp vụ nào trả 500.

**Commit:** `fix(api): map loi nghiep vu ve 409/403/404 thay vi 500`

---

## Step 6 — [B7] Nút "Xóa chứng từ" đang lừa người dùng

**Vấn đề:** trong `apps/erp-frontend/src/components/sales/SalesOrderForm.tsx`, khối `onConfirm` của `ConfirmDialog` xử lý `confirmState.type === 'DELETE'` bằng đúng `closeTab(activeTabId)` — **không gọi API nào**, trong khi hộp thoại ghi "Hành động này không thể hoàn tác". Backend cũng **không có** endpoint xoá/huỷ hoá đơn.

**Quyết định:** ẩn chức năng xoá ở module Bán hàng (không tự ý thêm nghiệp vụ huỷ hoá đơn — việc đó cần bù trừ kho/công nợ, để sprint sau).

**Thao tác:**
1. `apps/erp-frontend/src/components/sales/SalesModule.tsx`: **xoá** dòng truyền `onDelete`:
   ```tsx
   // XOÁ dòng này:
   onDelete={() => formRef.current?.handleDelete()}
   ```
2. `apps/erp-frontend/src/components/layout/MdiModuleLayout.tsx`: nút "Xóa" hiện luôn hiển thị. Bọc nó theo prop giống nút "Xác nhận":
   ```tsx
   {onDelete && (
     <button onClick={onDelete} data-testid="btn-delete" className="...giữ nguyên class...">
       <Trash2 size={14} className="text-erp-text-accent-red" />
       <span>Xóa</span>
       <span className="text-slate-500 ml-1 text-erp-label">(F8)</span>
     </button>
   )}
   ```
   Đồng thời kiểm tra phần xử lý phím tắt trong cùng file (khối `useEffect` bắt F2/F3/F8/F9): F8 chỉ gọi `onDelete` khi prop tồn tại (`if (isView && onDelete) onDelete();`).
3. `SalesOrderForm.tsx`: giữ `handleDelete` nhưng đổi thành no-op có cảnh báo, để nếu module khác còn gọi thì không âm thầm đóng tab:
   ```tsx
   const handleDelete = () => {
     toast.error('Chuc nang xoa hoa don chua duoc ho tro. Vui long lien he quan tri vien.');
   };
   ```
   và **xoá** nhánh `'DELETE'` khỏi `confirmState` (`type` chỉ còn `'CANCEL' | 'EXIT'`), xoá các đoạn `confirmState.type === 'DELETE' ? ... : ...` trong `title`/`message` của `ConfirmDialog`.
4. Kiểm tra `components/inventory/InboundReceiptModule.tsx` và `components/returns/GoodsReturnModule.tsx` xem có cùng lỗi (nút Xóa không gọi API) không — nếu có, xử lý y hệt.

**Test:** `tests/sales-create.spec.ts` — thêm case: ở chế độ VIEW không tồn tại `btn-delete` (`await expect(page.getByTestId('btn-delete')).toHaveCount(0)`).

**Commit:** `fix(sales): bo nut xoa gia o man don ban hang`

---

## Step 7 — [B8] Chiết khấu & Thuế: làm đầy đủ từ DB đến UI

**Vấn đề:** UI có ô Chiết khấu (`discount`) và VAT (`tax`) — `SalesOrderForm.tsx:101-102`, dùng ở `:192` (`finalAmount = totalAmount - discount + tax`) và truyền xuống form ở `:327-330`; ô nhập nằm ở `GenericDocumentForm.tsx:256-269`. Nhưng payload gửi lên **không có** 2 trường này, backend tính `totalAmount = Σ lineTotal` → số "Còn nợ" hiển thị/in ra lệch với công nợ thực ghi vào DB.

### 7.1 Migration
Tạo `services/erp-backend/src/main/resources/db/migration/V21__add_discount_tax_to_sales_invoice.sql`:
```sql
-- Chiet khau va thue cho hoa don ban hang.
-- total_amount = SUM(line_total) - discount_amount + tax_amount
ALTER TABLE sales_invoice ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE sales_invoice ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
```

### 7.2 Entity
`order/domain/SalesInvoice.java` — thêm 2 field và sửa `calculateTotal()`:
```java
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;
```
```java
    public void calculateTotal() {
        BigDecimal lineSum = lines.stream()
                .map(SalesInvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        this.totalAmount = lineSum.subtract(discount).add(tax);
    }
```

### 7.3 DTO request
`order/application/dto/SalesInvoiceCreateDto.java` — thêm:
```java
    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = true, message = "Discount must be positive or zero")
    private BigDecimal discountAmount;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = true, message = "Tax must be positive or zero")
    private BigDecimal taxAmount;
```

### 7.4 Service
`order/application/SalesInvoiceService.java`, hàm `private SalesInvoice buildInvoice(...)` — set 2 field **trước** khi gọi `invoice.calculateTotal()`:
```java
        if (dto.getDiscountAmount() != null) invoice.setDiscountAmount(dto.getDiscountAmount());
        if (dto.getTaxAmount() != null) invoice.setTaxAmount(dto.getTaxAmount());
```
Và **sau** `invoice.calculateTotal()`, thêm validate (đặt cạnh validate advancePayment đã có):
```java
        BigDecimal lineSum = invoice.getLines().stream()
                .map(SalesInvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (invoice.getDiscountAmount().compareTo(lineSum) > 0) {
            throw new IllegalArgumentException("Chiet khau khong duoc lon hon tong tien hang");
        }
        if (invoice.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tong tien hoa don khong duoc am");
        }
```
Lưu ý: validate `advancePayment <= totalAmount` sẵn có vẫn đúng vì `totalAmount` giờ đã trừ chiết khấu.

### 7.5 DTO response
`order/api/dto/SalesInvoiceResponseDto.java` — thêm `private BigDecimal discountAmount;` và `private BigDecimal taxAmount;`, set trong `fromEntity(...)`.

### 7.6 Frontend
`apps/erp-frontend/src/components/sales/SalesOrderForm.tsx`:
- Trong `normalizeInitialData`, ánh xạ thêm: `discount: Number(data.discountAmount ?? 0), tax: Number(data.taxAmount ?? 0)`.
- Khởi tạo state theo dữ liệu: `const [discount, setDiscount] = useState<number>(initialData?.discount || 0);` và tương tự cho `tax`.
- Trong `handleSubmit`, thêm vào `payload`: `discountAmount: discount, taxAmount: tax,`.
- Thêm validate phía UI trước khi gửi: nếu `discount > totalAmount` → `toast.error('Chiết khấu không được lớn hơn tiền hàng')` và `return`.
- Trong `handleAdd` (reset form), thêm `setDiscount(0); setTax(0);`.

### 7.7 Cập nhật API contract
```bash
# 1) chạy backend (profile local hoặc docker hq-app)
# 2) lấy spec mới:
curl http://localhost:8080/v3/api-docs -o code/erp-platform/packages/api-contract/openapi.json
# 3) sinh lại type:
cd code/erp-platform/packages/api-contract && npm run generate
```
Nếu không chạy được backend, sửa tay `openapi.json` (thêm `discountAmount`, `taxAmount` vào `SalesInvoiceCreateDto` và `SalesInvoiceResponseDto`, kiểu `number`, format `double`) rồi vẫn chạy `npm run generate`.

### 7.8 Test
- Backend `SalesInvoiceServiceTest`: thêm case `createAndConfirm` với `discountAmount = 50`, `taxAmount = 20`, 2 dòng hàng tổng 200 → `totalAmount == 170`, công nợ tăng đúng 170; case `discountAmount = 300 > lineSum` → `IllegalArgumentException`.
- Playwright `tests/sales-create.spec.ts`: bổ sung assert `capturedRequest.discountAmount` và `capturedRequest.taxAmount` tồn tại.

**Acceptance:** nhập chiết khấu 50.000 → "Còn nợ" trên form và `remaining_debt` trong DB khớp nhau.

**Commit:** `feat(sales): ho tro chiet khau va thue tu DB den UI`

---

## Step 8 — [B9] Cho phép chọn hình thức thanh toán

**Vấn đề:** `SalesOrderForm.tsx:86` — `const [paymentMethod] = useState(initialData?.paymentMethod || 'CASH');` không có setter, không có UI → mọi hoá đơn đều `CASH`, trong khi enum backend (`order/domain/PaymentMethod.java`) có `CASH`, `CREDIT`, `MIXED` và DTO bắt buộc trường này.

**Thao tác:**
1. `SalesOrderForm.tsx`: `const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'CREDIT' | 'MIXED'>(initialData?.paymentMethod || 'CASH');`
2. `GenericDocumentForm.tsx`: thêm 2 prop tuỳ chọn vào interface `GenericDocumentFormProps`:
   ```tsx
   paymentMethod?: string;
   setPaymentMethod?: (val: string) => void;
   ```
   và render một `<select>` ngay phía trên ô "Trả trước" (khối quanh dòng 272), dùng đúng class của các input khác:
   ```tsx
   {setPaymentMethod && (
     <>
       <div className="text-right text-erp-label pr-2">Thanh toán</div>
       <select
         data-testid="sales-payment-method"
         disabled={isView}
         value={paymentMethod}
         onChange={(e) => setPaymentMethod(e.target.value)}
         className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none"
       >
         <option value="CASH">Tiền mặt</option>
         <option value="CREDIT">Công nợ</option>
         <option value="MIXED">Kết hợp</option>
       </select>
     </>
   )}
   ```
3. `SalesOrderForm.tsx`: truyền `paymentMethod={paymentMethod} setPaymentMethod={(v) => setPaymentMethod(v as any)}` xuống `GenericDocumentForm`; trong `handleAdd` reset về `'CASH'`.

**Test:** Playwright — chọn `CREDIT` rồi Lưu, assert `capturedRequest.paymentMethod === 'CREDIT'`.

**Commit:** `feat(sales): cho phep chon hinh thuc thanh toan`

---

## Step 9 — [A3 + B5] Siết phạm vi dữ liệu theo chi nhánh

**Vấn đề A3:** `common/security/JwtAuthenticationFilter.java:63` chỉ chặn khi `branchId != null`:
```java
if ("BRANCH".equalsIgnoreCase(instanceRole) && branchId != null && !branchId.equals(configuredBranchId)) { ... 403 ... }
```
→ token ADMIN (`branchId = null`) đi lọt vào instance BRANCH; sau đó `AuthUtils.getBranchIdOrNull()` trả `null` và các read path hiểu null = "tất cả chi nhánh": `inventory/application/StockService.java:27` (`findAll`), `crm/api/CustomerReadController.java:28`, `crm/api/ReceivableDebtController.java:38`.

**Vấn đề B5:** khi `branchId = null`, `order/application/SalesInvoiceService.java:179` trả **rỗng** (`findByBranchId(null)`), trong khi các module khác trả **tất cả** → hành vi không nhất quán.

**Thao tác:**
1. `JwtAuthenticationFilter`: đổi điều kiện thành — ở instance BRANCH, token **bắt buộc** phải có `branchId` khớp:
   ```java
   if ("BRANCH".equalsIgnoreCase(instanceRole)) {
       if (branchId == null || branchId.isBlank() || !branchId.equals(configuredBranchId)) {
           logger.error(String.format(
               "Branch isolation violation: token branchId '%s' khong khop instance branchId '%s'",
               branchId, configuredBranchId));
           response.setStatus(HttpServletResponse.SC_FORBIDDEN);
           response.setContentType("application/json");
           response.setCharacterEncoding("UTF-8");
           ApiResponse<Void> apiResponse = ApiResponse.error(
               "Token khong thuoc chi nhanh nay", Collections.singletonList("FORBIDDEN_BRANCH"));
           response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
           return;
       }
   }
   ```
   **Lưu ý:** việc này chặn ADMIN gọi trực tiếp instance Branch. Đó là hành vi mong muốn (ADMIN làm việc ở HQ). Nếu sau này ADMIN cần xem dữ liệu chi nhánh thì đi qua HQ + replication.
2. Chuẩn hoá quy ước `branchId == null` = "tất cả chi nhánh" **chỉ áp dụng ở HQ**. Sửa `SalesInvoiceService.getInvoicesByBranch` cho nhất quán:
   ```java
   @Transactional(readOnly = true)
   public List<SalesInvoice> getInvoicesByBranch(Long branchId) {
       // branchId == null (ADMIN o HQ) -> xem tat ca chi nhanh, giong Stock/Customer/Debt
       return branchId == null ? invoiceRepository.findAll() : invoiceRepository.findByBranchId(branchId);
   }
   ```
3. Viết quy ước này thành comment ở `common/security/AuthUtils.java` phía trên `getBranchIdOrNull()`:
   ```java
   /**
    * Tra ve branchId tu JWT, hoac null khi token khong gan chi nhanh (ADMIN o HQ).
    * QUY UOC TOAN HE THONG: null = "moi chi nhanh" (chi xay ra o instance HQ, vi
    * instance BRANCH da chan token khong co branchId o JwtAuthenticationFilter).
    */
   ```

**Test:**
- `JwtAuthenticationFilterTest`: thêm case `instanceRole=BRANCH`, token không có `branchId` → 403.
- Cập nhật test cũ nếu có case dựa trên hành vi "cho qua khi branchId null".
- `SalesInvoiceServiceTest`: `getInvoicesByBranch(null)` gọi `findAll()`.

**Commit:** `fix(security): chan token khong co branchId tren instance BRANCH, chuan hoa quy uoc branchId null`

---

## Step 10 — [A4 + A5] Bổ sung `@PreAuthorize` và `@Valid`

**A4 — thiếu phân quyền:** `branch/api/BranchController.java` — `getAllBranches()` (dòng ~34) và `getBranchById()` (dòng ~41) không có `@PreAuthorize` → mọi user đăng nhập, kể cả STAFF, đọc được `internal_url` của tất cả chi nhánh.
Thêm vào cả hai:
```java
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
```
(Frontend cần danh sách chi nhánh ở form khách hàng — Step 7 của sprint trước — nên giữ quyền cho STAFF, nhưng phải khai báo tường minh.) Đồng thời trong `branch/application/dto/BranchDTO.java`, **bỏ `internalUrl` khỏi response khi người gọi là STAFF**: cách đơn giản và an toàn là tạo thêm `BranchSummaryDto` (id, code, name) và cho endpoint GET dùng DTO này; chỉ `@PreAuthorize("hasAuthority('ADMIN')")` mới trả `BranchDTO` đầy đủ.

**A5 — thiếu `@Valid`** ở 6 endpoint nhận `@RequestBody`:
| File | Dòng | Endpoint |
|---|---|---|
| `branch/api/BranchController.java` | 26 | `POST /api/v1/branches` |
| `branch/api/BranchController.java` | 48 | `PUT /api/v1/branches/{id}` |
| `crm/api/ReceivableDebtController.java` | 50 | `POST /api/v1/receivable-debts/opening-balance` |
| `identity/api/AuthController.java` | 25 | `POST /api/v1/auth/login` |
| `identity/api/AuthController.java` | 31 | `POST /api/v1/auth/refresh` |
| `identity/api/AuthController.java` | 37 | `POST /api/v1/auth/revoke` |

Với mỗi endpoint: thêm `@Valid` trước `@RequestBody`, và thêm ràng buộc vào DTO tương ứng:
- `branch/application/dto/BranchDTO.java`: `@NotBlank` cho `code`, `name`; `@Size(max = 255)` cho `internalUrl`.
- `crm/api/dto/OpeningBalanceRequestDto.java`: `@NotNull` cho `customerId`, `@NotNull @DecimalMin("0.0")` cho `amount`.
- `identity/api/dto/AuthRequest.java`: `@NotBlank` cho `username`, `password`.
- `identity/api/dto/TokenRefreshRequest.java`: `@NotBlank` cho `refreshToken`.

**Lưu ý:** sau khi thêm `@NotBlank` cho login, `AuthService.login` không còn cần tự kiểm tra rỗng, nhưng **giữ nguyên** kiểm tra đó (defence in depth) — chỉ đảm bảo nó ném `IllegalArgumentException` (đã map 400 ở Step 5).

**Test:** `AuthIntegrationTest` — POST login với body `{}` → 400 kèm message của bean validation (hiện đang 401).

**Commit:** `fix(api): them @PreAuthorize cho BranchController va @Valid cho cac endpoint ghi`

---

## Step 11 — [A2] Cho phép thu hồi access token

**Vấn đề:** `identity/application/AuthService.revoke()` chỉ ghi `tokenId` của **refresh token** vào Redis; `refresh()` mới kiểm tra. `common/security/JwtAuthenticationFilter` không kiểm tra gì → sau khi Đăng xuất, access token vẫn dùng được tới 30 phút. Instance Branch không có Redis nên không kiểm tra được.

**Cách sửa (phương án khả thi với kiến trúc hiện tại):**
1. **Rút ngắn tuổi access token**: `application-hq.yml` → `jwt.expiration-ms: 900000` (15 phút). Ghi chú lý do bằng comment.
2. **Thu hồi cả access token tại HQ**: trong `AuthResponse`/`AuthService.login`, access token và refresh token hiện dùng `tokenId` khác nhau. Sửa `login()` để **dùng chung một `tokenId`** cho cặp token (đặt tên `sessionId`), nhờ vậy revoke một lần là chặn cả cặp:
   ```java
   String sessionId = UUID.randomUUID().toString();
   String accessToken  = tokenProvider.generateToken(subject, roleCode, branchIdStr, sessionId);
   String refreshToken = tokenProvider.generateRefreshToken(subject, roleCode, branchIdStr, sessionId);
   ```
   Làm tương tự trong `refresh()` (sinh `sessionId` mới cho cặp token mới).
3. **Kiểm tra danh sách thu hồi ở HQ**: tạo interface `common/security/TokenRevocationChecker` với method `boolean isRevoked(String tokenId)`;
   - Bản HQ (`@ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")`) dùng `StringRedisTemplate` với tiền tố `revoked_token:` (giữ nguyên tiền tố hiện tại trong `AuthService`).
   - Bản mặc định (`@ConditionalOnMissingBean`) luôn trả `false` (Branch không có Redis).
   Trong `JwtAuthenticationFilter`, sau khi lấy `tokenId`, thêm:
   ```java
   if (revocationChecker.isRevoked(tokenId)) {
       throw new io.jsonwebtoken.JwtException("Token da bi thu hoi");
   }
   ```
4. **Frontend**: `apps/erp-frontend/src/context/AuthContext.tsx` — `logout()` hiện gọi `ApiService.Auth.revoke(refreshToken)` **sau khi** đã xoá localStorage; giữ nguyên thứ tự nhưng bọc `await` trong `try/catch` (đã có) và thêm comment rằng access token cũ chỉ hết hiệu lực ở HQ.
5. **Ghi rõ giới hạn** vào `code/docs/architecture/SECURITY_MODEL.md` (Step 21): Branch không kiểm tra thu hồi được vì không có Redis; bù lại access token chỉ sống 15 phút.

**Test:** `AuthIntegrationTest` — login → revoke → gọi endpoint bảo vệ bằng access token cũ ở HQ → 401.

**Commit:** `feat(security): thu hoi ca access token bang session id chung`

---

## Step 12 — [B3] Idempotency: chống kẹt `IN_PROGRESS` và dọn bảng

**Vấn đề:** `common/aop/IdempotencyAspect.java:67` ghi `IN_PROGRESS` trước khi chạy nghiệp vụ; nếu tiến trình chết giữa chừng, bản ghi ở lại vĩnh viễn → mọi request sau cùng `Idempotency-Key` nhận 409 mãi (`:86`). Bảng `idempotency_record` cũng không bao giờ được dọn.

**Thao tác:**
1. Kiểm tra entity `common/domain/IdempotencyRecord.java`: nếu **chưa** có `createdAt` thì thêm (kèm migration). Schema `V1` đã có cột `created_at` cho bảng này, nên nhiều khả năng chỉ cần bổ sung field:
   ```java
   @Column(name = "created_at", nullable = false)
   private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
   ```
2. `IdempotencyAspect` — coi `IN_PROGRESS` quá hạn là đã chết. Ở nhánh `if (!isLockAcquired)`, thay đoạn xử lý `IN_PROGRESS`:
   ```java
   private static final java.time.Duration IN_PROGRESS_TTL = java.time.Duration.ofMinutes(2);
   ...
   if ("IN_PROGRESS".equals(existing.getResponseSnapshot())) {
       boolean stale = existing.getCreatedAt() != null
               && existing.getCreatedAt().isBefore(java.time.LocalDateTime.now().minus(IN_PROGRESS_TTL));
       if (!stale) {
           throw new ResponseStatusException(HttpStatus.CONFLICT,
                   "Request dang duoc xu ly, vui long thu lai sau");
       }
       // Ban ghi treo tu lan chay truoc (tien trinh chet giua chung) -> thu hoi khoa
       log.warn("Thu hoi idempotency lock treo: key={}", idempotencyKey);
       transactionTemplate.execute(status -> {
           idempotencyRepo.findByIdempotencyKey(idempotencyKey).ifPresent(idempotencyRepo::delete);
           return null;
       });
       throw new ResponseStatusException(HttpStatus.CONFLICT,
               "Yeu cau truoc do bi gian doan, vui long gui lai");
   }
   ```
3. Tạo job dọn `common/application/IdempotencyCleanupJob.java`:
   ```java
   package com.storename.erp.common.application;

   import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
   import lombok.RequiredArgsConstructor;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.scheduling.annotation.Scheduled;
   import org.springframework.stereotype.Component;
   import org.springframework.transaction.annotation.Transactional;

   import java.time.LocalDateTime;

   /** Xoa ban ghi idempotency cu de bang khong phinh vo han. */
   @Slf4j
   @Component
   @RequiredArgsConstructor
   @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
       name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
   public class IdempotencyCleanupJob {

       private final IdempotencyRecordRepository repository;

       @Scheduled(cron = "0 30 3 * * *") // 03:30 hang ngay
       @Transactional
       public void cleanup() {
           LocalDateTime threshold = LocalDateTime.now().minusDays(7);
           int deleted = repository.deleteByCreatedAtBefore(threshold);
           log.info("[IDEMPOTENCY] Da xoa {} ban ghi cu hon {}", deleted, threshold);
       }
   }
   ```
   và thêm vào `common/infrastructure/IdempotencyRecordRepository.java`:
   ```java
   @org.springframework.data.jpa.repository.Modifying
   @org.springframework.transaction.annotation.Transactional
   int deleteByCreatedAtBefore(java.time.LocalDateTime threshold);
   ```
4. Thêm index để job không quét toàn bảng — migration `V22__index_idempotency_created_at.sql`:
   ```sql
   CREATE INDEX IF NOT EXISTS idx_idempotency_record_created_at ON idempotency_record (created_at);
   ```

**Test:** `IdempotencyConcurrencyIT` (đã tồn tại) phải xanh; thêm unit test: bản ghi `IN_PROGRESS` với `createdAt` 10 phút trước → request mới **không** bị 409 vĩnh viễn mà được nhận lại sau khi khoá bị thu hồi.

**Commit:** `fix(idempotency): thu hoi khoa treo va them job don ban ghi cu`

---

## Step 13 — [B6 + B14] Sửa vi phạm rules-of-hooks và toast trùng lặp

**B6 — hook trong hàm thường:** `apps/erp-frontend/src/hooks/useSalesInvoice.ts:14`:
```ts
const getById = (id: string) => useQuery({ ... });   // SAI: gọi hook trong hàm thường
```
**Thao tác:** tách thành hook riêng ở cuối file và **xoá** `getById` khỏi object trả về:
```ts
export const useSalesInvoiceById = (id: string) =>
  useQuery({
    queryKey: ['salesInvoices', id],
    queryFn: () => ApiService.SalesInvoice.getById(id),
    enabled: !!id,
  });
```
Tìm mọi nơi dùng `getById` từ hook này (`grep -rn "useSalesInvoice()" apps/erp-frontend/src`) và chuyển sang `useSalesInvoiceById`. Kiểm tra các hook khác cùng lỗi: `grep -rn "=> useQuery(" apps/erp-frontend/src` — sửa tương tự (`useCustomers`, `useProducts`, `useSuppliers`, `useInboundReceipt` nếu có).

**B14 — toast trùng:** mutation vừa `notify.success` trong hook (`useCustomers.ts:17,25,33`, `useProducts.ts`, `useSuppliers.ts`, `useSalesInvoice.ts`), vừa có toast lỗi toàn cục trong `api/axiosInstance.ts`, vừa có `toast.error` trong component → một hành động có thể bắn 2 thông báo.
**Quy ước mới (ghi vào `apps/erp-frontend/README.md`):**
- **Thành công**: chỉ hook (`notify.success`) được phép báo.
- **Lỗi**: chỉ interceptor toàn cục trong `axiosInstance` được phép báo.
- Component chỉ toast cho lỗi **validate phía client** (chưa gọi API).
**Thao tác:** trong `components/sales/SalesOrderForm.tsx` bỏ `toast.error(err.message || 'Lỗi khi lưu đơn hàng')` trong `catch` của `handleSubmit` và `handleConfirm` (chỉ giữ `console.error`), vì interceptor đã toast. Rà tương tự ở `InboundReceiptModule.tsx`, `GoodsReturnModule.tsx`, `CustomerList.tsx`, `ProductList.tsx`, `SupplierList.tsx`.

**Test:** `npx vitest run`; Playwright case lỗi 400 ở `sales-create.spec.ts` vẫn thấy đúng **một** thông báo lỗi.

**Commit:** `fix(frontend): tach hook useSalesInvoiceById va bo toast trung lap`

---

## Step 14 — [B12 + B13] Thống nhất chính sách retry và tạo tồn kho lần đầu

**B12:** `order/application/GoodsReturnService.java:74` chỉ retry `ObjectOptimisticLockingFailureException`, trong khi `SalesInvoiceService` retry thêm `DataIntegrityViolationException`.
**Thao tác:** sửa annotation của `confirmReturn` thành:
```java
    @org.springframework.retry.annotation.Retryable(
        retryFor = {
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class
        },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
```
Làm tương tự cho `inventory/application/InboundReceiptService.confirmReceipt` (kiểm tra annotation hiện tại và đồng bộ).

**B13:** `inventory/application/InventoryFacadeImpl.java:67-68` dùng `orElseGet(() -> stockOnHandRepository.save(new StockOnHand(productId, branchId)))` — hai request song song cho sản phẩm **chưa có** dòng tồn kho sẽ cùng INSERT và vi phạm unique `uk_stock_product_branch`.
**Thao tác:** bọc việc tạo mới để lỗi trùng được xử lý thay vì ném thẳng:
```java
        StockOnHand stock = stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)
                .orElseGet(() -> createStockRow(productId, branchId));
```
```java
    private StockOnHand createStockRow(Long productId, Long branchId) {
        try {
            return stockOnHandRepository.saveAndFlush(new StockOnHand(productId, branchId));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Luong khac vua tao dong ton kho cho cung (product, branch) -> doc lai
            log.warn("Dong ton kho product={} branch={} vua duoc tao boi luong khac", productId, branchId);
            return stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)
                    .orElseThrow(() -> e);
        }
    }
```
**Lưu ý:** `saveAndFlush` là cần thiết để lỗi trùng nổ ra ngay tại đây thay vì lúc commit.

**Test:** `InboundReceiptConcurrencyIT` / `GoodsReturnConcurrencyIT` (đã có) phải xanh.

**Commit:** `fix(inventory): dong bo chinh sach retry va xu ly tao dong ton kho dong thoi`

---

# PHẦN III — MỨC TRUNG BÌNH

## Step 15 — [B10] Sửa tsconfig và bật type-check

**Vấn đề:** `apps/erp-frontend/tsconfig.json` đang là cấu hình **Next.js** (`plugins: [{ "name": "next" }]`, include `next-env.d.ts`, `.next/types/**`) trong dự án **Vite**; thiếu `types: ["vite/client"]`. `package.json` build bằng `vite build` (không type-check) → lỗi kiểu thật vẫn nằm im.

**Thao tác:**
1. Thay toàn bộ `tsconfig.json` bằng:
   ```json
   {
     "compilerOptions": {
       "target": "ES2020",
       "lib": ["ES2020", "DOM", "DOM.Iterable"],
       "module": "ESNext",
       "moduleResolution": "bundler",
       "jsx": "react-jsx",
       "strict": true,
       "noEmit": true,
       "skipLibCheck": true,
       "esModuleInterop": true,
       "resolveJsonModule": true,
       "isolatedModules": true,
       "allowJs": false,
       "types": ["vite/client", "vitest/globals", "@testing-library/jest-dom"],
       "baseUrl": ".",
       "paths": { "@/*": ["./src/*"] }
     },
     "include": ["src", "vite.config.ts", "vitest.config.ts", "vitest.setup.ts"],
     "exclude": ["node_modules", "dist", "tests"]
   }
   ```
   (`tests/` là Playwright, có tsconfig/type riêng; nếu muốn check luôn thì thêm và cài `@playwright/test` vào devDependencies.)
2. Tạo `src/vite-env.d.ts`:
   ```ts
   /// <reference types="vite/client" />

   interface ImportMetaEnv {
     readonly VITE_ENFORCE_BRANCH_URL?: string;
     readonly VITE_HQ_TARGET?: string;
     readonly VITE_BRANCH_TARGET?: string;
   }
   interface ImportMeta {
     readonly env: ImportMetaEnv;
   }
   ```
3. Thêm script vào `package.json`:
   ```json
   "typecheck": "tsc --noEmit",
   "build": "tsc --noEmit && vite build",
   ```
4. Chạy `npx tsc --noEmit` và **sửa hết lỗi**. Các lỗi đã biết:
   - `components/sales/SalesOrderForm.tsx:353` — `customer.customerId` không tồn tại trong `CustomerResponseDto` → dùng `customer.customerCode` (đã có fallback `||`), xoá nhánh `customer.customerId`.
   - `components/sales/SalesOrderForm.tsx:358` — `customer.contactPerson` không tồn tại → bỏ dòng `setContactPerson(customer.contactPerson || '')` hoặc thêm field `contactPerson` vào backend `CustomerResponseDto` + `Customer` entity + migration (chọn **bỏ ở frontend**, vì DB không có cột này).
   - `components/sales/SalesOrderForm.tsx:449` và `:476` — `SearchModal<Customer>` / combobox nhận `Promise<CustomerResponseDto[]>` nhưng khai báo `Promise<Customer[]>` → bỏ type thủ công, dùng type sinh từ contract (xem Step 18.3).
   - `components/common/document/GenericDocumentForm.tsx` — `errors['partner']`, `errors[\`item_${i}_product\`]` báo lỗi index `{}` → khai báo `errors?: Record<string, string>` trong interface props (hiện đang là `{}` ngầm định).
   - Sửa tiếp mọi lỗi khác `tsc` báo.

**Acceptance:** `npm run typecheck` trả về 0 lỗi.

**Commit:** `chore(frontend): sua tsconfig cho Vite va bat type-check trong build`

---

## Step 16 — [E3] Trả các rule ESLint quan trọng về `error`

**Vấn đề:** `apps/erp-frontend/eslint.config.mjs` đang hạ cấp các rule bắt lỗi thật (Sprint 36 làm vậy để build không đứt).

**Thao tác:** sửa khối `rules` thành:
```js
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'react-hooks/rules-of-hooks': 'error',      // bat loi that (xem Step 13)
      'react-hooks/exhaustive-deps': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-empty': 'error',
      // 2 rule thu nghiem cua React Compiler van tat, kem ly do:
      'react-hooks/set-state-in-effect': 'off',   // chua on dinh, gay false positive
      'react-hooks/immutability': 'off',
    },
```
Chạy `npm run lint`, sửa **mọi** lỗi `error` mới xuất hiện (rules-of-hooks đã xử lý ở Step 13; `no-empty` thường là `catch {}` → thêm `console.error` hoặc comment giải thích).
Giảm dần `any`: ưu tiên các file `api/ApiService.ts` (`(res: any)` → `res.data.data` với type sinh sẵn) và `components/sales/*`.

**Acceptance:** `npm run lint` exit code 0, không còn `error`.

**Commit:** `chore(frontend): tra rules-of-hooks ve error va don loi lint`

---

## Step 17 — [B11] Lọc và phân trang ở tầng DB

**Vấn đề:** không có `Pageable`/`Page<>` ở đâu; nhiều endpoint tải **toàn bộ bảng** rồi lọc trong Java: `crm/api/CustomerReadController.java:28`, `catalog/api/ProductReadController.java:36`, `catalog/application/ProductReader.java:33,52`, `inventory/application/StockService.java:27`, `crm/api/ReceivableDebtController.java:38`, `branch/application/BranchService.java:55`.

**Nguyên tắc:** giữ nguyên **hình dạng response** (vẫn trả mảng) để không phá hợp đồng API và frontend; chỉ đẩy việc lọc xuống DB và giới hạn số bản ghi.

**17.1 Khách hàng** — `crm/infrastructure/CustomerRepository.java`, thay `findAllByBranchIdOrNull` bằng query có tìm kiếm:
```java
    @org.springframework.data.jpa.repository.Query("""
            SELECT c FROM Customer c
            WHERE (:branchId IS NULL OR c.branchId IS NULL OR c.branchId = :branchId)
              AND c.isDeleted = false
              AND (:search IS NULL OR :search = ''
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR c.phone LIKE CONCAT('%', :search, '%'))
            ORDER BY c.name ASC
            """)
    java.util.List<Customer> search(@Param("branchId") Long branchId,
                                    @Param("search") String search,
                                    org.springframework.data.domain.Pageable pageable);
```
`CustomerReadController.getCustomers` gọi:
```java
        Long branchId = AuthUtils.getBranchIdOrNull();
        var page = org.springframework.data.domain.PageRequest.of(0, 50);   // combobox chi can 50 dong dau
        List<CustomerResponseDto> result = customerRepository.search(branchId, search, page)
                .stream().map(CustomerResponseDto::fromEntity).toList();
```
**Chú ý:** `isDeleted` hiện đang lọc trong Java — đã chuyển vào query, nhớ xoá `.filter(c -> !c.isDeleted())`.

**17.2 Sản phẩm** — thêm vào `catalog/infrastructure/ProductRepository.java`:
```java
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category"})
    @org.springframework.data.jpa.repository.Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND (:search IS NULL OR :search = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.name ASC
            """)
    java.util.List<Product> search(@Param("search") String search,
                                   org.springframework.data.domain.Pageable pageable);
```
`ProductReader` thêm `searchProducts(String search, Long branchId, boolean withBranchPrice)` dùng query trên, vẫn gắn giá theo chi nhánh như `getAllProductsWithBranchPrice` đang làm (giữ nguyên phần batch `priceListRepository.findByBranchIdAndProductIdIn` để tránh N+1). `ProductReadController.getAllProducts` chuyển sang gọi hàm mới, bỏ `.filter(...)` trong Java.
**Chú ý cache:** `@Cacheable(value="products")` chỉ được đặt trên hàm **không** tham số tìm kiếm/chi nhánh. Không cache hàm mới.

**17.3 Tồn kho, công nợ, chi nhánh:** thêm `Pageable` mặc định `PageRequest.of(0, 500)` cho `StockService.getStockByBranch` (khi `branchId == null`), `ReceivableDebtController` (khi lấy tất cả), `BranchService.getAllBranches` giữ nguyên (`branch` là bảng nhỏ) nhưng thêm `ORDER BY code`.

**17.4 Frontend:** không đổi gì (response vẫn là mảng). Kiểm tra lại combobox khách hàng/sản phẩm vẫn hoạt động khi gõ tìm kiếm.

**Test:** `CustomerCrudTest`, `ProductReaderTest`, `SupplierBranchScopeTest` xanh; thêm test: tạo 60 khách, gọi search rỗng → trả tối đa 50; search theo tên → khớp không phân biệt hoa thường.

**Commit:** `perf(api): loc va gioi han ban ghi o tang DB thay vi loc trong bo nho`

---

## Step 18 — [C3, C4, C5] Hợp đồng API và type dùng chung

**18.1 [C3] Chống lệch `openapi.json`.** File này đang được cập nhật thủ công. Thêm test bảo vệ — tạo `src/test/java/com/storename/erp/OpenApiContractTest.java`:
```java
package com.storename.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Bao ve hop dong API: moi endpoint trong code phai co trong packages/api-contract/openapi.json.
 * Neu test do, chay lai: curl http://localhost:8080/v3/api-docs -o packages/api-contract/openapi.json
 * roi `npm run generate` trong packages/api-contract.
 */
@SpringBootTest(properties = { "instance.role=ALL" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void openapiSnapshotChuaDuMoiEndpoint() throws Exception {
        String live = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Path snapshot = Path.of("..", "..", "packages", "api-contract", "openapi.json");
        String committed = Files.readString(snapshot);

        Set<String> livePaths = extractPaths(live);
        Set<String> committedPaths = extractPaths(committed);
        Set<String> missing = livePaths.stream().filter(p -> !committedPaths.contains(p)).collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), "openapi.json thieu cac endpoint: " + missing);
    }

    private Set<String> extractPaths(String json) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        Set<String> out = new java.util.HashSet<>();
        root.path("paths").fieldNames().forEachRemaining(p ->
                root.path("paths").path(p).fieldNames().forEachRemaining(m -> out.add(m.toUpperCase() + " " + p)));
        return out;
    }
}
```
**Lưu ý:** đường dẫn tương đối tính từ thư mục làm việc của Maven (`services/erp-backend`). Nếu sai, dùng `System.getProperty("user.dir")` để dò và sửa cho đúng — **không** hardcode đường dẫn tuyệt đối.

**18.2 [C4] Xoá DTO trùng tên.** `inventory/application/dto/InboundReceiptResponseDto.java` **không được dùng ở đâu** (controller dùng bản `inventory/api/dto/`). Kiểm tra lại rồi xoá:
```bash
grep -rn "application.dto.InboundReceiptResponseDto" code/erp-platform/services/erp-backend/src   # phải rỗng
git rm code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/inventory/application/dto/InboundReceiptResponseDto.java
```

**18.3 [C5] Một nguồn sự thật cho type ở frontend.** Hiện có cả `@erp/api-contract` (sinh tự động) lẫn `src/types/*.ts` viết tay (`catalog.ts`, `sales.ts`, `api/customer.ts`) cho cùng khái niệm — đây chính là nguồn gốc lỗi kiểu ở Step 15.
**Thao tác:** với từng file trong `src/types/`:
- Nếu type chỉ mô tả dữ liệu API → xoá, thay bằng `components['schemas']['XxxDto']` từ `@erp/api-contract`. Có thể tạo alias tập trung trong `src/types/api.ts`:
  ```ts
  import type { components } from '@erp/api-contract';
  export type Customer = components['schemas']['CustomerResponseDto'];
  export type Product = components['schemas']['ProductResponseDto'];
  export type SalesInvoice = components['schemas']['SalesInvoiceResponseDto'];
  ```
- Nếu type chỉ dùng cho state UI (không phải dữ liệu API) → giữ, nhưng đổi tên có hậu tố `...ViewModel` để phân biệt.
Sau đó cập nhật import ở các component và chạy lại `npm run typecheck`.

**Commit:** `chore(contract): them test bao ve openapi, xoa DTO trung va thong nhat type frontend`

---

## Step 19 — [D1–D5] Dọn dẹp mã nguồn

**19.1 Xoá 17 script rác đã commit** ở `apps/erp-frontend/`:
```bash
cd code/erp-platform/apps/erp-frontend
git rm apply_classic_erp.js apply_classic_erp_grid.js apply_footer.js apply_ux_ui.js apply_ux_ui_safe.js \
       build_ribbon.js compare.js fix_footer.js fix_label.js fix_sidebar.js fix_table.js fix_tests.js \
       rebuild_final_form.js repair.js repair2.js test.js ui_ux_promax.js
```
**Trước khi xoá**, kiểm tra không file nào được tham chiếu: `grep -rn "apply_ux_ui\|build_ribbon\|repair2" --include=*.json --include=*.ts --include=*.tsx .` phải rỗng.

**19.2 Xoá component trùng:** `src/layout/MdiModuleLayout.tsx` không được import ở đâu (bản đang dùng là `src/components/layout/MdiModuleLayout.tsx`). Xác nhận rồi xoá:
```bash
grep -rn "from '.*\.\./layout/MdiModuleLayout'" src   # xem import nào trỏ tới đâu
git rm src/layout/MdiModuleLayout.tsx
rmdir src/layout 2>/dev/null || true
```

**19.3 `src/shared/components/RoleGuard.tsx` chưa dùng ở đâu.** Chọn 1 trong 2:
- **Khuyến nghị**: dùng nó thật — bọc các phần chỉ dành cho ADMIN trong `TopRibbon.tsx`/`App.tsx` bằng `<RoleGuard allow={['ADMIN']}>...</RoleGuard>` thay cho các `if (user.role === ...)` rải rác.
- Hoặc xoá file nếu không định dùng.

**19.4 Bỏ tên class đầy đủ trong thân hàm.** Ở `order/application/SalesInvoiceService.java`, `order/application/GoodsReturnService.java`, `crm/api/ReceivableDebtController.java` có rất nhiều `com.storename.erp.…` viết thẳng trong code. Chuyển thành `import` ở đầu file (IDE: Optimize Imports). Không đổi logic.

**19.5 Thêm `.gitattributes` ở gốc repo** để chấm dứt ~300 file luôn "modified" vì CRLF/LF:
```gitattributes
* text=auto eol=lf
*.bat text eol=crlf
*.cmd text eol=crlf
*.ps1 text eol=crlf
*.pem binary
*.png binary
*.jpg binary
*.xlsx binary
```
Sau đó chuẩn hoá lại index:
```bash
git add --renormalize .
git status   # xem lai truoc khi commit
```
**Cảnh báo:** việc này tạo một commit lớn chạm nhiều file — commit **riêng**, không trộn với thay đổi logic.

**Commit:** 3 commit riêng: `chore: xoa script rac o frontend`, `chore: xoa component va DTO trung lap`, `chore: them .gitattributes va chuan hoa line ending`

---

## Step 20 — [E2, E4, E5] CI và chiến lược test

**20.1 [E2] Tạo pipeline CI.** Tạo `.github/workflows/ci.yml` ở **gốc repo**:
```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: code/erp-platform/services/erp-backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Build & test
        run: ./mvnw -B verify

  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: code/erp-platform
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
      - run: npm install
      - run: npm run lint --workspace=apps/erp-frontend
      - run: npm run typecheck --workspace=apps/erp-frontend
      - run: npx vitest run --workspace=apps/erp-frontend
```
Nếu dự án không dùng GitHub thì tạo script tương đương `code/scripts/ci-local.sh` chạy đủ 4 bước trên và ghi vào README cách chạy trước khi commit.

**20.2 [E4] Bổ sung test chạy trên Postgres thật.** Hiện phần lớn test dùng H2 (`ddl-auto: create-drop`, `flyway.enabled=false`) trong khi production là Postgres + Flyway `validate`; đã có sẵn `application-postgres-it.yml` + Testcontainers trong `pom.xml`.
**Thao tác:** viết `src/test/java/com/storename/erp/order/integration/SalesInvoiceFlowPostgresIT.java` dùng `@Testcontainers` + `PostgreSQLContainer:15`, profile `postgres-it`, chạy **toàn bộ Flyway** (`spring.flyway.enabled=true`) rồi kiểm tra kịch bản: tạo hoá đơn → `stock_on_hand` giảm đúng → `receivable_debt` tăng đúng → `receivable_debt_movement` có đúng 1 dòng `INVOICE`.
Đây cũng là bài test bảo vệ migration V19/V20/V21/V22 thật sự chạy được trên Postgres.

**20.3 [E5] Đo coverage.** Thêm plugin JaCoCo vào `pom.xml`:
```xml
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.12</version>
        <executions>
          <execution><goals><goal>prepare-agent</goal></goals></execution>
          <execution><id>report</id><phase>verify</phase><goals><goal>report</goal></goals></execution>
        </executions>
      </plugin>
```
Chạy `./mvnw verify` → báo cáo ở `target/site/jacoco/index.html`. Ghi mức coverage hiện tại vào `code/docs/testing/coverage-matrix.md` (không đặt ngưỡng fail build ở sprint này).

**Commit:** `ci: them pipeline, integration test Postgres va do coverage`

---

# PHẦN IV — TÀI LIỆU (docs phải sửa theo code)

## Step 21 — [F1–F7] Cập nhật tài liệu cho khớp code

Sửa từng file dưới đây. **Nguyên tắc: code là chuẩn, docs sửa theo.**

**F1 — `code/docs/architecture/SECURITY_MODEL.md` (khoảng dòng 14–20).**
Hiện ghi `sub / subject = username`. Sửa thành:
```markdown
JWT claims được dùng hiện tại:

| Claim | Ý nghĩa |
|---|---|
| `sub` | `user_account.public_id` (UUID) — KHÔNG phải username, cũng không phải id số. Xem migration `V19__add_public_id_to_user_account.sql`. Các cột audit (`created_by`, `updated_by`, `confirmed_by`) đều là UUID và nhận giá trị này. |
| `role` | `ADMIN` hoặc `STAFF` |
| `branchId` | Id SỐ của chi nhánh (`branch.id`), null với ADMIN ở HQ |
| `tokenId` | Id phiên, dùng chung cho cặp access/refresh token để thu hồi |
| `type` | `access` hoặc `refresh` |
```
Bổ sung mục về thu hồi token (kết quả Step 11): access token sống 15 phút; HQ kiểm tra danh sách thu hồi trong Redis; Branch không kiểm tra được vì không có Redis.

**F2 — Đơn bán hàng không còn 2 bước Draft → Confirm.** Sửa 4 chỗ:
- `code/docs/architecture/ARCHITECTURE_DECISIONS.md` mục **ADR-07 (dòng ~73)**: ghi rõ ADR-07 đã được thay đổi bởi Sprint 37 — Sales Invoice tạo và xác nhận trong **một** transaction (`SalesInvoiceService.createAndConfirm`), không còn bản DRAFT trung gian; Inbound Receipt và Goods Return **vẫn** giữ 2 bước. Giữ lại phần lịch sử và thêm mục "Cập nhật 2026-09-17".
- `code/docs/architecture/SYSTEM_ARCHITECTURE.md` (dòng ~39): sửa mô tả "Draft & Confirm" cho đúng từng nghiệp vụ.
- `code/docs/architecture/API_CONTRACT.md` (dòng ~54): đổi nhãn `POST /api/v1/sales-invoices` từ "Draft Invoice" thành "Tạo và xác nhận hoá đơn (trừ kho + ghi công nợ ngay)". Giữ nguyên nhãn Draft cho `/inventory/inbound` (dòng ~48) và `/goods-returns` (dòng ~56).
- `code/docs/BUSINESS_ANALYSIS.md` (dòng ~138) và `BUSINESS_ANALYSIS_MODIFIED.md` (dòng ~133): sửa sơ đồ trạng thái của hoá đơn bán hàng.

**F3 — Sai tên annotation.** `code/docs/architecture/SECURITY_MODEL.md` (dòng ~78–79) và `code/docs/architecture/DATABASE_REPLICATION.md` (dòng ~48) ghi `@ConditionalOnProperty(name="instance.role", havingValue="BRANCH")`. Code thực tế dùng:
```java
@ConditionalOnExpression("'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")
```
Sửa lại cho đúng và giải thích: giá trị `ALL` (profile `local`) bật cả hai nhóm controller.

**F4 — `code/docs/sprints/sprint-39-sales-order-workflow-fixes.md`.** Mục 2 tuyên bố "vô hiệu hoá ô chiết khấu/thuế" — thực tế chưa làm, và Step 7 của kế hoạch này đã triển khai đầy đủ thay vì vô hiệu hoá. Thêm một dòng đính chính ở cuối file:
```markdown
> **Đính chính (2026-09-18):** mục "vô hiệu hoá ô chiết khấu/thuế" không được thực hiện trong Sprint 39.
> Sprint 40 đã triển khai đầy đủ chiết khấu/thuế từ DB đến UI (xem `docs/plans/full-remediation-plan-2026-09-18.md`, Step 7).
```

**F5 — `code/docs/architecture/DATA_SCHEMA.md`.** Bổ sung:
- `user_account.public_id UUID NOT NULL UNIQUE` (V19) — dùng làm `sub` của JWT và giá trị cho mọi cột audit UUID.
- Quy ước `customer.branch_id IS NULL` = khách dùng chung mọi chi nhánh (V20 + `CustomerRepository.search`).
- `branch.internal_url` là URL cổng SPA của chi nhánh mà trình duyệt truy cập được (V20), dùng để chặn STAFF đăng nhập nhầm cổng.
- `sales_invoice.discount_amount`, `sales_invoice.tax_amount` (V21) và công thức `total_amount = SUM(line_total) - discount_amount + tax_amount`.
- `idempotency_record` có index `created_at` và được job dọn sau 7 ngày (V22).

**F6 — `code/docs/development/RUNTIME_CONFIG.md`.** Thêm mục "Cổng và cách truy cập":
```markdown
| Cổng | Dịch vụ | Dành cho |
|---|---|---|
| 80  | erp-frontend + Nginx → hq-app | ADMIN (master data, báo cáo) |
| 81  | branch-tp1-nginx (SPA + gateway) | STAFF chi nhánh TP1 |
| 82  | branch-tp2-nginx | STAFF chi nhánh TP2 |
| 8080 | hq-app (REST) | debug trực tiếp |
| 8081 | branch-tp1-app (REST) | debug trực tiếp |

Nginx của chi nhánh định tuyến: `/api/v1/auth`, `/api/v1/branches`, `/api/v1/analytics`, `/api/v1/admin`
và các thao tác GHI master data → HQ; phần còn lại → backend chi nhánh.
STAFF đăng nhập sai cổng sẽ bị từ chối (so sánh `branch.internal_url` với origin hiện tại).
Dev không Docker: `VITE_ENFORCE_BRANCH_URL=false npm run dev`.
```

**F7** đã nằm trong F2 (API_CONTRACT.md).

**Commit:** `docs: cap nhat tai lieu cho khop code sau dot remediation`

---

## Step 22 — Sprint doc, kiểm thử tổng thể và bàn giao

**22.1 Viết sprint doc** `code/docs/sprints/sprint-40-full-remediation.md` theo đúng khuôn mẫu bắt buộc của `.agents/rules/Sprint-docs-rule.md` với 8 mục: Mục tiêu Sprint · Thành quả đạt được · Quyết định kiến trúc & Lý do (ADR rút gọn) · Hướng dẫn đọc code theo thứ tự · Rủi ro / Nợ kỹ thuật đã biết · Việc chưa làm / Out of scope · Cách chạy & Cách verify · Điểm nối cho Sprint tiếp theo.
Sau đó thêm một dòng vào index `code/docs/sprints/README.md` theo đúng định dạng các dòng gần nhất:
```
- Sprint 40: [Full Remediation sau code review](sprint-40-full-remediation.md) - 2026-09-18 - Sửa toàn bộ phát hiện trong code-review-2026-09-18.
```
**Lưu ý kỹ thuật:** `code/docs/sprints/README.md` có thể đang ở encoding **UTF-16 LE** — đọc/ghi phải giữ nguyên encoding, đừng ghi đè bằng UTF-8.

**22.2 Kiểm thử tổng thể (bắt buộc, làm đủ trước khi báo hoàn thành):**
```bash
# 1. Backend
cd code/erp-platform/services/erp-backend && ./mvnw clean verify

# 2. Frontend
cd code/erp-platform/apps/erp-frontend && npm run lint && npm run typecheck && npx vitest run

# 3. E2E
npx playwright test

# 4. Hệ thống thật
cd code && docker compose up -d --build
bash scripts/setup-replication.sh tp1
```

**22.3 Kịch bản thủ công (STAFF, `http://localhost:81`, tài khoản `staff_tp1`):**
1. Đăng nhập tại `http://localhost` bằng tài khoản STAFF → **bị từ chối** kèm thông báo chỉ ra cổng đúng.
2. Đăng nhập tại `http://localhost:81` → vào thẳng form Bán hàng.
3. Chọn khách hàng `KH-001` → hiện "Nợ cũ" (ghi lại giá trị **X**).
4. Thêm dòng hàng: sản phẩm id 1, số lượng 2 → Đơn giá tự điền từ bảng giá chi nhánh (ghi lại tồn kho trước **Y**).
5. Nhập Chiết khấu 50.000, VAT 20.000, chọn hình thức thanh toán `CREDIT`.
6. Bấm **Lưu** → thành công, mã `HD…` xuất hiện, form chuyển sang chế độ xem; **không** thấy nút Xóa.
7. Kiểm tra DB chi nhánh:
   ```sql
   -- docker exec code-branch-tp1-db-1 psql -U erp_user -d erp_branch_tp1 -c "..."
   SELECT invoice_code, status, discount_amount, tax_amount, total_amount,
          previous_debt, remaining_debt, payment_method, created_by, confirmed_by
     FROM sales_invoice ORDER BY created_at DESC LIMIT 1;
   -- ky vong: CONFIRMED, total = SUM(line_total) - 50000 + 20000, created_by/confirmed_by la UUID

   SELECT quantity FROM stock_on_hand WHERE product_id = 1 AND branch_id = 1;   -- = Y - 2
   SELECT movement_type, quantity, created_by FROM stock_movement ORDER BY created_at DESC LIMIT 1; -- SALE, -2, created_by khac NULL
   SELECT total_debt FROM receivable_debt WHERE branch_id = 1
     AND customer_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';                  -- = X + total_amount
   ```
8. Gọi `curl -X POST http://localhost:8080/api/v1/sales-invoices` → **404** (HQ không có API giao dịch).
9. Đăng nhập ADMIN ở `http://localhost` → mở Tồn Kho, Công nợ: dữ liệu hiển thị bình thường (đọc DB HQ).
10. Đăng xuất ở cổng 81, dùng lại access token cũ bằng `curl` → **401**.

**22.4 Báo cáo cuối cùng cho người dùng** gồm: danh sách Step đã làm, Step nào bỏ qua và vì sao, kết quả 4 lệnh kiểm thử ở 22.2, và mọi lỗi còn lại chưa xử lý được.

---

# PHỤ LỤC A — BẢNG TRA CỨU NHANH (mã lỗi → Step)

| Mã | Mô tả ngắn | Step |
|---|---|---|
| A1 | Private key JWT bị commit | 1 |
| A2 | Access token không thu hồi được | 11 |
| A3 | Token ADMIN lọt vào instance Branch | 9 |
| A4 | BranchController thiếu `@PreAuthorize` | 10 |
| A5 | 6 endpoint ghi thiếu `@Valid` | 10 |
| A6 | Token trong localStorage | 11 (ghi chú rủi ro) + F1 |
| A7 | Credential DB hardcode | 20 (ghi vào docs vận hành) |
| B1 | `@Scheduled` không chạy | 3 |
| B2 | `@Cacheable` vô hiệu | 4 |
| B3 | Idempotency kẹt `IN_PROGRESS`, bảng phình | 12 |
| B4 | Lỗi nghiệp vụ trả 500 | 5 |
| B5 | `branchId = null` không nhất quán | 9 |
| B6 | Vi phạm rules-of-hooks | 13 |
| B7 | Nút Xóa không xoá gì | 6 |
| B8 | Chiết khấu/thuế không gửi lên backend | 7 |
| B9 | Không chọn được hình thức thanh toán | 8 |
| B10 | tsconfig sai, không type-check | 15 |
| B11 | Không phân trang, lọc trong bộ nhớ | 17 |
| B12 | Chính sách retry không nhất quán | 14 |
| B13 | Tạo `stock_on_hand` lần đầu có thể va unique | 14 |
| B14 | Toast trùng lặp | 13 |
| C3 | `openapi.json` dễ lệch | 18.1 |
| C4 | DTO trùng tên | 18.2 |
| C5 | Hai nguồn type ở frontend | 18.3 |
| D1 | 17 script rác | 19.1 |
| D2 | Component trùng | 19.2 |
| D3 | `RoleGuard` không dùng | 19.3 |
| D4 | Tên class đầy đủ trong thân hàm | 19.4 |
| D5 | Thiếu `.gitattributes` | 19.5 |
| E1 | Test không chạy trên máy sạch | 2 |
| E2 | Chưa có CI | 20.1 |
| E3 | ESLint bị hạ cấp | 16 |
| E4 | Test H2 ≠ production Postgres | 20.2 |
| E5 | Không đo coverage | 20.3 |
| F1–F7 | Docs lệch code | 21 |

# PHỤ LỤC B — MIGRATION MỚI TRONG KẾ HOẠCH NÀY

| File | Nội dung | Step |
|---|---|---|
| `V21__add_discount_tax_to_sales_invoice.sql` | Thêm `discount_amount`, `tax_amount` | 7.1 |
| `V22__index_idempotency_created_at.sql` | Index `created_at` cho `idempotency_record` | 12.4 |

**Lưu ý về replication:** khi đã bật replication HQ ↔ Branch, mọi thay đổi DDL phải chạy ở **Branch trước, HQ sau** (logical replication không replicate DDL; subscriber thiếu cột sẽ làm hỏng luồng apply).

# PHỤ LỤC C — NHỮNG THỨ ĐÃ KIỂM TRA VÀ KHÔNG CẦN SỬA

Không tốn thời gian "sửa" các mục sau, chúng đã đúng:
1. Ánh xạ entity ↔ schema: quét toàn bộ 29 bảng, không lệch cột. `ddl-auto: validate` đang bảo vệ.
2. Endpoint code ↔ `openapi.json`: khớp 47/47 (nhưng cần test bảo vệ — Step 18.1).
3. Không có SQL nối chuỗi: `AnalyticsDataAdapter` dùng `NamedParameterJdbcTemplate` đúng cách.
4. Không có `catch` rỗng, không có `@Transactional` trên method private, không có self-invocation làm mất transaction.
5. Ranh giới transaction ở nghiệp vụ tiền/kho đúng: `createAndConfirm` gói trọn tạo hoá đơn + trừ kho + ghi công nợ; `FifoCostService.consume` dùng `Propagation.MANDATORY`; có `PESSIMISTIC_WRITE` ở `CostLayerRepository` và `SalesInvoiceRepository.findByIdForUpdate`.
6. Sổ công nợ append-only có khoá idempotency (`uk_debt_movement_idempotency`).
7. Việc cho phép **âm kho** là quyết định nghiệp vụ đã chốt — **không** được "sửa".
