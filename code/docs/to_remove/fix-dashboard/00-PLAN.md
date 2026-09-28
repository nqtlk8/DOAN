# PLAN — Fix Dashboard & Cảnh báo tồn kho (fix-dashboard)

Ngày lập: 2026-09-27 · Người lập: Claude (Orchestrator/Architecture) · Người thực thi: Gemini
Nguồn: rà soát source ngày 2026-09-27 + câu trả lời của người dùng cho `code/docs/plans/analytics-dashboard-alert-questions.md`.

> **Cách dùng file này:** Mỗi sprint có một khối **"PROMPT GỬI GEMINI"**. Copy nguyên khối đó (từ dòng `=== BẮT ĐẦU PROMPT` đến `=== KẾT THÚC PROMPT`) vào Gemini khi bắt đầu sprint. Khối rule đã được chèn sẵn trong từng prompt — không cần dán thêm. Làm tuần tự FD-0 → FD-5, không nhảy cóc.

---

## 1. Hiện trạng đã xác minh (bằng chứng)

| # | Vấn đề | Bằng chứng | Mức |
|---|---|---|---|
| P1 | **Lỗi 500** khi "Tất cả chi nhánh" (`branchId = null`) | Log người dùng: `org.postgresql.util.PSQLException: ERROR: could not determine data type of parameter $1`. Chọn TP1 thì không lỗi. SQL trong `AnalyticsDataAdapter` dùng `(:branchId IS NULL OR i.branch_id = :branchId)`; `MapSqlParameterSource.addValue("branchId", null)` gửi null **không kiểu** → PostgreSQL không suy ra kiểu cho vế `IS NULL`. | Critical |
| P2 | Test không bắt được P1 | `AnalyticsDataAdapterTest` chạy **H2** (`MODE=PostgreSQL`), H2 chấp nhận null không kiểu. | High |
| P3 | Các test `*IT.java` (Testcontainers) **không bao giờ chạy** trong `mvn test` | `pom.xml` không có `maven-failsafe-plugin`; Surefire mặc định chỉ chạy `*Test`, `Test*`, `*Tests`, `*TestCase`. Các file `PostgresSpecificFeaturesIT`, `SchemaCompatibilityIT`, `FlywayUpgradePostgresIT`, `ReceivableDebt*PostgresIT` bị bỏ qua. | High |
| P4 | Khi lọc theo chi nhánh, HQ đọc `stock_on_hand`, `receivable_debt` — 2 bảng **không được replicate** lên HQ (quyết định Sprint 6) → số liệu rỗng/cũ | `AnalyticsDataAdapter.getCurrentStockQuantity`, `getTotalReceivableDebt`; `scripts/setup-replication.sh` (`TRANSACTION_TABLES`) | High |
| P5 | Alert không có đầu ra | Có bảng/entity/job (`LowStockAlertJob`), nhưng: không có `@EnableScheduling` (job chưa từng chạy), không API, không UI, log không lưu loại cảnh báo | High |
| P6 | Chỉ số dashboard sai/thiếu | `inventoryTurnoverRatio` hard-code 0; `slowMovingProducts` không gán; thẻ "Công nợ quá hạn" thực chất là tổng nợ; top SP sắp theo số lượng nhưng UI ghi "theo doanh thu"; không lọc `status='CONFIRMED'`; lọc ngày bằng `to_date(...)` trên `confirmed_at` lưu theo giờ JVM (container = UTC) | Medium |
| P7 | Seed V2/V13 chèn dữ liệu giao dịch `branch_id = 1` vào **mọi** DB (HQ, TP1, TP2) với cùng UUID | `V13__seed_transactions.sql`, `V2__seed_test_data.sql` → nếu replicate `stock_on_hand` không lọc, TP2 sẽ đẩy bản sao dòng của chi nhánh 1 lên HQ (vi phạm single-writer) | High (liên quan P4) |
| P8 | `docs/sprints/README.md` có 1 dòng bị lỗi encoding (UTF-16) | Dòng "Sprint 39" hiển thị ký tự lỗi | Low (không sửa trong đợt này, chỉ ghi nhận) |

Môi trường người dùng khi gặp lỗi: `docker compose`, `hq-app` profile `hq` (log có `app.jar!`, PID 1).

---

## 2. Quyết định đã chốt (Gemini KHÔNG được tự thay đổi)

### 2.1. Người dùng đã trả lời
| Mã | Quyết định |
|---|---|
| D-01 | Cảnh báo chỉ hiển thị trên Dashboard (chỉ `ADMIN` tại HQ). Thông báo cho chi nhánh: để sau. |
| D-02 | **Replicate `stock_on_hand` và `receivable_debt` từ Branch lên HQ.** ⚠️ Đảo ngược quyết định Sprint 6 → phải ghi ADR. |
| D-03 | Bổ sung Testcontainers hoàn chỉnh, test phải chạy trên PostgreSQL thật. |
| D-04 | Ngưỡng tồn thấp theo **(sản phẩm, chi nhánh)** — bảng `inventory_alert_config` hiện có. Không có UI quản lý ngưỡng; ngưỡng nhập bằng SQL tại HQ (tự replicate xuống Branch). |
| D-05 | **Tồn âm luôn cảnh báo**, kể cả sản phẩm chưa có cấu hình ngưỡng. |
| D-06 | Phạm vi cảnh báo: **theo từng chi nhánh** (mỗi dòng = 1 cặp sản phẩm–chi nhánh). |
| D-07 | Dashboard **tính cảnh báo trực tiếp từ `stock_movement`** khi gọi API (không phụ thuộc job). |
| D-08 | **Không cần lịch sử cảnh báo** — chỉ danh sách hiện tại. |
| D-09 | Dashboard **tự làm mới định kỳ** (không chỉ khi bấm "Làm mới"). |
| D-10 | Đổi thẻ "Công nợ quá hạn" → **"Tổng công nợ phải thu"**. |
| D-11 | Công nợ theo chi nhánh: 1 khách nợ 2 chi nhánh = 2 khoản nợ độc lập (đúng với `receivable_debt` unique `(customer_id, branch_id)`). |
| D-12 | Export Excel: **không** bổ sung sheet mới (chỉ cập nhật tên field bị đổi). |
| D-13 | Document-Driven: tài liệu trước, code sau. |
| D-14 | Tài liệu sprint đặt trong **`code/docs/fix-dashboard/`**. |
| D-15 | Có migration Flyway mới (`V21`). Không sửa V1–V20. |
| D-16 | Không seed dữ liệu demo. |

### 2.2. Mục người dùng chưa trả lời — plan dùng mặc định sau (người dùng có thể đổi trước khi chạy sprint tương ứng)
| Mã | Mặc định | Lý do |
|---|---|---|
| M-01 | Vòng quay tồn kho = `Giá vốn hàng bán trong kỳ / Giá trị tồn kho hiện tại`; giá trị tồn = `SUM(cost_layer.remaining_qty * unit_cost)`. Mẫu số = 0 → trả `0`. Làm tròn 2 chữ số. | Dữ liệu `cost_layer` đã replicate lên HQ; không cần tính tồn đầu kỳ. Hạn chế ghi vào Tech Debt. |
| M-02 | Sản phẩm bán chậm = sản phẩm có tồn > 0 (trong phạm vi lọc) nhưng **không bán** trong kỳ; top 10 theo tồn giảm dần. | Định nghĩa đơn giản, kiểm chứng được. |
| M-03 | Top sản phẩm sắp theo **doanh thu** giảm dần (khớp tiêu đề UI). | |
| M-04 | Doanh thu = `SUM(sales_invoice_line.line_total)` của hóa đơn `status='CONFIRMED' AND is_deleted=false`. **Chưa trừ hàng trả** (giữ doanh thu gộp). | Giá vốn hàng trả chưa có quy tắc rõ; ghi vào Out of scope. |
| M-05 | Lợi nhuận gộp = Doanh thu − Giá vốn (`SUM(quantity * COALESCE(unit_cost,0))`), tính trong Java từ 1 query tổng hợp. | Tránh 3 query lặp điều kiện. |
| M-06 | Múi giờ: ngày nghiệp vụ theo `Asia/Ho_Chi_Minh`; `confirmed_at` được ghi bằng `LocalDateTime.now()` theo giờ JVM. Backend đổi `dateKey` → khoảng `[start, end)` kiểu `LocalDateTime` theo "storage zone" = `ZoneId.systemDefault()` (cấu hình được qua `analytics.storage-zone`). | Docker = UTC, chạy local = giờ VN; cả hai đều đúng khi HQ và Branch cùng múi giờ JVM (đang đúng). |
| M-07 | Tổng công nợ phải thu = `SUM(GREATEST(total_debt, 0))` từ `receivable_debt` (`is_deleted=false`), áp dụng cho cả "Tất cả" và từng chi nhánh. | Số dư âm (khách trả trước) không phải "phải thu". |
| M-08 | Tồn thấp khi `0 ≤ tồn ≤ ngưỡng` (dùng `<=` như job cũ). Tồn âm luôn là mức `NEGATIVE_STOCK` (ưu tiên hơn `LOW_STOCK`). | |
| M-09 | Chu kỳ tự làm mới dashboard + cảnh báo: **60 giây**, không chạy khi tab ẩn. | |
| M-10 | Bỏ `LowStockAlertJob`, entity/repository `InventoryAlertLog`, DROP bảng `inventory_alert_log` trong V21. **Không bật `@EnableScheduling`**; `AnalyticsEtlJob` giữ nguyên dạng khung (không chạy), ghi Tech Debt. | D-07 + D-08 làm job không còn người tiêu thụ; bật scheduling chỉ để chạy 1 job rỗng là vô nghĩa. |
| M-11 | Đổi tên field DTO `totalOverdueDebt` → `totalReceivableDebt`. ⚠️ BREAKING. | Tên cũ sai nghĩa (D-10). |
| M-12 | API cảnh báo tách riêng: `GET /api/v1/analytics/stock-alerts`. | Tách lỗi/refresh độc lập với KPI; cảnh báo không phụ thuộc kỳ báo cáo. |

---

## 3. Thiết kế đích (Architecture spec)

### 3.1. API

**(a) `GET /api/v1/analytics/dashboard`** — giữ URL, `@PreAuthorize("hasAuthority('ADMIN')")`, chỉ HQ/ALL.
- Query: `branchId` (Long, optional), `startDateKey`, `endDateKey` (Integer `YYYYMMDD`, optional, giữ default hiện tại).
- Validate: `dateKey` phải là ngày hợp lệ; `start ≤ end` → sai thì ném `IllegalArgumentException` (GlobalExceptionHandler trả 400).
- `DashboardMetricsDto` (sau thay đổi):
  - `totalRevenue: BigDecimal`
  - `grossProfit: BigDecimal`
  - `inventoryTurnoverRatio: BigDecimal` (M-01)
  - `totalReceivableDebt: BigDecimal` (⚠️ đổi tên từ `totalOverdueDebt`, M-07)
  - `topSellingProducts: List<ProductPerformanceDto>` (top 10 theo doanh thu, M-03)
  - `slowMovingProducts: List<SlowMovingProductDto>` (M-02) — DTO mới: `productId, productCode, productName, currentStock`

