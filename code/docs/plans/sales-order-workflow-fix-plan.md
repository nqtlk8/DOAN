# Workflow Tạo Đơn Bán Hàng — Context, Debug & Kế hoạch sửa (dành cho Gemini)

- Ngày: 2026-09-17 · Người viết: Claude (phân tích từ source, chưa sửa code trong tài liệu này)
- Đọc kèm: `.agents/AGENTS.md`, `.agents/rules/strict-debugging-circuit-breaker.md`, `.agents/rules/Sprint-docs-rule.md`
- Triệu chứng người dùng báo (test thực tế tại `http://localhost`):
  ```
  GET  http://localhost/api/v1/sales-invoices 404 (Not Found)
  POST http://localhost/api/v1/sales-invoices 404 (Not Found)
  ApiError: Resource not found
  ```

---

## PHẦN A — CONTEXT: workflow hiện tại (trạng thái source ngày 2026-09-17)

### A.1 Kiến trúc triển khai (`code/docker-compose.yml`)
| Service | Cổng host | Vai trò | Ghi chú |
|---|---|---|---|
| `hq-app` | 8080 | Spring profile `hq`, `instance.role: HQ` | Master data, cấp JWT (có private key + Redis) |
| `branch-tp1-app` | 8081 | profile `branch`, `INSTANCE_ROLE=BRANCH`, `BRANCH_ID=1` | Dữ liệu giao dịch TP1, DB `erp_branch_tp1` (5433) |
| `branch-tp2-app` | – | profile `branch`, `BRANCH_ID=2` | DB `erp_branch_tp2` (5434) |
| `erp-frontend` | **80** | SPA + Nginx `apps/erp-frontend/nginx.conf` | **Mọi `/api/` → `hq-app`** (cổng dành cho ADMIN) |
| `branch-tp1-nginx` | **81** | cùng image SPA + `code/nginx-branch-tp1.conf` | auth / branches / analytics / admin / GHI master → HQ; còn lại → branch-tp1 |
| `branch-tp2-nginx` | **82** | như trên cho TP2 | |

- Controller giao dịch có điều kiện `instance.role == BRANCH || ALL`: `SalesInvoiceController`, `GoodsReturnController`, `InboundReceiptController` → **không tồn tại trên HQ** (HQ trả 404, có test `ReplicationOwnershipHqTest` khẳng định điều này).
- Controller đọc không có điều kiện (chạy cả HQ lẫn Branch): `CustomerReadController`, `ProductReadController`, `ReceivableDebtController`, `StockController`, `StockMovementController`, `CustomerProductPriceController`.
- Controller chỉ có ở HQ: `AuthController`, `BranchController`, `DashboardController`, `ProductWriteController`, `SupplierWriteController`, `CustomerWriteController`.
- Replication (logical, 2 chiều) **không tự bật**, phải chạy tay `code/scripts/setup-replication.sh tp1`:
  - HQ → Branch: `branch, category, product, supplier, price_list, customer, role, permission, role_permission, user_account, user_branch_role, inventory_alert_config, dim_date`
  - Branch → HQ: `sales_invoice(_line), goods_return(_line), inbound_receipt(_line), stock_movement, cost_layer`
  - KHÔNG replicate: `receivable_debt`, `receivable_debt_movement`, `stock_on_hand`, `idempotency_record`.
- Mỗi DB branch tự chạy toàn bộ Flyway `db/migration` (kể cả seed V2/V13) + `db/migration-branch/V9` (REVOKE INSERT/UPDATE/DELETE trên `branch, category, product, price_list, customer` với `erp_user`).

### A.2 Đăng nhập
1. `Login` → `AuthContext.login()` → `axios.post('/api/v1/auth/login')` (không qua interceptor) → HQ `AuthService.login`.
2. JWT: `sub = user_account.public_id` (UUID, migration V19), claim `role` (`STAFF`/`ADMIN`), `branchId` (id SỐ của branch, vd `"1"`), `tokenId`, `type`.
3. Response có `branchUrl` = `branch.internal_url` (seed: `http://branch-tp1.local:80`) — **đang bị bỏ qua** (đoạn kiểm tra trong `AuthContext.tsx` bị comment "DISABLED FOR LOCAL TESTING").
4. Lưu `user`, `access_token`, `refresh_token` vào `localStorage` (theo origin → cổng 80 và 81 là 2 phiên khác nhau).
5. `App.tsx`: STAFF → tự mở tab `SalesModule` (mode ADD). ADMIN → Dashboard. Ribbon "Chức năng/Bán hàng" chỉ hiện cho STAFF (`config/menuConfig.ts`).