**(b) `GET /api/v1/analytics/stock-alerts`** — MỚI, `ADMIN`, chỉ HQ/ALL.
- Query: `branchId` (optional; null = tất cả chi nhánh).
- Response `ApiResponse<StockAlertSummaryDto>`:
  - `negativeCount: int`, `lowStockCount: int`, `generatedAt: OffsetDateTime`
  - `alerts: List<StockAlertDto>` sắp xếp: `NEGATIVE_STOCK` trước, sau đó `currentQuantity` tăng dần.
- `StockAlertDto`: `productId, productCode, productName, branchId, branchName, currentQuantity, minQuantityThreshold (nullable), alertType ("NEGATIVE_STOCK" | "LOW_STOCK")`.

### 3.2. Quy tắc tính cảnh báo (StockAlertService, trong module `analytics`)
1. `levels` = `SELECT product_id, branch_id, SUM(quantity) FROM stock_movement WHERE (:branchId IS NULL OR branch_id = :branchId) GROUP BY product_id, branch_id` (tham số `branchId` **có kiểu** `Types.BIGINT`).
2. `configs` = các `inventory_alert_config` có `is_active = true` (lọc theo `branchId` nếu có) — qua `InventoryAlertConfigRepository` (cùng module analytics).
3. Với mỗi `level.qty < 0` → `NEGATIVE_STOCK` (kèm ngưỡng nếu có config) — D-05.
4. Với mỗi config: `qty = level.qty` hoặc `0` nếu chưa có movement; nếu `0 ≤ qty ≤ threshold` → `LOW_STOCK` — M-08.
5. Không sinh trùng: 1 cặp (product, branch) tối đa 1 cảnh báo.
6. Làm giàu tên: `CatalogFacade.getProductBasicInfo(ids)` và `BranchFacade.getBranchNames(ids)` — gọi **batch 1 lần** (không gọi trong vòng lặp). Thiếu tên → dùng `"#<id>"`.
7. Không JOIN SQL giữa `stock_movement` (inventory) và `inventory_alert_config` (analytics) — ghép trong Java (tuân thủ AGENTS.md).

Lý do dùng `stock_movement` thay vì `stock_on_hand` dù đã replicate (D-07): `stock_movement` là append-only, là nguồn sự thật (`SUM(quantity) = stock_on_hand.quantity`), không có rủi ro cập nhật chồng. `stock_on_hand` replicate để phục vụ màn tồn kho ở HQ và để đối soát (xem test FD-2).

### 3.3. SQL — quy ước bắt buộc
- Mọi tham số có thể null phải có kiểu: `params.addValue("branchId", branchId, java.sql.Types.BIGINT)`. Tạo 1 helper dùng chung trong `analytics/infrastructure` (ví dụ `AnalyticsSqlParams`) để không lặp.
- Lọc thời gian bằng khoảng: `i.confirmed_at >= :fromTs AND i.confirmed_at < :toTs` (tham số `LocalDateTime`, `Types.TIMESTAMP`), KHÔNG dùng `to_date(:key::text, ...)` bọc cột.
- Doanh thu/giá vốn: 1 query trả 2 cột `revenue`, `cogs`.
- Công nợ: đọc `receivable_debt` (đã replicate).
- Bỏ các hàm port không còn dùng (`getCurrentStockQuantity`, `getTotalGrossProfit`, `getTotalCogs` riêng lẻ nếu gộp).

### 3.4. Migration `V21__analytics_alert_and_snapshot_replication.sql` (chạy trên mọi DB: HQ + Branch)
1. `inventory_alert_config`:
   - `UPDATE ... SET is_active = true WHERE is_active IS NULL`; `ALTER COLUMN is_active SET DEFAULT true, SET NOT NULL`.
   - Nếu tồn tại dòng thiếu `product_id`/`branch_id`/`min_quantity_threshold` → xoá các dòng rác đó (ghi rõ trong comment migration).
   - Khử trùng lặp `(product_id, branch_id)`: giữ dòng `id` lớn nhất.
   - `SET NOT NULL` cho `product_id`, `branch_id`, `min_quantity_threshold`.
   - `ADD CONSTRAINT ck_alert_threshold_non_negative CHECK (min_quantity_threshold >= 0)`.
   - `ADD CONSTRAINT uk_alert_config_product_branch UNIQUE (product_id, branch_id)`.
2. Replica identity cho replicate có lọc dòng (PostgreSQL 15+ yêu cầu cột trong `WHERE` của publication phải thuộc replica identity khi publish UPDATE/DELETE):
   - `ALTER TABLE stock_on_hand REPLICA IDENTITY USING INDEX uk_stock_product_branch;`
   - `ALTER TABLE receivable_debt REPLICA IDENTITY USING INDEX uk_debt_customer_branch;`
3. `DROP TABLE IF EXISTS inventory_alert_log;` (M-10)
4. Index hỗ trợ dashboard: `CREATE INDEX IF NOT EXISTS ix_sales_invoice_confirmed ON sales_invoice (confirmed_at) WHERE status = 'CONFIRMED' AND is_deleted = false;`

### 3.5. Replication `stock_on_hand`, `receivable_debt` (D-02)
- Branch publication `pub_<branch>_to_hq` thêm 2 bảng **có lọc dòng theo chi nhánh sở hữu**:
  `ALTER PUBLICATION pub_tp1_to_hq ADD TABLE stock_on_hand WHERE (branch_id = 1), receivable_debt WHERE (branch_id = 1);`
  Lý do lọc: seed V2/V13 tạo dòng `branch_id = 1` trong DB của mọi chi nhánh (P7). Không lọc → TP2 đẩy dòng của TP1 lên HQ → 2 writer cho cùng 1 khóa.
- `branch_id` số lấy từ DB chi nhánh: `SELECT id FROM branch WHERE upper(code) = upper('<tên>')` (không hard-code).
- HQ: trước khi copy dữ liệu ban đầu cho 1 chi nhánh, xoá **chỉ các dòng của chi nhánh đó** tại HQ (`DELETE FROM stock_on_hand WHERE branch_id = <id>` và tương tự `receivable_debt`) — KHÔNG `TRUNCATE` (sẽ xoá dữ liệu chi nhánh khác đã đồng bộ).
- HQ: `ALTER SUBSCRIPTION sub_hq_from_<branch> REFRESH PUBLICATION WITH (copy_data = true);` (chỉ copy các bảng mới thêm).
- Script mới idempotent: `code/scripts/enable-snapshot-replication.sh <branch>`; `setup-replication.sh` gọi script này ở cuối để chi nhánh mới tự có.
- Bất biến ứng dụng: HQ không có API ghi 2 bảng này (đã đúng: `SalesInvoiceController`, `GoodsReturnController`, `InboundReceiptController` chỉ bật ở BRANCH/ALL; `/receivable-debts/opening-balance` ném lỗi khi `branchId == null`). Phải có test khẳng định điều này.

### 3.6. Frontend
- `src/types/analytics.ts`: đổi `totalOverdueDebt` → `totalReceivableDebt`; thêm `slowMovingProducts`; thêm type `StockAlert`, `StockAlertSummary`.
- `src/api/ApiService.ts`: thêm `Analytics.getStockAlerts(branchId?)`.
- `src/config/dashboard.ts` (mới): `DASHBOARD_REFETCH_INTERVAL_MS = 60_000`, nhãn loại cảnh báo.
- `src/hooks/useDashboardMetrics.ts`, `src/hooks/useStockAlerts.ts` (mới) — `useQuery` với `refetchInterval`, `refetchIntervalInBackground: false`.
- `src/components/sales/StockAlertPanel.tsx` (mới, component "dumb"): nhận `summary`, `isLoading`, `isError`, `onRetry`.
  - Có cảnh báo: khối nổi bật đầu dashboard — dòng tóm tắt "X sản phẩm tồn âm · Y sản phẩm dưới ngưỡng", bảng: Mức (badge đỏ "Tồn âm" / vàng "Sắp hết"), Chi nhánh, Mã SP, Tên SP, Tồn hiện tại, Ngưỡng ("—" nếu null).
  - Không có: dòng xanh "Không có cảnh báo tồn kho".
  - Lỗi API cảnh báo KHÔNG làm hỏng phần KPI (query độc lập).
  - Ghi chú nhỏ: "Cảnh báo theo tồn hiện tại, không phụ thuộc kỳ báo cáo".
- `Dashboard.tsx`: dùng 2 hook mới (không gọi `useQuery` trực tiếp cho metrics), đổi nhãn thẻ công nợ (D-10), sửa tiêu đề top SP, thêm bảng "Sản phẩm bán chậm", hiển thị thời điểm cập nhật cuối.
- Dùng lại design token/class sẵn có (`card`, `badge-danger`, `erp-table`, `DataState`...). Không thêm thư viện.

---

## 4. Danh sách sprint

| Sprint | Tên | Kết quả chính | Phụ thuộc |
|---|---|---|---|
| FD-0 | Tài liệu kiến trúc & baseline | Docs API/Schema/Replication/ADR cập nhật; baseline test | — |
| FD-1 | Sửa lỗi 500 + hạ tầng test Postgres | Dashboard "Tất cả chi nhánh" chạy; failsafe; IT adapter trên Postgres | FD-0 |
| FD-2 | Migration V21 + replicate 2 bảng snapshot | V21; script replication; IT 2 container | FD-1 |
| FD-3 | Backend analytics & API cảnh báo | Chỉ số mới, `/stock-alerts`, bỏ job cũ | FD-2 |
| FD-4 | Frontend dashboard & cảnh báo | Panel cảnh báo, auto refresh, đổi nhãn, bán chậm | FD-3 |
| FD-5 | Kiểm thử E2E trên Docker & đóng đợt | Kịch bản thật HQ+TP1+TP2; CHANGELOG, AI_CONTEXT | FD-4 |

Thư mục tài liệu: `code/docs/fix-dashboard/`
- `00-PLAN.md` (file này) · `README.md` (index sprint) · `sprint-fd0-...md` … `sprint-fd5-...md`.

---

## SPRINT FD-0 — Tài liệu kiến trúc & baseline

=== BẮT ĐẦU PROMPT GỬI GEMINI (FD-0) ===