### A.3 Mỗi request API (`api/axiosInstance.ts`)
- Gắn `Authorization: Bearer`, gắn `Idempotency-Key` cho POST/PUT (cache 5s theo method+url+payload).
- 401 → thử `/api/v1/auth/refresh` → thất bại thì xoá phiên và về trang login. 403/404/500 → chỉ toast.
- Backend `JwtAuthenticationFilter`: ở BRANCH, claim `branchId` phải bằng `${branch-id:${instance.branch-id:}}` (env `BRANCH_ID`), sai → 403 "Branch isolation violation".

### A.4 Form tạo đơn (`components/sales/SalesOrderForm.tsx`, `SalesModule.tsx`, `common/document/GenericDocumentForm.tsx`)
1. **Chọn khách hàng**: `SearchableCombobox` → `ApiService.Catalog.searchCustomers(q)` → `GET /api/v1/customers?search=q`
   → `CustomerReadController` → `customerRepository.findAllByBranchIdOrNull(branchIdTrongToken)` = `WHERE :branchId IS NULL OR c.branchId = :branchId`.
   Khi chọn: set `customerId = customer.id`, tên, địa chỉ, SĐT; gọi `GET /api/v1/receivable-debts/{id}/balance` → `oldDebt`.
2. **Chọn sản phẩm**: `sales-add-line` thêm dòng (qty 1, price 0) → combobox → `ApiService.Catalog.searchProducts(q)` → `GET /api/v1/catalog/products?search=q` (**không** truyền `withBranchPrice`)
   → `ProductReader.getAllProducts()` → DTO **không có `price`**.
   Khi chọn: `productId`, `productName`, `unitPrice = product.price` (→ `undefined`), `unitOfMeasure = product.baseUnit || 'CAI'`.
3. UI tính: `totalAmount = Σ qty*price`, `finalAmount = total - discount + tax`, `remainingBalance = oldDebt + finalAmount - advancePayment`.
4. **Bấm Lưu** (`MdiModuleLayout.onSave` → `handleSubmit`): validate khách hàng + ≥1 dòng + productId + qty>0 → payload
   `{ customerId, paymentMethod, note, advancePayment, lines:[{productId:Number, productName, quantity, unitPrice, unitOfMeasure}] }`
   (**không** gửi `discount`, `tax`) → `POST /api/v1/sales-invoices`.
5. Thành công → `setOrderCode`, `setCurrentInvoiceId`, `setInvoiceStatus('CONFIRMED')`, mode `VIEW`. Toast từ `useSalesInvoice.createMutation`.
6. Nút "Xác nhận" (F9) chỉ dùng cho đơn DRAFT cũ (`POST /{id}/confirm`), đơn mới đã CONFIRMED sẵn.

### A.5 Backend tạo đơn (`order/api/SalesInvoiceController.java`, `order/application/SalesInvoiceService.java`)
`POST /api/v1/sales-invoices` (`@PreAuthorize STAFF`, `@IdempotencyProtected`)
→ `branchId = AuthUtils.getBranchIdOrNull()`, `userId = AuthUtils.getUserId()` (UUID từ `sub`, sai → 403)
→ `createAndConfirm(dto, branchId, userId)` — **một** `@Transactional` (+ `@Retryable`):
1. `buildInvoice`: header + lines, `lineTotal = qty * unitPrice`, `totalAmount = Σ lineTotal`, validate `0 ≤ advancePayment ≤ total`.
2. `invoiceRepository.save` (lấy UUID cho invoice + lines).
3. `applyConfirmationEffects`:
   - Mỗi dòng: `inventoryFacade.recordSaleAndGetCost(productId, branchId, qty, invoiceId, lineId, **null**)` → FIFO cost, `stock_on_hand.decreaseAllowNegative` (cho phép âm kho), ghi `stock_movement`.
   - `debtService.getCurrentDebt` → `increaseDebt(total, INVOICE)` → nếu có trả trước: `decreaseDebt(advance, PAYMENT)`.
     `processDebtChange` → nếu chưa có `receivable_debt` thì `createDebtRecord` → `customerRepository.findById` → **không có → `RuntimeException("Customer not found")` (HTTP 500)**.
   - `snapshotDebt(previous, remaining)`, `confirm(userId)` → status `CONFIRMED`.