> ### 🔒 KHỐI RULE BẮT BUỘC — ĐỌC LẠI TOÀN BỘ TRƯỚC KHI LÀM SPRINT NÀY
> (Khối này được copy nguyên văn vào mọi sprint. Không được bỏ qua với lý do "đã đọc ở sprint trước".)
>
> **R0. Trước khi viết bất kỳ dòng code nào**, mở và đọc lại 4 file sau:
> `.agents/AGENTS.md`, `.agents/rules/strict-debugging-circuit-breaker.md`, `.agents/rules/Sprint-docs-rule.md`, `code/docs/fix-dashboard/00-PLAN.md` (mục 2 "Quyết định đã chốt").
> Sau đó viết lại 3 dòng tóm tắt: (1) sprint này làm gì, (2) KHÔNG được làm gì, (3) điều kiện "xong".
>
> **R1. Kiến trúc (AGENTS.md)**
> - Modular monolith, bounded context: KHÔNG inject `*Repository` của module khác. Lấy tên sản phẩm/chi nhánh qua `CatalogFacade` / `BranchFacade`.
> - KHÔNG trả Entity ra API — luôn dùng DTO. Mọi REST trả `ApiResponse<T>` (`success, data, message, errors`).
> - Mọi Service/Controller có `@Slf4j` và log `info/debug/error`. KHÔNG ghi log vào DB.
> - Mọi class/hàm có business logic phải có Javadoc/JSDoc giải thích **why**.
> - Frontend: component KHÔNG gọi axios trực tiếp — đi qua `ApiService`; fetch data qua custom hook trong `src/hooks/`; hằng số (chu kỳ refresh, nhãn...) đặt trong `src/config/`.
> - Document-Driven: code phải khớp tài liệu trong `code/docs/fix-dashboard/00-PLAN.md` và `code/docs/architecture/*`. Nếu thấy tài liệu sai/thiếu → DỪNG, hỏi người dùng, KHÔNG tự đoán nghiệp vụ.
>
> **R2. Circuit Breaker khi gặp lỗi build/runtime/test (nguyên văn rule)**
> 1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng. Nếu đã sửa code và chạy lại từ 2 lần trở lên mà lỗi VẪN xuất hiện (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace), phải NGAY LẬP TỨC dừng việc sửa code. KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành bước 2.
> 2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi, trả lời đủ 5 mục: (1) Nguyên văn lỗi mới nhất (copy chính xác). (2) Giả thuyết đã thử là gì và tại sao nó KHÔNG đúng — nếu không biết, ghi rõ "tôi không biết vì sao lần trước thất bại". (3) Lỗi nằm ở lớp nào, chọn đúng 1: `syntax` / `logic` / `dependency-version` / `config-env` / `data-state` / `race-condition` / `unknown`. (4) Bằng chứng cụ thể (dán đúng đoạn code, log/print giá trị thực tế, hoặc doc/version của lib). (5) Kế hoạch sửa — chỉ 1 thay đổi duy nhất.
> 3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi. Sau 3 lần vẫn lỗi → KHÔNG thử lần 4; dừng hẳn, tóm tắt những gì đã thử/loại trừ, hỏi người dùng kèm 2–3 hướng khả dĩ.
> 4. Cấm: sửa code mà không trích lại đúng đoạn log lỗi mới nhất; nói "thử cách khác xem sao" mà không nêu giả thuyết; sửa nhiều file/nhiều chỗ trong 1 lần khi chưa xác định root cause; nuốt exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi".
> 5. Lỗi liên quan version/dependency/library: phải tra doc/changelog thật hoặc hỏi người dùng version TRƯỚC khi đoán API (không bịa API).
>
> **R3. Tài liệu kết thúc sprint (Sprint-docs-rule.md)**
> - Tạo `code/docs/fix-dashboard/sprint-<mã>-<tên-ngắn>.md`, dòng đầu ghi ngày hoàn thành + mã sprint.
> - Đủ 8 mục: 1. Mục tiêu · 2. Thành quả (có đường dẫn file) · 3. Quyết định kiến trúc & lý do (có phương án đã bỏ + lý do bỏ) · 4. Hướng dẫn đọc code theo thứ tự · 5. Rủi ro/Nợ kỹ thuật · 6. Việc chưa làm/Out of scope · 7. Cách chạy & verify (lệnh copy-paste + kết quả mong đợi) · 8. Handoff sprint sau.
> - Viết cho người đọc lần đầu; không viết code chi tiết trong docs (chỉ trỏ file + why); cấm tính từ tự khen ("xuất sắc", "hoàn hảo", "chuẩn"); thay đổi hành vi/API đánh dấu `⚠️ BREAKING`.
> - Thêm 1 dòng vào `code/docs/fix-dashboard/README.md` (số sprint, tên, ngày, 1 dòng tóm tắt).
> - Cuối sprint tự điền checklist ✅/❌ 10 mục (8 mục + đã thêm link README + không có tính từ mơ hồ). Còn ❌ → chưa xong.
>
> **R4. Kỷ luật phạm vi cho Gemini**
> - Chỉ sửa các file liệt kê trong "Phạm vi file" của sprint. Cần sửa file ngoài danh sách → ghi lý do vào sprint doc mục 3 trước khi sửa.
> - KHÔNG sửa migration cũ `V1`…`V20` (Flyway checksum). Thay đổi schema chỉ qua migration mới.
> - KHÔNG xoá file bằng `rm` mà không ghi vào sprint doc; KHÔNG `git commit`/`git push` trừ khi người dùng yêu cầu.
> - Mỗi sprint kết thúc bằng việc chạy đúng các lệnh trong mục "Verify" và dán kết quả (số test pass/fail) vào sprint doc mục 7.
> - Không chắc → hỏi. Không tự mở rộng scope ("tiện tay sửa luôn").

**Bối cảnh:** Dự án ERP (Spring Boot 3.4 + React). Repo gốc là thư mục `DOAN`. Đợt "fix-dashboard" hoàn thiện module analytics: sửa lỗi 500 dashboard, replicate `stock_on_hand`/`receivable_debt` lên HQ, và thêm cảnh báo tồn kho âm/dưới ngưỡng trên dashboard. Kế hoạch tổng: `code/docs/fix-dashboard/00-PLAN.md` — đọc mục 1, 2, 3 trước.

**Mục tiêu sprint:** Chỉ viết/cập nhật TÀI LIỆU (không sửa code Java/TS) + chạy baseline test.

**Việc cần làm:**
1. Chạy baseline và ghi kết quả (số test run/fail/skip) vào sprint doc mục 7:
   - Backend (thư mục `code/erp-platform/services/erp-backend`): `.\mvnw.cmd test`
   - Frontend (thư mục `code/erp-platform/apps/erp-frontend`): `npm run build` và `npx vitest run`
   Nếu baseline có test fail → KHÔNG sửa, chỉ ghi lại tên test fail vào mục 5 của sprint doc.
2. `code/docs/architecture/API_CONTRACT.md`: cập nhật/ thêm mục Analytics theo **00-PLAN mục 3.1** (2 endpoint, DTO, mã lỗi 400/403, ví dụ JSON). Đánh dấu `⚠️ BREAKING` cho đổi tên `totalOverdueDebt` → `totalReceivableDebt`.
3. `code/docs/architecture/DATA_SCHEMA.md`: mô tả V21 theo **mục 3.4**; ghi `inventory_alert_log` bị xoá.
4. `code/docs/architecture/DATA_OWNERSHIP_MATRIX.md`: thêm dòng `stock_on_hand`, `receivable_debt` (Owner Branch, HQ Write No, Branch Write Yes, `Branch -> HQ (row filter branch_id)`); xoá dòng `inventory_alert_log`.
5. `code/docs/architecture/DATABASE_REPLICATION.md` + `REPLICATION_RUNBOOK.md`: thêm 2 bảng vào danh sách Branch → HQ, giải thích row filter, replica identity, quy trình bật cho chi nhánh đang chạy (**mục 3.5**), lý do không TRUNCATE ở HQ.
6. `code/docs/architecture/ARCHITECTURE_DECISIONS.md`: thêm ADR mới "Replicate snapshot tables với row filter — thay thế quyết định Sprint 6", có: bối cảnh, quyết định, phương án đã bỏ (giữ nguyên Sprint 6: tính động — bỏ vì công nợ theo chi nhánh không tính được từ `sales_invoice` khi có thu nợ/trả hàng/số dư đầu kỳ; replicate không lọc — bỏ vì P7), hệ quả.
7. Tạo `code/docs/fix-dashboard/README.md` (index) và `code/docs/fix-dashboard/business-rules.md` chép lại **mục 2 + 3.2** thành quy tắc nghiệp vụ có ví dụ số (ví dụ: tồn -5 không có ngưỡng → NEGATIVE; tồn 0 ngưỡng 0 → LOW; tồn 11 ngưỡng 10 → không cảnh báo). Kèm ví dụ SQL nhập ngưỡng tại HQ:
   `INSERT INTO inventory_alert_config (product_id, branch_id, min_quantity_threshold, email_recipients, is_active, created_at, updated_at) VALUES (1, 1, 10, NULL, true, now(), now());`
8. Thêm 1 dòng vào `code/docs/sprints/README.md` trỏ tới `../fix-dashboard/README.md` (chỉ thêm dòng mới ở cuối, lưu UTF-8; KHÔNG sửa dòng Sprint 39 đang lỗi encoding).

**Phạm vi file:** chỉ các file `.md` nêu trên + sprint doc.
**KHÔNG làm:** sửa code, sửa migration, chạy script replication.
**Điều kiện xong:** 8 việc trên xong; mọi mục trong tài liệu khớp 00-PLAN (không tự thêm nghiệp vụ); sprint doc `code/docs/fix-dashboard/sprint-fd0-architecture-docs.md` đủ 8 mục + checklist R3.

=== KẾT THÚC PROMPT (FD-0) ===

---

## SPRINT FD-1 — Sửa lỗi 500 + hạ tầng test Postgres

=== BẮT ĐẦU PROMPT GỬI GEMINI (FD-1) ===

> ### 🔒 KHỐI RULE BẮT BUỘC — ĐỌC LẠI TOÀN BỘ TRƯỚC KHI LÀM SPRINT NÀY
> (Khối này được copy nguyên văn vào mọi sprint. Không được bỏ qua với lý do "đã đọc ở sprint trước".)
>
> **R0. Trước khi viết bất kỳ dòng code nào**, mở và đọc lại 4 file sau:
> `.agents/AGENTS.md`, `.agents/rules/strict-debugging-circuit-breaker.md`, `.agents/rules/Sprint-docs-rule.md`, `code/docs/fix-dashboard/00-PLAN.md` (mục 2 "Quyết định đã chốt").
> Sau đó viết lại 3 dòng tóm tắt: (1) sprint này làm gì, (2) KHÔNG được làm gì, (3) điều kiện "xong".
>
> **R1. Kiến trúc (AGENTS.md)**
> - Modular monolith, bounded context: KHÔNG inject `*Repository` của module khác. Lấy tên sản phẩm/chi nhánh qua `CatalogFacade` / `BranchFacade`.
> - KHÔNG trả Entity ra API — luôn dùng DTO. Mọi REST trả `ApiResponse<T>` (`success, data, message, errors`).
> - Mọi Service/Controller có `@Slf4j` và log `info/debug/error`. KHÔNG ghi log vào DB.
> - Mọi class/hàm có business logic phải có Javadoc/JSDoc giải thích **why**.
> - Frontend: component KHÔNG gọi axios trực tiếp — đi qua `ApiService`; fetch data qua custom hook trong `src/hooks/`; hằng số (chu kỳ refresh, nhãn...) đặt trong `src/config/`.
> - Document-Driven: code phải khớp tài liệu trong `code/docs/fix-dashboard/00-PLAN.md` và `code/docs/architecture/*`. Nếu thấy tài liệu sai/thiếu → DỪNG, hỏi người dùng, KHÔNG tự đoán nghiệp vụ.
>
> **R2. Circuit Breaker khi gặp lỗi build/runtime/test (nguyên văn rule)**
> 1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng. Nếu đã sửa code và chạy lại từ 2 lần trở lên mà lỗi VẪN xuất hiện (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace), phải NGAY LẬP TỨC dừng việc sửa code. KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành bước 2.
> 2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi, trả lời đủ 5 mục: (1) Nguyên văn lỗi mới nhất (copy chính xác). (2) Giả thuyết đã thử là gì và tại sao nó KHÔNG đúng — nếu không biết, ghi rõ "tôi không biết vì sao lần trước thất bại". (3) Lỗi nằm ở lớp nào, chọn đúng 1: `syntax` / `logic` / `dependency-version` / `config-env` / `data-state` / `race-condition` / `unknown`. (4) Bằng chứng cụ thể (dán đúng đoạn code, log/print giá trị thực tế, hoặc doc/version của lib). (5) Kế hoạch sửa — chỉ 1 thay đổi duy nhất.
> 3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi. Sau 3 lần vẫn lỗi → KHÔNG thử lần 4; dừng hẳn, tóm tắt những gì đã thử/loại trừ, hỏi người dùng kèm 2–3 hướng khả dĩ.
> 4. Cấm: sửa code mà không trích lại đúng đoạn log lỗi mới nhất; nói "thử cách khác xem sao" mà không nêu giả thuyết; sửa nhiều file/nhiều chỗ trong 1 lần khi chưa xác định root cause; nuốt exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi".
> 5. Lỗi liên quan version/dependency/library: phải tra doc/changelog thật hoặc hỏi người dùng version TRƯỚC khi đoán API (không bịa API).
>
> **R3. Tài liệu kết thúc sprint (Sprint-docs-rule.md)**
> - Tạo `code/docs/fix-dashboard/sprint-<mã>-<tên-ngắn>.md`, dòng đầu ghi ngày hoàn thành + mã sprint.
> - Đủ 8 mục: 1. Mục tiêu · 2. Thành quả (có đường dẫn file) · 3. Quyết định kiến trúc & lý do (có phương án đã bỏ + lý do bỏ) · 4. Hướng dẫn đọc code theo thứ tự · 5. Rủi ro/Nợ kỹ thuật · 6. Việc chưa làm/Out of scope · 7. Cách chạy & verify (lệnh copy-paste + kết quả mong đợi) · 8. Handoff sprint sau.
> - Viết cho người đọc lần đầu; không viết code chi tiết trong docs (chỉ trỏ file + why); cấm tính từ tự khen ("xuất sắc", "hoàn hảo", "chuẩn"); thay đổi hành vi/API đánh dấu `⚠️ BREAKING`.
> - Thêm 1 dòng vào `code/docs/fix-dashboard/README.md` (số sprint, tên, ngày, 1 dòng tóm tắt).
> - Cuối sprint tự điền checklist ✅/❌ 10 mục (8 mục + đã thêm link README + không có tính từ mơ hồ). Còn ❌ → chưa xong.
>
> **R4. Kỷ luật phạm vi cho Gemini**
> - Chỉ sửa các file liệt kê trong "Phạm vi file" của sprint. Cần sửa file ngoài danh sách → ghi lý do vào sprint doc mục 3 trước khi sửa.
> - KHÔNG sửa migration cũ `V1`…`V20` (Flyway checksum). Thay đổi schema chỉ qua migration mới.
> - KHÔNG xoá file bằng `rm` mà không ghi vào sprint doc; KHÔNG `git commit`/`git push` trừ khi người dùng yêu cầu.
> - Mỗi sprint kết thúc bằng việc chạy đúng các lệnh trong mục "Verify" và dán kết quả (số test pass/fail) vào sprint doc mục 7.
> - Không chắc → hỏi. Không tự mở rộng scope ("tiện tay sửa luôn").

**Bối cảnh:** Đọc `code/docs/fix-dashboard/00-PLAN.md` mục 1 (P1, P2, P3) và 3.3. Sprint FD-0 đã cập nhật tài liệu.

**Lỗi cần sửa (nguyên văn log người dùng cung cấp):**
```
Caused by: org.postgresql.util.PSQLException: ERROR: could not determine data type of parameter $1
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2733)
	...
	at org.springframework.jdbc.core.JdbcTemplate.execute(JdbcTemplate.java:658)
```
Tái hiện: `GET /api/v1/analytics/dashboard?startDateKey=20260901&endDateKey=20260927` (không có `branchId`) → 500. Có `branchId=1` → 200.
Lớp lỗi: `config-env` (PostgreSQL không suy ra kiểu tham số null; H2 thì có). Root cause đã xác định — nhưng vẫn phải làm đúng thứ tự "test đỏ → sửa 1 chỗ → test xanh".

**Việc cần làm (theo đúng thứ tự):**
1. **Bật chạy IT:** thêm `maven-failsafe-plugin` vào `pom.xml` (goals `integration-test`, `verify`; include `**/*IT.java`). Không đổi version Spring Boot. Kiểm tra version plugin do Spring Boot parent quản lý (không tự ghi version nếu parent đã quản lý). Chạy `.\mvnw.cmd verify` và ghi lại: các IT cũ (`PostgresSpecificFeaturesIT`, `SchemaCompatibilityIT`, `FlywayUpgradePostgresIT`, `ReceivableDebt*PostgresIT`) pass/fail thế nào. IT cũ nào fail → KHÔNG sửa trong sprint này, ghi vào Tech Debt + báo người dùng.
2. **Viết test đỏ:** tạo `src/test/java/com/storename/erp/analytics/infrastructure/AnalyticsDataAdapterPostgresIT.java`, `extends PostgresIntegrationTest`, `@SpringBootTest`, `@ActiveProfiles("postgres-it")`, `@Transactional` (rollback). Gọi các hàm của `AnalyticsDataAdapter` với `branchId = null` và `branchId = 1`. Chạy: `.\mvnw.cmd -Dtest=AnalyticsDataAdapterPostgresIT -Dsurefire.failIfNoSpecifiedTests=false test` → phải thấy đúng lỗi `could not determine data type of parameter`. Dán nguyên văn vào sprint doc.
   Lưu ý: DB test đã có seed V2/V13 → assert bằng dữ liệu fixture riêng (tạo hóa đơn test với `invoice_code` duy nhất) hoặc so sánh chênh lệch trước/sau; không assert số tuyệt đối từ seed.
3. **Sửa đúng 1 chỗ:** tạo helper `AnalyticsSqlParams` trong `analytics/infrastructure` và dùng `addValue("branchId", branchId, Types.BIGINT)` ở mọi query có `:branchId`. Không đổi logic khác. Chạy lại test → xanh.
4. **Sửa múi giờ & lọc ngày (M-06) — thay đổi thứ 2, chỉ sau khi bước 3 xanh:**
   - Thêm `analytics.business-zone` (mặc định `Asia/Ho_Chi_Minh`) và `analytics.storage-zone` (mặc định rỗng = `ZoneId.systemDefault()`) vào `application.yml`, đọc bằng `@ConfigurationProperties` (class trong `analytics`).
   - Service chuyển `startDateKey/endDateKey` → `fromTs`, `toTs` (`LocalDateTime`, `toTs` = 00:00 ngày sau `endDateKey`), validate ngày hợp lệ và `start ≤ end` (sai → `IllegalArgumentException`).
   - SQL dùng `confirmed_at >= :fromTs AND confirmed_at < :toTs`, thêm `i.status = 'CONFIRMED'`.
   - Test IT: hóa đơn `confirmed_at` = `2026-09-01 17:30` (storage UTC) phải thuộc ngày 2026-09-02 theo giờ VN.
5. **Xoá** `AnalyticsDataAdapterTest.java` (H2) — lý do: H2 không kiểm chứng được cú pháp PostgreSQL, gây ra P2. Ghi lý do vào sprint doc mục 3.
6. Cập nhật `DashboardServiceTest`, `DashboardControllerTest` (thêm case 400 khi `startDateKey` > `endDateKey` hoặc ngày không hợp lệ như `20260231`).

**Phạm vi file:** `pom.xml`; `analytics/infrastructure/AnalyticsDataAdapter.java`, `analytics/infrastructure/AnalyticsSqlParams.java` (mới); class properties mới trong `analytics`; `analytics/application/DashboardService.java`; `application.yml`; test trong `src/test/java/com/storename/erp/analytics/**`.
**KHÔNG làm:** đổi DTO, đổi công thức chỉ số, đụng replication, frontend.
**Verify:**
- `.\mvnw.cmd test` → không có test fail mới so với baseline FD-0.
- `.\mvnw.cmd verify` → `AnalyticsDataAdapterPostgresIT` pass.
- Thủ công: rebuild `hq-app` (`docker compose up -d --build hq-app` tại thư mục `code`), mở dashboard chọn "Tất cả chi nhánh" → 200. Nếu không tự chạy được Docker, ghi rõ "chưa verify thủ công" và nhờ người dùng chạy.
**Điều kiện xong:** 500 hết; IT chạy trên Postgres; sprint doc `sprint-fd1-fix-500-postgres-it.md` đủ 8 mục + checklist.

=== KẾT THÚC PROMPT (FD-1) ===

---

## SPRINT FD-2 — Migration V21 + replicate `stock_on_hand`, `receivable_debt`

=== BẮT ĐẦU PROMPT GỬI GEMINI (FD-2) ===