4. `save` lần 2 → commit. Lỗi ở bất kỳ bước nào → rollback toàn bộ.

---

## PHẦN B — DEBUG: các lỗi trong workflow

> Quy ước mức độ: 🔴 chặn luồng · 🟠 sai dữ liệu/sai kết quả · 🟡 UX/bảo trì

### 🔴 BUG-A — 404 khi test tại `http://localhost` (lỗi người dùng đang gặp)
- **Nguyên văn:** `POST http://localhost/api/v1/sales-invoices 404` → `ApiError: Resource not found`.
- **Lớp lỗi:** `config-env`.
- **Bằng chứng:** cổng 80 là `erp-frontend` → `nginx.conf` proxy mọi `/api/` về `hq-app`; `application-hq.yml` `role: HQ` → `SalesInvoiceController` không được tạo (điều kiện `BRANCH || ALL`) → Spring trả 404 (`NoResourceFoundException` → "Resource not found").
- **Bản chất:** không phải bug backend mà là **STAFF đang dùng cổng của ADMIN**. Cổng đúng cho STAFF TP1 là `http://localhost:81`. Hệ thống **không chặn và không báo** điều này: đăng nhập STAFF ở cổng 80 vẫn thành công và tự mở form bán hàng, vì kiểm tra `branchUrl` trong `AuthContext.tsx` đang bị comment, còn `branch.internal_url` seed (`http://branch-tp1.local:80`) không phải URL trình duyệt truy cập được.
- **Lịch sử:** commit `8ab2632` từng "sửa" 404 bằng cách cho HQ chạy `ALL` → đơn bị ghi vào DB HQ (sai kiến trúc). Sprint 38 đã trả HQ về `HQ` nhưng chưa làm phần chặn/hướng dẫn ở frontend.

### 🔴 BUG-B — Sản phẩm không có giá → Lưu bị 400 "Unit price is required"
- **Bằng chứng:** `ApiService.Catalog.searchProducts` gọi `/api/v1/catalog/products?search=` không có `withBranchPrice=true` → `ProductReader.mapToResponse` không set `price` → `unitPrice = undefined` → JSON bỏ field → `SalesInvoiceLineDto.unitPrice @NotNull` → 400 (trừ khi người dùng tự gõ giá vào ô Đơn giá). Cột "Giá" trong combobox cũng hiển thị 0.
- **Lớp lỗi:** `logic` (frontend không dùng đúng API contract).
- Phụ: `price_list` seed chỉ có giá cho branch 1 → TP2 luôn không có giá.

### 🔴 BUG-C — Khách hàng không hiện ở chi nhánh / không ghi được công nợ
1. `CustomerWriteService` chỉ set `branchId` nếu DTO có; `CustomerList.tsx` (admin) **không gửi `branchId`** → khách tạo ở HQ có `branch_id = NULL` → query branch `c.branchId = :branchId` **loại bỏ** → STAFF không tìm thấy.
2. `V16__add_branch_id_to_customer.sql` đặt `branch_id = 1` cho mọi khách seed **ở mọi DB** → STAFF TP2 (`branchId=2`) không thấy khách nào.
3. Khách tạo ở HQ chỉ xuống branch khi đã chạy `setup-replication.sh`; nếu chưa, `createDebtRecord` ném `RuntimeException("Customer not found")` → **HTTP 500**, message không rõ nghĩa.
- **Lớp lỗi:** `data-state` + `config-env`.

### 🟠 BUG-D — Stock movement của đơn bán không ghi người tạo
- `SalesInvoiceService.applyConfirmationEffects` truyền `null` cho tham số `userId` của `recordSaleAndGetCost` → `stock_movement` thiếu người thao tác (các phiếu khác có).
- `SalesInvoiceServiceTest` đang khớp `isNull()` cho tham số này ở 2 test → phải cập nhật cùng lúc.

### 🟠 BUG-E — Công nợ hiển thị trên form ≠ công nợ backend ghi nhận
- UI dùng `finalAmount = total - discount + tax`, nhưng payload không có `discount`/`tax`, backend `totalAmount = Σ lineTotal`. Nếu người dùng nhập chiết khấu/thuế, "Còn nợ" trên form và bản in sai so với DB.
- **Lớp lỗi:** `logic`. Cần quyết định nghiệp vụ (xem Step 7).

### 🟠 BUG-F — Phiên đăng nhập cũ sau khi deploy V19
- Token phát hành trước V19 có `sub` là số → mọi API giao dịch trả **403** ("User identity is missing or invalid...") nhưng interceptor chỉ toast 403, không đăng xuất → người dùng kẹt. (Refresh đã bị từ chối đúng, nhưng chỉ khi access token hết hạn 30 phút.)

### 🟡 BUG-G — Thứ tự deploy với replication
- V19 thêm cột `user_account.public_id`. Logical replication không replicate DDL; nếu HQ migrate trước khi branch có cột → apply lỗi trên subscription của branch. Và V19 sinh `public_id` ngẫu nhiên **khác nhau** ở mỗi DB (không ảnh hưởng hiện tại vì branch không tra user theo `public_id`, nhưng dễ gây nhầm lẫn).

### ✅ Đã đúng (không cần sửa, chỉ cần verify)
- Create + confirm atomic (Sprint 37); `sub` UUID (Sprint 38); `BRANCH_ID` số; routing Nginx chi nhánh (đã kiểm tra 12 case bằng backend giả); HQ `role: HQ`; `IllegalArgumentException` → 400; xem lại đơn (BUG-7 Sprint 38).
- Migration cũ (V1–V18) chỉ khác CRLF so với HEAD → không lo lệch checksum Flyway.

---

## PHẦN C — KẾ HOẠCH SỬA STEP-BY-STEP (cho Gemini)

### Quy tắc bắt buộc khi thực hiện
- **Không revert** các thay đổi chưa commit của Sprint 38 (V19, `AuthUtils`, nginx chi nhánh, compose, `SalesOrderForm`...). `git status` sẽ báo ~300 file `M` do CRLF — **không** "dọn" chúng.
- Làm tuần tự, mỗi step xong chạy test liên quan rồi mới sang step sau. Tuân thủ circuit breaker (tối đa 3 vòng sửa/1 lỗi).
- Migration mới dùng UTF-8 **không BOM**, số phiên bản tiếp theo là **V20**.
- Mọi Service/Controller sửa đổi phải giữ `@Slf4j` + log theo `AGENTS.md`.

### Step 0 — Dọn môi trường
1. Xoá các file tạm: `.git/HEAD.lock.stale-claude`, `code/erp-platform/services/erp-backend/target/claude-src-snapshot.tgz`, `.../target/claude-fe-snapshot.tgz`.
2. `git status -sb` phải chạy được (không có `.git/*.lock`).
3. Chưa commit gì ở step này.

### Step 1 — BUG-A: STAFF phải dùng cổng chi nhánh, và được báo rõ nếu vào nhầm
1. Tạo `code/erp-platform/services/erp-backend/src/main/resources/db/migration/V20__branch_portal_url_and_shared_customers.sql`:
   ```sql
   -- internal_url = URL cổng SPA chi nhánh mà TRÌNH DUYỆT truy cập được (dùng để chặn STAFF đăng nhập nhầm cổng).
   -- Branch DB bị REVOKE UPDATE trên bảng branch/customer (V9) -> chỉ chạy khi có quyền (HQ); branch nhận qua replication.
   DO $$
   BEGIN
     IF has_table_privilege(current_user, 'branch', 'UPDATE') THEN
       UPDATE branch SET internal_url = 'http://localhost:81' WHERE code = 'TP1';
       UPDATE branch SET internal_url = 'http://localhost:82' WHERE code = 'TP2';
     END IF;
   END $$;
   ```
   (Phần khách hàng dùng chung được thêm ở Step 3 vào **cùng file** này.)