> ### 🔒 KHỐI RULE BẮT BUỘC — ĐỌC LẠI TOÀN BỘ TRƯỚC KHI LÀM SPRINT NÀY
> (Khối này được copy nguyên văn vào mọi sprint. Không được bỏ qua với lý do "đã đọc ở sprint trước".)
>
> **R0. Trước khi viết bất kỳ dòng code nào**, mở và đọc lại 4 file sau:
> `.agents/AGENTS.md`, `.agents/rules/strict-debugging-circuit-breaker.md`, `.agents/rules/Sprint-docs-rule.md`, `code/docs/fix-dashboard/00-PLAN.md` (mục 2 "Quyết định đã chốt").
> Sau đó viết lại 3 dòng tóm tắt: (1) sprint này làm gì, (2) KHÔNG được làm gì, (3) điều kiện "xong".
>
> **R1. Kiến trúc (AGENTS.md)**
> - Modular monolith, bounded context: KHÔNG inject `*Repository` của module khác. Lấy tên sản phẩm/chi nhánh qua `CatalogFacade` / `BranchFacade`.
> - KHÔNG trả Entity ra API — luôn dùng DTO. Mọi REST trả `ApiResponse<T>` (`success, data, message, errors`).
> - Mọi Service/Controller có `@Slf4j` và log `info/debug/error`. KHÔNG ghi log vào DB.
> - Mọi class/hàm có business logic phải có Javadoc/JSDoc giải thích **why**.
> - Frontend: component KHÔNG gọi axios trực tiếp — đi qua `ApiService`; fetch data qua custom hook trong `src/hooks/`; hằng số (chu kỳ refresh, nhãn...) đặt trong `src/config/`.
> - Document-Driven: code phải khớp tài liệu trong `code/docs/fix-dashboard/00-PLAN.md` và `code/docs/architecture/*`. Nếu thấy tài liệu sai/thiếu → DỪNG, hỏi người dùng, KHÔNG tự đoán nghiệp vụ.
>
> **R2. Circuit Breaker khi gặp lỗi build/runtime/test (nguyên văn rule)**
> 1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng. Nếu đã sửa code và chạy lại từ 2 lần trở lên mà lỗi VẪN xuất hiện (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace), phải NGAY LẬP TỨC dừng việc sửa code. KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành bước 2.
> 2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi, trả lời đủ 5 mục: (1) Nguyên văn lỗi mới nhất (copy chính xác). (2) Giả thuyết đã thử là gì và tại sao nó KHÔNG đúng — nếu không biết, ghi rõ "tôi không biết vì sao lần trước thất bại". (3) Lỗi nằm ở lớp nào, chọn đúng 1: `syntax` / `logic` / `dependency-version` / `config-env` / `data-state` / `race-condition` / `unknown`. (4) Bằng chứng cụ thể (dán đúng đoạn code, log/print giá trị thực tế, hoặc doc/version của lib). (5) Kế hoạch sửa — chỉ 1 thay đổi duy nhất.
> 3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi. Sau 3 lần vẫn lỗi → KHÔNG thử lần 4; dừng hẳn, tóm tắt những gì đã thử/loại trừ, hỏi người dùng kèm 2–3 hướng khả dĩ.
> 4. Cấm: sửa code mà không trích lại đúng đoạn log lỗi mới nhất; nói "thử cách khác xem sao" mà không nêu giả thuyết; sửa nhiều file/nhiều chỗ trong 1 lần khi chưa xác định root cause; nuốt exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi".
> 5. Lỗi liên quan version/dependency/library: phải tra doc/changelog thật hoặc hỏi người dùng version TRƯỚC khi đoán API (không bịa API).
>
> **R3. Tài liệu kết thúc sprint (Sprint-docs-rule.md)**
> - Tạo `code/docs/fix-dashboard/sprint-<mã>-<tên-ngắn>.md`, dòng đầu ghi ngày hoàn thành + mã sprint.
> - Đủ 8 mục: 1. Mục tiêu · 2. Thành quả (có đường dẫn file) · 3. Quyết định kiến trúc & lý do (có phương án đã bỏ + lý do bỏ) · 4. Hướng dẫn đọc code theo thứ tự · 5. Rủi ro/Nợ kỹ thuật · 6. Việc chưa làm/Out of scope · 7. Cách chạy & verify (lệnh copy-paste + kết quả mong đợi) · 8. Handoff sprint sau.
> - Viết cho người đọc lần đầu; không viết code chi tiết trong docs (chỉ trỏ file + why); cấm tính từ tự khen ("xuất sắc", "hoàn hảo", "chuẩn"); thay đổi hành vi/API đánh dấu `⚠️ BREAKING`.
> - Thêm 1 dòng vào `code/docs/fix-dashboard/README.md` (số sprint, tên, ngày, 1 dòng tóm tắt).
> - Cuối sprint tự điền checklist ✅/❌ 10 mục (8 mục + đã thêm link README + không có tính từ mơ hồ). Còn ❌ → chưa xong.
>
> **R4. Kỷ luật phạm vi cho Gemini**
> - Chỉ sửa các file liệt kê trong "Phạm vi file" của sprint. Cần sửa file ngoài danh sách → ghi lý do vào sprint doc mục 3 trước khi sửa.
> - KHÔNG sửa migration cũ `V1`…`V20` (Flyway checksum). Thay đổi schema chỉ qua migration mới.
> - KHÔNG xoá file bằng `rm` mà không ghi vào sprint doc; KHÔNG `git commit`/`git push` trừ khi người dùng yêu cầu.
> - Mỗi sprint kết thúc bằng việc chạy đúng các lệnh trong mục "Verify" và dán kết quả (số test pass/fail) vào sprint doc mục 7.
> - Không chắc → hỏi. Không tự mở rộng scope ("tiện tay sửa luôn").

**Bối cảnh:** Đọc `00-PLAN.md` mục 1 (P4, P7), 2.1 (D-02), 3.4, 3.5; và `code/docs/architecture/DATABASE_REPLICATION.md` (đã cập nhật ở FD-0). PostgreSQL trong docker-compose là `postgres:15` (hỗ trợ row filter publication).

**Việc cần làm:**
1. Tạo `src/main/resources/db/migration/V21__analytics_alert_and_snapshot_replication.sql` đúng **mục 3.4**. Mỗi khối có comment giải thích why. Migration phải chạy được trên DB đã có dữ liệu (idempotent với `IF EXISTS`/`IF NOT EXISTS` khi có thể).
2. Cập nhật entity `InventoryAlertConfig` khớp schema mới (`@Column(nullable = false)` cho các cột NOT NULL). Kiểm tra `ddl-auto: validate` vẫn khởi động được (IT `FlywayPostgresIntegrationTest`).
   **Xoá luôn trong sprint này** các file liên quan `inventory_alert_log` (vì V21 DROP bảng; nếu giữ entity thì Hibernate `ddl-auto: validate` sẽ lỗi khi khởi động): `InventoryAlertLog.java`, `InventoryAlertLogRepository.java`, `LowStockAlertJob.java`, `LowStockAlertJobTest.java`. Grep toàn repo (`inventory_alert_log`, `InventoryAlertLog`, `LowStockAlertJob`) để chắc không còn tham chiếu (kể cả scripts và docs; docs chỉ cập nhật nếu mô tả hành vi hiện tại).
3. Script mới `code/scripts/enable-snapshot-replication.sh <branch_name>` (bash, `set -euo pipefail`, cùng phong cách và biến như `setup-replication.sh`), idempotent:
   a. Lấy `BRANCH_NUM_ID` từ DB chi nhánh: `SELECT id FROM branch WHERE upper(code)=upper('<branch>')`; không có → exit 1.
   b. Kiểm tra `relreplident = 'i'` cho 2 bảng ở cả HQ và Branch (V21 đã chạy) — sai → exit 1 kèm hướng dẫn.
   c. Nếu bảng chưa có trong `pg_publication_tables` của `pub_<branch>_to_hq` → `ALTER PUBLICATION ... ADD TABLE stock_on_hand WHERE (branch_id = N), receivable_debt WHERE (branch_id = N)`.
   d. Nếu `pg_subscription_rel` của `sub_hq_from_<branch>` chưa có 2 bảng: in cảnh báo + yêu cầu xác nhận (`read -p`), rồi tại HQ `DELETE FROM stock_on_hand WHERE branch_id = N; DELETE FROM receivable_debt WHERE branch_id = N;` và `ALTER SUBSCRIPTION ... REFRESH PUBLICATION WITH (copy_data = true)`. Cho phép bỏ qua xác nhận bằng cờ `--yes`.
   e. Chờ và kiểm tra `srsubstate = 'r'` cho 2 bảng; so sánh `COUNT(*)` theo `branch_id = N` giữa Branch và HQ; in kết quả.
4. `setup-replication.sh`: sau bước tạo subscription, gọi `enable-snapshot-replication.sh <branch> --yes` (chi nhánh mới). Không đổi danh sách `TRANSACTION_TABLES` cũ.
5. **Integration test replication:** `src/test/java/com/storename/erp/replication/SnapshotReplicationPostgresIT.java`:
   - 2 container `postgres:15-alpine` trên cùng `Network`, alias `hq-db`, `branch-db`, command `postgres -c wal_level=logical`.
   - Chạy Flyway (API `Flyway.configure()`) với `classpath:db/migration` cho cả hai (KHÔNG dùng `migration-branch` vì V9 cần role `erp_user`).
   - Branch giả lập chi nhánh id = 2: publication `stock_on_hand WHERE (branch_id = 2), receivable_debt WHERE (branch_id = 2)`; HQ subscription `copy_data = true` (connection tới `branch-db:5432`), trước đó xoá dòng `branch_id = 2` tại HQ.
   - Kịch bản assert (polling tối đa 15s, không dùng `Thread.sleep` cố định dài):
     1) INSERT `stock_on_hand` (product 1, branch 2, qty 7) ở Branch → HQ thấy qty 7.
     2) UPDATE qty → -3 ở Branch → HQ thấy -3.
     3) UPDATE dòng seed `branch_id = 1` ở Branch → HQ **không** bị đổi (row filter).
     4) `receivable_debt` tương tự cho (customer seed, branch 2).
     5) `relreplident = 'i'` cho 2 bảng.
   - Nếu Docker trong môi trường test không cho container nói chuyện qua network → dừng theo R2, báo người dùng, không "chữa cháy" bằng `@Disabled`.
6. Test bất biến HQ không ghi: bổ sung vào `ReplicationOwnershipHqTest` (hoặc test mới cùng kiểu) — HQ gọi `POST /api/v1/receivable-debts/opening-balance` với ADMIN không có branchId → 403; `POST /api/v1/inventory/inbound` → 404.
7. Cập nhật `REPLICATION_RUNBOOK.md` mục "Bật replicate bảng snapshot cho chi nhánh đang chạy" với lệnh cụ thể cho tp1, tp2 và bước kiểm tra.

**Phạm vi file:** V21 (mới); `InventoryAlertConfig.java`; xoá 4 file nêu ở bước 2; `code/scripts/enable-snapshot-replication.sh` (mới); `code/scripts/setup-replication.sh`; test mới/sửa ở bước 5, 6; `REPLICATION_RUNBOOK.md`, `DATABASE_REPLICATION.md` (nếu cần chỉnh chi tiết).
**KHÔNG làm:** chạy script trên Docker thật của người dùng (để FD-5); sửa `AnalyticsDataAdapter` (để FD-3).
**Verify:** `.\mvnw.cmd test` và `.\mvnw.cmd verify` → pass; `bash -n code/scripts/enable-snapshot-replication.sh` (kiểm cú pháp) → không lỗi.
**Điều kiện xong:** sprint doc `sprint-fd2-snapshot-replication.md` đủ 8 mục; mục 3 có ADR tóm tắt + `⚠️ BREAKING` (DROP `inventory_alert_log`, đảo quyết định Sprint 6); mục 7 có lệnh chạy script cho tp1/tp2.

=== KẾT THÚC PROMPT (FD-2) ===

---

## SPRINT FD-3 — Backend analytics & API cảnh báo

=== BẮT ĐẦU PROMPT GỬI GEMINI (FD-3) ===

> ### 🔒 KHỐI RULE BẮT BUỘC — ĐỌC LẠI TOÀN BỘ TRƯỚC KHI LÀM SPRINT NÀY
> (Khối này được copy nguyên văn vào mọi sprint. Không được bỏ qua với lý do "đã đọc ở sprint trước".)
>
> **R0. Trước khi viết bất kỳ dòng code nào**, mở và đọc lại 4 file sau:
> `.agents/AGENTS.md`, `.agents/rules/strict-debugging-circuit-breaker.md`, `.agents/rules/Sprint-docs-rule.md`, `code/docs/fix-dashboard/00-PLAN.md` (mục 2 "Quyết định đã chốt").
> Sau đó viết lại 3 dòng tóm tắt: (1) sprint này làm gì, (2) KHÔNG được làm gì, (3) điều kiện "xong".
>
> **R1. Kiến trúc (AGENTS.md)**
> - Modular monolith, bounded context: KHÔNG inject `*Repository` của module khác. Lấy tên sản phẩm/chi nhánh qua `CatalogFacade` / `BranchFacade`.
> - KHÔNG trả Entity ra API — luôn dùng DTO. Mọi REST trả `ApiResponse<T>` (`success, data, message, errors`).
> - Mọi Service/Controller có `@Slf4j` và log `info/debug/error`. KHÔNG ghi log vào DB.
> - Mọi class/hàm có business logic phải có Javadoc/JSDoc giải thích **why**.
> - Frontend: component KHÔNG gọi axios trực tiếp — đi qua `ApiService`; fetch data qua custom hook trong `src/hooks/`; hằng số (chu kỳ refresh, nhãn...) đặt trong `src/config/`.
> - Document-Driven: code phải khớp tài liệu trong `code/docs/fix-dashboard/00-PLAN.md` và `code/docs/architecture/*`. Nếu thấy tài liệu sai/thiếu → DỪNG, hỏi người dùng, KHÔNG tự đoán nghiệp vụ.
>
> **R2. Circuit Breaker khi gặp lỗi build/runtime/test (nguyên văn rule)**
> 1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng. Nếu đã sửa code và chạy lại từ 2 lần trở lên mà lỗi VẪN xuất hiện (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace), phải NGAY LẬP TỨC dừng việc sửa code. KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành bước 2.
> 2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi, trả lời đủ 5 mục: (1) Nguyên văn lỗi mới nhất (copy chính xác). (2) Giả thuyết đã thử là gì và tại sao nó KHÔNG đúng — nếu không biết, ghi rõ "tôi không biết vì sao lần trước thất bại". (3) Lỗi nằm ở lớp nào, chọn đúng 1: `syntax` / `logic` / `dependency-version` / `config-env` / `data-state` / `race-condition` / `unknown`. (4) Bằng chứng cụ thể (dán đúng đoạn code, log/print giá trị thực tế, hoặc doc/version của lib). (5) Kế hoạch sửa — chỉ 1 thay đổi duy nhất.
> 3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi. Sau 3 lần vẫn lỗi → KHÔNG thử lần 4; dừng hẳn, tóm tắt những gì đã thử/loại trừ, hỏi người dùng kèm 2–3 hướng khả dĩ.
> 4. Cấm: sửa code mà không trích lại đúng đoạn log lỗi mới nhất; nói "thử cách khác xem sao" mà không nêu giả thuyết; sửa nhiều file/nhiều chỗ trong 1 lần khi chưa xác định root cause; nuốt exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi".
> 5. Lỗi liên quan version/dependency/library: phải tra doc/changelog thật hoặc hỏi người dùng version TRƯỚC khi đoán API (không bịa API).
>
> **R3. Tài liệu kết thúc sprint (Sprint-docs-rule.md)**
> - Tạo `code/docs/fix-dashboard/sprint-<mã>-<tên-ngắn>.md`, dòng đầu ghi ngày hoàn thành + mã sprint.
> - Đủ 8 mục: 1. Mục tiêu · 2. Thành quả (có đường dẫn file) · 3. Quyết định kiến trúc & lý do (có phương án đã bỏ + lý do bỏ) · 4. Hướng dẫn đọc code theo thứ tự · 5. Rủi ro/Nợ kỹ thuật · 6. Việc chưa làm/Out of scope · 7. Cách chạy & verify (lệnh copy-paste + kết quả mong đợi) · 8. Handoff sprint sau.
> - Viết cho người đọc lần đầu; không viết code chi tiết trong docs (chỉ trỏ file + why); cấm tính từ tự khen ("xuất sắc", "hoàn hảo", "chuẩn"); thay đổi hành vi/API đánh dấu `⚠️ BREAKING`.
> - Thêm 1 dòng vào `code/docs/fix-dashboard/README.md` (số sprint, tên, ngày, 1 dòng tóm tắt).
> - Cuối sprint tự điền checklist ✅/❌ 10 mục (8 mục + đã thêm link README + không có tính từ mơ hồ). Còn ❌ → chưa xong.
>
> **R4. Kỷ luật phạm vi cho Gemini**
> - Chỉ sửa các file liệt kê trong "Phạm vi file" của sprint. Cần sửa file ngoài danh sách → ghi lý do vào sprint doc mục 3 trước khi sửa.
> - KHÔNG sửa migration cũ `V1`…`V20` (Flyway checksum). Thay đổi schema chỉ qua migration mới.
> - KHÔNG xoá file bằng `rm` mà không ghi vào sprint doc; KHÔNG `git commit`/`git push` trừ khi người dùng yêu cầu.
> - Mỗi sprint kết thúc bằng việc chạy đúng các lệnh trong mục "Verify" và dán kết quả (số test pass/fail) vào sprint doc mục 7.
> - Không chắc → hỏi. Không tự mở rộng scope ("tiện tay sửa luôn").

**Bối cảnh:** Đọc `00-PLAN.md` mục 2 (toàn bộ), 3.1, 3.2, 3.3 và `code/docs/fix-dashboard/business-rules.md`. FD-1 đã sửa lỗi 500 và thêm `AnalyticsSqlParams`; FD-2 đã có V21, replicate 2 bảng snapshot, xoá job cũ.

**Việc cần làm:**
1. `AnalyticsDataPort` + `AnalyticsDataAdapter` (giữ quy ước 3.3):
   - `SalesTotals getSalesTotals(branchId, fromTs, toTs)` → `revenue` (`SUM(line_total)`), `cogs` (`SUM(quantity * COALESCE(unit_cost,0))`); lọc `status='CONFIRMED'`, `is_deleted=false` (cả hóa đơn và dòng).
   - `List<ProductPerformanceDto> getTopProductsByRevenue(branchId, fromTs, toTs, limit=10)` — `ORDER BY revenue DESC`.
   - `BigDecimal getInventoryValue(branchId)` = `SUM(remaining_qty * unit_cost)` từ `cost_layer`.
   - `BigDecimal getTotalReceivableDebt(branchId)` = `SUM(GREATEST(total_debt,0))` từ `receivable_debt` `is_deleted=false` (M-07) — bỏ nhánh `sales_invoice.remaining_debt`.
   - `List<StockLevel> getStockLevels(branchId)` (record `productId, branchId, quantity`) từ `stock_movement` (mục 3.2 bước 1).
   - `List<Long> getProductIdsSoldInPeriod(branchId, fromTs, toTs)` (phục vụ bán chậm).
   - Xoá hàm không còn dùng. Mỗi hàm có Javadoc nêu nguồn bảng + lý do.
2. `DashboardService`: tính `grossProfit = revenue - cogs`; `inventoryTurnoverRatio` theo M-01 (`RoundingMode.HALF_UP`, scale 2, mẫu = 0 → 0); `slowMovingProducts` theo M-02 (gộp tồn theo product trong phạm vi lọc, tên/mã qua `CatalogFacade`, top 10).
3. DTO: đổi `totalOverdueDebt` → `totalReceivableDebt` (⚠️ BREAKING); thêm `SlowMovingProductDto`; cập nhật `ReportExportService` (dòng "Total Receivable Debt"), không thêm sheet (D-12).
4. Cảnh báo:
   - `analytics/domain/StockAlertType` (enum `NEGATIVE_STOCK`, `LOW_STOCK`).
   - `analytics/application/StockAlertService` theo **mục 3.2** (bước 1–7). `InventoryAlertConfigRepository`: thêm `findByIsActiveTrueAndBranchId(Long)`.
   - DTO `StockAlertDto`, `StockAlertSummaryDto` trong `analytics/api/dto`.
   - Endpoint `GET /api/v1/analytics/stock-alerts` trong `DashboardController` (hoặc `StockAlertController` mới cùng điều kiện `ConditionalOnExpression` HQ/ALL + `@PreAuthorize("hasAuthority('ADMIN')")`).
5. Test:
   - Unit `StockAlertServiceTest` (Mockito) — bắt buộc các case: tồn âm không có config → NEGATIVE; tồn âm có config → NEGATIVE (có ngưỡng); config nhưng chưa có movement (tồn 0, ngưỡng 5) → LOW; tồn = ngưỡng → LOW; tồn > ngưỡng → không; config `is_active=false` → bỏ qua; lọc branch; thứ tự sắp xếp; Facade chỉ được gọi 1 lần (verify times(1)).
   - Unit `DashboardServiceTest`: turnover (mẫu 0 và khác 0), gross profit, debt null → 0.
   - IT Postgres `AnalyticsDataAdapterPostgresIT` mở rộng: mỗi query mới với `branchId = null` và có giá trị; `getStockLevels` khớp tổng movement fixture; debt bỏ qua số dư âm.
   - IT `StockAlertApiPostgresIT` (SpringBootTest + MockMvc, profile `postgres-it`): tạo fixture movement âm + config → gọi API → JSON đúng thứ tự, đúng count.
   - `DashboardControllerTest`: field mới, endpoint mới 200 với ADMIN, 403 với STAFF.
6. Regenerate OpenAPI: chạy `OpenApiGeneratorTest` (ghi `packages/api-contract/openapi.json`) — xem cách nó chạy trong file test trước khi chạy.

**Phạm vi file:** `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/analytics/**`, test tương ứng, `packages/api-contract/openapi.json` (sinh tự động).
**KHÔNG làm:** frontend; sửa module khác (chỉ gọi Facade có sẵn — nếu Facade thiếu hàm cần thiết thì DỪNG hỏi người dùng); bật `@EnableScheduling`.
**Verify:** `.\mvnw.cmd test` và `.\mvnw.cmd verify` pass; dán số lượng test.
**Điều kiện xong:** sprint doc `sprint-fd3-analytics-backend-alerts.md` đủ 8 mục; mục 3 nêu lý do tách endpoint (M-12), lý do dùng `stock_movement` (D-07), lý do không JOIN chéo context.