2. `apps/erp-frontend/src/context/AuthContext.tsx`:
   - Thay đoạn bị comment bằng kiểm tra thật:
     ```ts
     const enforceBranchUrl = import.meta.env.VITE_ENFORCE_BRANCH_URL !== 'false';
     if (parsedRole === 'STAFF' && enforceBranchUrl && authData.branchUrl) {
       const expected = new URL(authData.branchUrl).origin;
       if (expected !== window.location.origin) {
         return { success: false, message: `Tài khoản nhân viên chi nhánh phải đăng nhập tại ${expected}` };
       }
     }
     ```
     (Return **trước** khi lưu token vào `localStorage`.)
   - Lưu thêm `localStorage.setItem('branch_url', authData.branchUrl ?? '')`; khi khôi phục phiên trong `useEffect`, nếu user là STAFF, `enforceBranchUrl` bật và origin khác `branch_url` → xoá phiên (giống nhánh `else` hiện có).
   - `logout()` xoá thêm `branch_url`.
3. `src/vite-env.d.ts` **chưa có** → tạo file với `/// <reference types="vite/client" />` và khai báo `VITE_ENFORCE_BRANCH_URL?: string`.
4. Dev không Docker (`npm run dev`, origin `localhost:5173`): hướng dẫn chạy `VITE_ENFORCE_BRANCH_URL=false npm run dev`. Ghi vào `apps/erp-frontend/README.md`.
5. `apps/erp-frontend/src/components/auth/Login.tsx`: nếu message có URL, hiển thị dạng link bấm được (tuỳ chọn).
6. **Test:**
   - Vitest cho `AuthContext` (mock `ApiService.Auth.login`): STAFF + `branchUrl` khác origin → `success:false`, không có `access_token` trong `localStorage`; ADMIN → luôn thành công.
   - Cập nhật Playwright `tests/login.spec.ts` (`TC-LOGIN-02` đăng nhập STAFF) — mock `branchUrl` bằng `window.location.origin` hoặc chạy với `VITE_ENFORCE_BRANCH_URL=false`.
7. **Acceptance:** đăng nhập `staff_tp1` tại `http://localhost` → thông báo yêu cầu dùng `http://localhost:81`, không mở form bán hàng; tại `http://localhost:81` → đăng nhập bình thường.

### Step 2 — BUG-B: lấy giá theo chi nhánh khi chọn sản phẩm
1. `apps/erp-frontend/src/api/ApiService.ts` → `searchProducts`: URL `'/api/v1/catalog/products?withBranchPrice=true&search=' + encodeURIComponent(query)`.
2. `SalesOrderForm.tsx` → `renderProductCombobox.onSelect`: `updateItem(itemId, 'unitPrice', Number(product.price ?? 0));`
3. Phòng thủ trong `handleSubmit`: `unitPrice: Number(i.unitPrice) || 0`.
4. (Tuỳ chọn) `ProductReadController`/`ProductReader`: lọc sản phẩm `active = false` khỏi kết quả tìm kiếm cho STAFF.
5. **Test:** Playwright `tests/sales-create.spec.ts` — route mock `**/api/v1/catalog/products?*` đang dùng pattern `products?search=*`; cập nhật pattern cho query mới và thêm assert `capturedRequest.lines[0].unitPrice === 50000` (đã có). Backend: test `ProductReadController` với `withBranchPrice=true` trả `price` từ `price_list`.
6. **Acceptance:** chọn "Xi măng..." ở TP1 → ô Đơn giá = giá trong `price_list` (vd 185000), Lưu không bị 400.

### Step 3 — BUG-C: khách hàng dùng chung + lỗi "Customer not found" rõ ràng
1. Quy ước mới: `customer.branch_id IS NULL` = khách dùng chung mọi chi nhánh.
   `crm/infrastructure/CustomerRepository.findAllByBranchIdOrNull` đổi query:
   ```java
   @Query("SELECT c FROM Customer c WHERE :branchId IS NULL OR c.branchId IS NULL OR c.branchId = :branchId")
   ```
2. Thêm vào `V20__...sql` (trong cùng khối `DO $$`, có kiểm tra quyền `customer`):
   ```sql
   IF has_table_privilege(current_user, 'customer', 'UPDATE') THEN
     UPDATE customer SET branch_id = NULL WHERE customer_code IN ('KH-001','KH-002','KH-003');
   END IF;
   ```
3. `apps/erp-frontend/src/components/catalog/CustomerList.tsx` (form admin): thêm select "Chi nhánh" (mặc định "Dùng chung" = không gửi `branchId`), dữ liệu lấy từ `GET /api/v1/branches`.
4. `common/exception/GlobalExceptionHandler`: hiện **chưa** có handler cho `com.storename.erp.common.exception.ResourceNotFoundException` (chỉ có `NoResourceFoundException`) → thêm handler trả **404** với `ex.getMessage()`.
5. `crm/application/ReceivableDebtService.createDebtRecord`: thay `RuntimeException` bằng `ResourceNotFoundException("Khách hàng chưa có tại chi nhánh (chưa đồng bộ từ HQ): " + customerId)`.
6. Kiểm tra sớm ở đầu `SalesInvoiceService.createAndConfirm` (trước khi trừ kho): inject `CrmFacade` (đã có sẵn trong service) và gọi `crmFacade.customerExists(dto.getCustomerId())` (method **đã tồn tại**); `false` → ném `ResourceNotFoundException` như trên. Không gọi `CustomerRepository` trực tiếp từ domain `order`.
7. **Test:**
   - `CustomerCrudTest`/test repository: khách `branch_id NULL` xuất hiện cho branch 1 và 2; khách `branch_id=2` không xuất hiện cho branch 1.
   - `SalesInvoiceServiceTest`: thêm `@Mock CrmFacade crmFacade` (hiện test chưa mock, `@InjectMocks` sẽ để null → NPE); stub `customerExists → true` cho các test cũ của `createAndConfirm`; test mới: `false` → `ResourceNotFoundException`, `inventoryFacade` **không** được gọi.
   - `GlobalExceptionHandlerTest`: `ResourceNotFoundException` → 404.
8. **Vận hành:** sau `docker compose up`, chạy `bash code/scripts/setup-replication.sh tp1` (và `tp2`) để khách/sản phẩm tạo ở HQ xuống chi nhánh. Ghi rõ trong `code/docs` hướng dẫn chạy.

### Step 4 — BUG-D: ghi người tạo cho stock movement
1. `SalesInvoiceService.applyConfirmationEffects`: tham số cuối của `recordSaleAndGetCost` đổi `null` → `userId`.
2. `SalesInvoiceServiceTest`: 2 chỗ `isNull()` ở stub `recordSaleAndGetCost` đổi thành `eq(userId)` (test `confirmInvoice_...`) và `eq(userId)` (test `createAndConfirm_...`).
3. **Acceptance:** `SELECT created_by FROM stock_movement ORDER BY created_at DESC LIMIT 1;` (hoặc cột user tương ứng trong `StockMovement.sale`) khác NULL.

### Step 5 — BUG-F: tự đăng xuất khi token không hợp lệ với định danh mới
1. `AuthContext.tsx` khi khôi phục phiên: decode payload `access_token` (base64url, không cần thư viện); nếu `sub` không khớp regex UUID → xoá `user/access_token/refresh_token/branch_url`.
2. `axiosInstance.ts`: với 403 mà `normalizedError.message` bắt đầu bằng `"User identity is missing"` → xoá phiên, toast "Phiên đăng nhập cũ không còn hợp lệ, vui lòng đăng nhập lại", chuyển về `/`.
3. **Test:** Vitest cho hàm decode/kiểm tra `sub`.

### Step 6 — BUG-G: thứ tự deploy
1. Ghi vào tài liệu vận hành: khi đã bật replication, **migrate/khởi động branch trước** (`docker compose up -d branch-tp1-app branch-tp2-app`), sau đó mới `hq-app`.
2. Không sửa code. (Nếu sau này branch cần tra user theo `public_id`, thêm migration đồng bộ `public_id` từ HQ.)

### Step 7 — BUG-E: chiết khấu / thuế (CẦN HỎI NGƯỜI DÙNG TRƯỚC)
Hỏi người dùng chọn 1 trong 2, **không tự quyết**:
- **7a (nhanh):** vô hiệu hoá ô Chiết khấu/Thuế trong `GenericDocumentForm` cho form bán hàng (luôn 0) cho tới khi backend hỗ trợ.
- **7b (đầy đủ):** migration thêm `discount_amount`, `tax_amount` vào `sales_invoice`; DTO `SalesInvoiceCreateDto` + `SalesInvoiceResponseDto`; `SalesInvoice.calculateTotal()` = Σ lineTotal − discount + tax; validate `0 ≤ discount ≤ Σ lineTotal`, `tax ≥ 0`; frontend gửi 2 field; regenerate `packages/api-contract` (`openapi.json` + `npm run generate`); test công nợ tăng đúng `finalAmount`. Theo Document-Driven: viết spec vào `code/docs` trước khi code.