=== KẾT THÚC PROMPT (FD-3) ===

---

## SPRINT FD-4 — Frontend dashboard & cảnh báo

=== BẮT ĐẦU PROMPT GỬI GEMINI (FD-4) ===

> ### 🔒 KHỐI RULE BẮT BUỘC — ĐỌC LẠI TOÀN BỘ TRƯỚC KHI LÀM SPRINT NÀY
> (Khối này được copy nguyên văn vào mọi sprint. Không được bỏ qua với lý do "đã đọc ở sprint trước".)
>
> **R0. Trước khi viết bất kỳ dòng code nào**, mở và đọc lại 4 file sau:
> `.agents/AGENTS.md`, `.agents/rules/strict-debugging-circuit-breaker.md`, `.agents/rules/Sprint-docs-rule.md`, `code/docs/fix-dashboard/00-PLAN.md` (mục 2 "Quyết định đã chốt").
> Sau đó viết lại 3 dòng tóm tắt: (1) sprint này làm gì, (2) KHÔNG được làm gì, (3) điều kiện "xong".
>
> **R1. Kiến trúc (AGENTS.md)**
> - Modular monolith, bounded context: KHÔNG inject `*Repository` của module khác. Lấy tên sản phẩm/chi nhánh qua `CatalogFacade` / `BranchFacade`.
> - KHÔNG trả Entity ra API — luôn dùng DTO. Mọi REST trả `ApiResponse<T>` (`success, data, message, errors`).
> - Mọi Service/Controller có `@Slf4j` và log `info/debug/error`. KHÔNG ghi log vào DB.
> - Mọi class/hàm có business logic phải có Javadoc/JSDoc giải thích **why**.
> - Frontend: component KHÔNG gọi axios trực tiếp — đi qua `ApiService`; fetch data qua custom hook trong `src/hooks/`; hằng số (chu kỳ refresh, nhãn...) đặt trong `src/config/`.
> - Document-Driven: code phải khớp tài liệu trong `code/docs/fix-dashboard/00-PLAN.md` và `code/docs/architecture/*`. Nếu thấy tài liệu sai/thiếu → DỪNG, hỏi người dùng, KHÔNG tự đoán nghiệp vụ.
>
> **R2. Circuit Breaker khi gặp lỗi build/runtime/test (nguyên văn rule)**
> 1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng. Nếu đã sửa code và chạy lại từ 2 lần trở lên mà lỗi VẪN xuất hiện (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace), phải NGAY LẬP TỨC dừng việc sửa code. KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành bước 2.
> 2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi, trả lời đủ 5 mục: (1) Nguyên văn lỗi mới nhất (copy chính xác). (2) Giả thuyết đã thử là gì và tại sao nó KHÔNG đúng — nếu không biết, ghi rõ "tôi không biết vì sao lần trước thất bại". (3) Lỗi nằm ở lớp nào, chọn đúng 1: `syntax` / `logic` / `dependency-version` / `config-env` / `data-state` / `race-condition` / `unknown`. (4) Bằng chứng cụ thể (dán đúng đoạn code, log/print giá trị thực tế, hoặc doc/version của lib). (5) Kế hoạch sửa — chỉ 1 thay đổi duy nhất.
> 3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi. Sau 3 lần vẫn lỗi → KHÔNG thử lần 4; dừng hẳn, tóm tắt những gì đã thử/loại trừ, hỏi người dùng kèm 2–3 hướng khả dĩ.
> 4. Cấm: sửa code mà không trích lại đúng đoạn log lỗi mới nhất; nói "thử cách khác xem sao" mà không nêu giả thuyết; sửa nhiều file/nhiều chỗ trong 1 lần khi chưa xác định root cause; nuốt exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi".
> 5. Lỗi liên quan version/dependency/library: phải tra doc/changelog thật hoặc hỏi người dùng version TRƯỚC khi đoán API (không bịa API).
>
> **R3. Tài liệu kết thúc sprint (Sprint-docs-rule.md)**
> - Tạo `code/docs/fix-dashboard/sprint-<mã>-<tên-ngắn>.md`, dòng đầu ghi ngày hoàn thành + mã sprint.
> - Đủ 8 mục: 1. Mục tiêu · 2. Thành quả (có đường dẫn file) · 3. Quyết định kiến trúc & lý do (có phương án đã bỏ + lý do bỏ) · 4. Hướng dẫn đọc code theo thứ tự · 5. Rủi ro/Nợ kỹ thuật · 6. Việc chưa làm/Out of scope · 7. Cách chạy & verify (lệnh copy-paste + kết quả mong đợi) · 8. Handoff sprint sau.
> - Viết cho người đọc lần đầu; không viết code chi tiết trong docs (chỉ trỏ file + why); cấm tính từ tự khen ("xuất sắc", "hoàn hảo", "chuẩn"); thay đổi hành vi/API đánh dấu `⚠️ BREAKING`.
> - Thêm 1 dòng vào `code/docs/fix-dashboard/README.md` (số sprint, tên, ngày, 1 dòng tóm tắt).
> - Cuối sprint tự điền checklist ✅/❌ 10 mục (8 mục + đã thêm link README + không có tính từ mơ hồ). Còn ❌ → chưa xong.
>
> **R4. Kỷ luật phạm vi cho Gemini**
> - Chỉ sửa các file liệt kê trong "Phạm vi file" của sprint. Cần sửa file ngoài danh sách → ghi lý do vào sprint doc mục 3 trước khi sửa.
> - KHÔNG sửa migration cũ `V1`…`V20` (Flyway checksum). Thay đổi schema chỉ qua migration mới.
> - KHÔNG xoá file bằng `rm` mà không ghi vào sprint doc; KHÔNG `git commit`/`git push` trừ khi người dùng yêu cầu.
> - Mỗi sprint kết thúc bằng việc chạy đúng các lệnh trong mục "Verify" và dán kết quả (số test pass/fail) vào sprint doc mục 7.
> - Không chắc → hỏi. Không tự mở rộng scope ("tiện tay sửa luôn").

**Bối cảnh:** Đọc `00-PLAN.md` mục 2, 3.1, 3.6 và `code/docs/architecture/API_CONTRACT.md` (mục Analytics). Backend FD-3 đã có `totalReceivableDebt`, `slowMovingProducts`, `GET /api/v1/analytics/stock-alerts`. Frontend: React 19 + Vite + TanStack Query v5 + Tailwind, thư mục `code/erp-platform/apps/erp-frontend`. (File `AGENTS.md` trong thư mục frontend nói về Next.js — KHÔNG áp dụng cho app này, app dùng Vite.)

**Việc cần làm:**
1. `src/types/analytics.ts`: theo mục 3.6 (đổi tên field, thêm `SlowMovingProduct`, `StockAlert`, `StockAlertSummary`, union `'NEGATIVE_STOCK' | 'LOW_STOCK'`).
2. `src/api/ApiService.ts`: thêm `Analytics.getStockAlerts(branchId?: number): Promise<StockAlertSummary>` theo đúng mẫu các hàm Analytics hiện có (URLSearchParams, `.then(res => res.data.data)`), có kiểu trả về.
3. `src/config/dashboard.ts` (mới): `DASHBOARD_REFETCH_INTERVAL_MS = 60_000`; map nhãn/ tone cho loại cảnh báo (`NEGATIVE_STOCK` → "Tồn âm"/danger; `LOW_STOCK` → "Sắp hết"/warning).
4. Hook mới: `src/hooks/useDashboardMetrics.ts`, `src/hooks/useStockAlerts.ts` (queryKey gồm branchId + kỳ; `refetchInterval` từ config; `refetchIntervalInBackground: false`). Có JSDoc.
5. `src/components/sales/StockAlertPanel.tsx` (mới) theo mục 3.6. Component chỉ nhận props, không gọi API. Dùng class/token có sẵn (`card`, `badge-danger`, `erp-table`, …); nếu chưa có class cho mức cảnh báo vàng → kiểm tra `index.css`/design tokens trước, không hard-code màu hex.
6. `Dashboard.tsx`:
   - Dùng 2 hook mới; panel cảnh báo đặt ngay dưới `PageHeader`, trên 4 thẻ KPI; lọc theo `branchId` đang chọn.
   - Thẻ công nợ: tiêu đề "Tổng công nợ phải thu", subtitle "Tổng nợ khách hàng theo chi nhánh", đọc `totalReceivableDebt`.
   - Tiêu đề top: "Top sản phẩm theo doanh thu" (backend đã sắp theo doanh thu).
   - Thêm card "Sản phẩm bán chậm" (bảng: Mã SP, Tên SP, Tồn hiện tại; trống → "Không có sản phẩm tồn kho mà không bán trong kỳ").
   - Hiển thị "Cập nhật lúc HH:mm:ss" (dùng `dataUpdatedAt` của query). Nút "Làm mới" refetch cả 2 query.
   - Lỗi API cảnh báo chỉ hiển thị lỗi trong panel, KPI vẫn hiển thị.
7. Test:
   - Vitest `src/components/sales/__tests__/StockAlertPanel.test.tsx`: có cảnh báo (đúng số dòng, badge đúng loại, ngưỡng null hiển thị "—"), không cảnh báo, lỗi. (Kiểm tra `vitest.setup.ts` đã có Testing Library chưa; thiếu thư viện → DỪNG hỏi người dùng trước khi cài.)
   - Playwright `tests/ui/dashboard.spec.ts`: sửa mock (`totalReceivableDebt`, `slowMovingProducts`), thêm route mock `**/api/v1/analytics/stock-alerts*`, assert panel hiển thị. Không sửa `tests/_legacy`.
8. Kiểm tra lại mọi chỗ dùng `totalOverdueDebt` trong frontend (grep) → không còn.

**Phạm vi file:** các file nêu trên trong `apps/erp-frontend`.
**KHÔNG làm:** sửa backend; thêm thư viện mới khi chưa hỏi; đổi layout các trang khác.
**Verify:** `npm run lint`, `npm run build`, `npx vitest run` pass; `npm run test:e2e` nếu môi trường có Playwright browser (không có → ghi rõ).
**Điều kiện xong:** sprint doc `sprint-fd4-frontend-dashboard-alerts.md` đủ 8 mục; mục 3 nêu lý do 2 query độc lập + chu kỳ 60s (M-09).

=== KẾT THÚC PROMPT (FD-4) ===

---

## SPRINT FD-5 — Kiểm thử E2E trên Docker & đóng đợt

=== BẮT ĐẦU PROMPT GỬI GEMINI (FD-5) ===