### Step 8 — Verify end-to-end (bắt buộc trước khi báo xong)
1. `cd code/erp-platform/services/erp-backend && .\mvnw test` → BUILD SUCCESS.
2. `cd code/erp-platform/apps/erp-frontend && npm run lint && npx vitest run && npx playwright test tests/sales-create.spec.ts`.
3. `cd code && docker compose up -d --build` (branch trước, HQ sau nếu đã có replication) → `bash scripts/setup-replication.sh tp1`.
4. Kịch bản thủ công tại **`http://localhost:81`**, user `staff_tp1`:
   1. Chọn khách `KH-001` → "Nợ cũ" hiển thị (ghi lại giá trị X).
   2. Thêm 1 dòng, chọn sản phẩm id 1 → Đơn giá tự điền; SL = 2 (ghi lại tồn kho trước: Y).
   3. Lưu → toast thành công, mã `HD…` hiển thị, form chuyển VIEW.
5. Kiểm tra DB TP1 (`localhost:5433`, `erp_branch_tp1`):
   ```sql
   SELECT invoice_code, status, total_amount, previous_debt, remaining_debt, created_by, confirmed_by
     FROM sales_invoice ORDER BY created_at DESC LIMIT 1;           -- CONFIRMED, created_by/confirmed_by là UUID
   SELECT quantity FROM stock_on_hand WHERE product_id = 1 AND branch_id = 1;   -- = Y - 2
   SELECT total_debt FROM receivable_debt
    WHERE customer_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11' AND branch_id = 1; -- = X + total_amount - advance
   SELECT type, amount, ref_type FROM receivable_debt_movement ORDER BY created_at DESC LIMIT 2;
   ```
6. Kiểm tra HQ (`localhost:5432`): **không** có dòng `receivable_debt`/`stock_on_hand` mới cho đơn này; `sales_invoice` chỉ xuất hiện nếu replication Branch→HQ đang chạy.
7. `curl -X POST http://localhost:8080/api/v1/sales-invoices` → 404. Đăng nhập `staff_tp1` tại `http://localhost` → bị chặn với thông báo cổng đúng.
8. Mở danh sách đơn → double-click đơn vừa tạo → hiển thị đúng mã, dòng hàng, nợ cũ.

### Step 9 — Tài liệu & commit
1. Viết sprint doc mới theo `.agents/rules/Sprint-docs-rule.md` (Sprint 39) trong `code/docs/sprints/`, cập nhật index README của thư mục đó.
2. Commit theo từng step (message tiếng Việt không dấu như lịch sử repo), **chỉ add đúng file đã sửa** (không `git add -A` vì nhiễu CRLF).

---

## PHỤ LỤC — Bảng file liên quan
| File | Vai trò |
|---|---|
| `code/docker-compose.yml`, `code/nginx-branch-tp{1,2}.conf`, `apps/erp-frontend/nginx.conf`, `apps/erp-frontend/vite.config.ts` | Định tuyến |
| `apps/erp-frontend/src/context/AuthContext.tsx`, `api/axiosInstance.ts`, `api/ApiService.ts` | Phiên đăng nhập & gọi API |
| `apps/erp-frontend/src/components/sales/SalesOrderForm.tsx`, `SalesModule.tsx`, `common/document/GenericDocumentForm.tsx`, `hooks/useSalesInvoice.ts` | Form tạo đơn |
| `order/api/SalesInvoiceController.java`, `order/application/SalesInvoiceService.java`, `order/application/dto/*` | API tạo đơn |
| `inventory/application/InventoryFacadeImpl.java` | Trừ kho (FIFO, stock_on_hand, stock_movement) |
| `crm/application/ReceivableDebtService.java`, `crm/api/CustomerReadController.java`, `crm/infrastructure/CustomerRepository.java` | Công nợ & khách hàng |
| `catalog/api/ProductReadController.java`, `catalog/application/ProductReader.java` | Sản phẩm & giá chi nhánh |
| `common/security/{AuthUtils,JwtAuthenticationFilter}.java`, `identity/application/AuthService.java` | Bảo mật |
| `resources/application-{hq,branch,local}.yml`, `db/migration/V19__...`, `db/migration-branch/V9__...` | Cấu hình & schema |
| `code/scripts/setup-replication.sh` | Replication HQ ↔ Branch |