> ### 🔒 KHỐI RULE BẮT BUỘC — ĐỌC LẠI TOÀN BỘ TRƯỚC KHI LÀM SPRINT NÀY
> (Khối này được copy nguyên văn vào mọi sprint. Không được bỏ qua với lý do "đã đọc ở sprint trước".)
>
> **R0. Trước khi viết bất kỳ dòng code nào**, mở và đọc lại 4 file sau:
> `.agents/AGENTS.md`, `.agents/rules/strict-debugging-circuit-breaker.md`, `.agents/rules/Sprint-docs-rule.md`, `code/docs/fix-dashboard/00-PLAN.md` (mục 2 "Quyết định đã chốt").
> Sau đó viết lại 3 dòng tóm tắt: (1) sprint này làm gì, (2) KHÔNG được làm gì, (3) điều kiện "xong".
>
> **R1. Kiến trúc (AGENTS.md)**
> - Modular monolith, bounded context: KHÔNG inject `*Repository` của module khác. Lấy tên sản phẩm/chi nhánh qua `CatalogFacade` / `BranchFacade`.
> - KHÔNG trả Entity ra API — luôn dùng DTO. Mọi REST trả `ApiResponse<T>` (`success, data, message, errors`).
> - Mọi Service/Controller có `@Slf4j` và log `info/debug/error`. KHÔNG ghi log vào DB.
> - Mọi class/hàm có business logic phải có Javadoc/JSDoc giải thích **why**.
> - Frontend: component KHÔNG gọi axios trực tiếp — đi qua `ApiService`; fetch data qua custom hook trong `src/hooks/`; hằng số (chu kỳ refresh, nhãn...) đặt trong `src/config/`.
> - Document-Driven: code phải khớp tài liệu trong `code/docs/fix-dashboard/00-PLAN.md` và `code/docs/architecture/*`. Nếu thấy tài liệu sai/thiếu → DỪNG, hỏi người dùng, KHÔNG tự đoán nghiệp vụ.
>
> **R2. Circuit Breaker khi gặp lỗi build/runtime/test (nguyên văn rule)**
> 1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng. Nếu đã sửa code và chạy lại từ 2 lần trở lên mà lỗi VẪN xuất hiện (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace), phải NGAY LẬP TỨC dừng việc sửa code. KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành bước 2.
> 2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi, trả lời đủ 5 mục: (1) Nguyên văn lỗi mới nhất (copy chính xác). (2) Giả thuyết đã thử là gì và tại sao nó KHÔNG đúng — nếu không biết, ghi rõ "tôi không biết vì sao lần trước thất bại". (3) Lỗi nằm ở lớp nào, chọn đúng 1: `syntax` / `logic` / `dependency-version` / `config-env` / `data-state` / `race-condition` / `unknown`. (4) Bằng chứng cụ thể (dán đúng đoạn code, log/print giá trị thực tế, hoặc doc/version của lib). (5) Kế hoạch sửa — chỉ 1 thay đổi duy nhất.
> 3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi. Sau 3 lần vẫn lỗi → KHÔNG thử lần 4; dừng hẳn, tóm tắt những gì đã thử/loại trừ, hỏi người dùng kèm 2–3 hướng khả dĩ.
> 4. Cấm: sửa code mà không trích lại đúng đoạn log lỗi mới nhất; nói "thử cách khác xem sao" mà không nêu giả thuyết; sửa nhiều file/nhiều chỗ trong 1 lần khi chưa xác định root cause; nuốt exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi".
> 5. Lỗi liên quan version/dependency/library: phải tra doc/changelog thật hoặc hỏi người dùng version TRƯỚC khi đoán API (không bịa API).
>
> **R3. Tài liệu kết thúc sprint (Sprint-docs-rule.md)**
> - Tạo `code/docs/fix-dashboard/sprint-<mã>-<tên-ngắn>.md`, dòng đầu ghi ngày hoàn thành + mã sprint.
> - Đủ 8 mục: 1. Mục tiêu · 2. Thành quả (có đường dẫn file) · 3. Quyết định kiến trúc & lý do (có phương án đã bỏ + lý do bỏ) · 4. Hướng dẫn đọc code theo thứ tự · 5. Rủi ro/Nợ kỹ thuật · 6. Việc chưa làm/Out of scope · 7. Cách chạy & verify (lệnh copy-paste + kết quả mong đợi) · 8. Handoff sprint sau.
> - Viết cho người đọc lần đầu; không viết code chi tiết trong docs (chỉ trỏ file + why); cấm tính từ tự khen ("xuất sắc", "hoàn hảo", "chuẩn"); thay đổi hành vi/API đánh dấu `⚠️ BREAKING`.
> - Thêm 1 dòng vào `code/docs/fix-dashboard/README.md` (số sprint, tên, ngày, 1 dòng tóm tắt).
> - Cuối sprint tự điền checklist ✅/❌ 10 mục (8 mục + đã thêm link README + không có tính từ mơ hồ). Còn ❌ → chưa xong.
>
> **R4. Kỷ luật phạm vi cho Gemini**
> - Chỉ sửa các file liệt kê trong "Phạm vi file" của sprint. Cần sửa file ngoài danh sách → ghi lý do vào sprint doc mục 3 trước khi sửa.
> - KHÔNG sửa migration cũ `V1`…`V20` (Flyway checksum). Thay đổi schema chỉ qua migration mới.
> - KHÔNG xoá file bằng `rm` mà không ghi vào sprint doc; KHÔNG `git commit`/`git push` trừ khi người dùng yêu cầu.
> - Mỗi sprint kết thúc bằng việc chạy đúng các lệnh trong mục "Verify" và dán kết quả (số test pass/fail) vào sprint doc mục 7.
> - Không chắc → hỏi. Không tự mở rộng scope ("tiện tay sửa luôn").

**Bối cảnh:** FD-0 → FD-4 đã xong. Sprint này chạy hệ thống thật (HQ + TP1 + TP2 bằng `docker compose` trong thư mục `code`) và đóng đợt.
**Cảnh báo:** Bước 3 có xoá dữ liệu snapshot của chi nhánh tại HQ (sẽ được copy lại ngay). PHẢI hỏi người dùng xác nhận trước khi chạy, và gợi ý backup: `docker exec code-hq-db-1 pg_dump -U erp_user -d erp_hq -t stock_on_hand -t receivable_debt > backup_snapshot_hq.sql`.

**Việc cần làm:**
1. Build & chạy lại toàn bộ: `docker compose up -d --build` → kiểm tra 3 app khởi động, Flyway đạt V21 ở cả 3 DB: `bash scripts/check-schema-version.sh tp1`, `... tp2`.
2. Kiểm tra replication cũ vẫn chạy (`pg_stat_subscription` tại HQ).
3. (Sau khi người dùng xác nhận) `bash scripts/enable-snapshot-replication.sh tp1`, rồi `tp2`. Ghi output vào sprint doc.
4. Kịch bản kiểm thử thủ công (ghi kết quả từng bước, có ảnh chụp nếu được):
   a. Admin mở Dashboard, "Tất cả chi nhánh", các kỳ Hôm nay/Tháng này/Năm nay → không 500.
   b. Tại HQ nhập ngưỡng: sản phẩm X ở TP1 ngưỡng = tồn hiện tại + 5 (SQL mẫu trong `business-rules.md`) → trong ≤ 60s dashboard hiện "Sắp hết" cho X–TP1; chọn TP2 → không hiện.
   c. Tại TP1 (staff_tp1) bán sản phẩm Y nhiều hơn tồn → tồn âm → trong ≤ 60s + độ trễ replication, dashboard HQ hiện "Tồn âm" cho Y–TP1 (dù Y không có cấu hình ngưỡng).
   d. Nhập hàng Y tại TP1 cho tồn dương lại → cảnh báo biến mất.
   e. Đối soát: tại HQ `SELECT product_id, branch_id, SUM(quantity) FROM stock_movement GROUP BY 1,2` so với `stock_on_hand` cùng cặp → khớp (sau độ trễ). Công nợ thẻ KPI khớp `SELECT SUM(GREATEST(total_debt,0)) FROM receivable_debt` tại HQ và tổng tại các chi nhánh.
   f. Xuất Excel vẫn chạy, có dòng "Total Receivable Debt".
   Bước nào sai → áp dụng R2 (Circuit Breaker), không sửa lan man.
5. Chạy lại toàn bộ test: backend `.\mvnw.cmd verify`; frontend `npm run build`, `npx vitest run`.
6. Cập nhật tài liệu đóng đợt:
   - `code/docs/CHANGELOG.md`: mục "fix-dashboard" (liệt kê ⚠️ BREAKING: đổi tên field, DROP `inventory_alert_log`, replicate snapshot tables).
   - `code/docs/AI_CONTEXT.md`: sửa dòng "HQ Analytics dynamically aggregates stock and debt ... rather than copying snapshot tables" cho đúng hiện trạng mới; ghi `stock_on_hand`, `receivable_debt` replicate có row filter.
   - `code/docs/FOUND_ISSUES.md`: thêm mục fix-dashboard (P1–P8 trạng thái).
   - `code/docs/plans/analytics-dashboard-alert-questions.md`: thêm dòng đầu "Đã xử lý trong đợt fix-dashboard, xem `docs/fix-dashboard/`".
   - `code/docs/fix-dashboard/README.md`: bảng tổng kết 6 sprint.

**Phạm vi file:** tài liệu nêu trên + sửa lỗi nhỏ phát hiện khi E2E (mỗi lỗi ghi rõ vào sprint doc).
**Điều kiện xong:** kịch bản 4a–4f đạt; sprint doc `sprint-fd5-e2e-closeout.md` đủ 8 mục; mục 5 liệt kê toàn bộ Tech Debt còn lại (ít nhất: ETL/fact tables chưa dùng; doanh thu chưa trừ hàng trả; vòng quay dùng tồn hiện tại thay vì bình quân; `receivable_debt_movement` chưa replicate nên lịch sử công nợ ở HQ trống; seed V2/V13 tạo dữ liệu branch 1 trong mọi DB; cảnh báo cho chi nhánh chưa có; email chưa có SMTP).

=== KẾT THÚC PROMPT (FD-5) ===

---

## 5. Rủi ro chính & cách phòng

| Rủi ro | Phòng ngừa |
|---|---|
| Gemini quên rule giữa các sprint | Khối rule copy nguyên văn đầu mỗi prompt + bước R0 bắt tóm tắt lại. |
| Row filter publication lỗi vì replica identity | V21 đặt `REPLICA IDENTITY USING INDEX`; script kiểm tra `relreplident` trước khi ALTER PUBLICATION. |
| Xoá nhầm dữ liệu HQ khi bật replicate | Chỉ `DELETE ... WHERE branch_id = N`, có xác nhận + gợi ý backup; không TRUNCATE. |
| Test Postgres pass nhưng Docker thật lỗi | FD-5 chạy kịch bản thủ công trên compose thật. |
| Đổi tên field làm vỡ client khác | Chỉ frontend ERP dùng endpoint này (đã grep); ghi ⚠️ BREAKING trong CHANGELOG/API_CONTRACT. |
| Hiệu năng `SUM(stock_movement)` khi dữ liệu lớn | Đã có index `(product_id, branch_id, created_at)`; ghi Tech Debt (materialized view) nếu > vài trăm nghìn dòng. |
