# KẾ HOẠCH THỰC HIỆN — Cải thiện UI/UX `erp-frontend`

> **Người thực hiện:** Gemini (AI coding agent)
> **Người duyệt:** chủ dự án
> **Nguồn quyết định:** `docs/UI_UX_QUESTIONS.md` (đã được trả lời ngày 27/09/2026)
> **Phạm vi:** chỉ thư mục `apps/erp-frontend`. **Không sửa backend.**

---

## PHẦN 0 — ĐỌC TRƯỚC KHI LÀM (BẮT BUỘC)

### 0.1 Cách làm việc

1. Làm **tuần tự từng sprint**, không nhảy cóc. Mỗi sprint có mục **"Checkpoint"**: chạy lệnh kiểm tra, ghi mục log `CHECKPOINT` vào `docs/GEMINI_ACTION_LOG.md` (mục 0.4), viết báo cáo ngắn (đã làm gì, file nào đổi, test nào pass/fail), rồi **dừng lại chờ người duyệt** trước khi sang sprint sau.
2. Trong một sprint, làm theo đúng thứ tự các bước. Mỗi bước nhỏ, sửa xong chạy được ngay.
3. Khi gặp điều **không có trong tài liệu này** hoặc tài liệu mâu thuẫn với code thực tế → **dừng lại và hỏi**, không tự đoán.
4. Nếu dự án có git: tạo nhánh `feat/ui-ux-revamp`, **commit sau mỗi sprint** với message `ui: sprint N - <tóm tắt>`.

### 0.2 Mười quy tắc không được vi phạm

| # | Quy tắc |
|---|---|
| R1 | **Đây là Vite + React 19, KHÔNG phải Next.js.** File `AGENTS.md` và `tsconfig.json` có dòng nhắc tới Next.js — đó là rác do công cụ khác sinh ra, **bỏ qua**. Không thêm `'use client'`, không import `next/*`, không tạo thư mục `app/` hay `pages/`. |
| R2 | **Tailwind v4.3.3.** Không tạo `tailwind.config.js`. Mọi token nằm trong khối `@theme { }` của `src/index.css`. Không bao giờ dùng lại `@tailwind base/components/utilities`. |
| R3 | **Không sửa:** `services/erp-backend/**`, `packages/api-contract/**` (file generated), `vite.config.ts` (phần proxy), logic trong `src/context/AuthContext.tsx`, công thức tính tiền trong `SalesOrderForm.tsx`, payload tạo hóa đơn bán hàng. |
| R4 | **Giữ nguyên các `data-testid` đang có** (danh sách ở mục 0.6). Chỉ được thêm testid mới. Khi đổi cấu trúc mà testid cũ không còn chỗ gắn (ghi rõ trong sprint), phải làm đúng như sprint hướng dẫn. |
| R5 | **Không cài thêm thư viện** (không MUI, shadcn, headlessui, react-select, floating-ui, dayjs…). Dùng những gì đã có: React 19, `react-dom` (`createPortal`), `lucide-react`, `recharts`, `@tanstack/react-query`, `react-hot-toast`. Không chạy `npm audit fix` hay nâng version. |
| R6 | **Không xóa file.** File thừa → di chuyển vào `apps/erp-frontend/_to_delete/` (giữ cấu trúc thư mục con). |
| R7 | Mọi chữ hiển thị trên UI là **tiếng Việt có dấu**. Mọi file lưu **UTF-8** (lưu ý `DESIGN_SYSTEM.md` hiện đang là UTF-16 — khi viết lại phải lưu UTF-8). |
| R8 | **Không dùng màu hard-code** kiểu `#1D4ED8`, `bg-blue-600`, `text-indigo-*`, `emerald-*`, `purple-*`, `pink-*` trong component. Chỉ dùng token ngữ nghĩa ở Phần 2 (`bg-primary`, `text-ink-muted`, `border-line`…) và thang xám `slate-*` khi thật cần. |
| R9 | Không đổi text tiêu đề các trang danh mục (`Danh Mục Sản Phẩm`, `Danh Mục Khách Hàng`…) vì test đang dựa vào. |
| R10 | **Tính năng cảnh báo tồn kho (alert) KHÔNG làm trong đợt này.** Không thêm khối cảnh báo, badge, cột "Tồn" trong form hay trong dropdown, không gọi API tồn kho từ form/dashboard. |
| R11 | **Ghi nhật ký mọi hành động** vào `apps/erp-frontend/docs/GEMINI_ACTION_LOG.md` theo đúng định dạng ở mục 0.4. Ghi **ngay sau mỗi hành động**, không dồn đến cuối sprint. Không được sửa hay xóa dòng log cũ (chỉ ghi thêm). Hành động không có trong log được coi là **chưa làm**. Nhật ký này dùng để rà soát toàn bộ công việc sau khi hoàn thành. |

### 0.3 Lệnh cần dùng

Máy chạy **Windows**. Mọi lệnh chạy trong `apps/erp-frontend` (dự án là npm workspaces; nếu thiếu package thì chạy `npm install` ở thư mục gốc `erp-platform`).

| Mục đích | Lệnh |
|---|---|
| Chạy dev | `npm run dev` → http://localhost:5173 |
| Build | `npm run build` (chỉ bundle, **không** kiểm tra type) |
| Kiểm tra type | `npx tsc --noEmit -p tsconfig.json` |
| Lint | `npm run lint` |
| Unit test | `npx vitest run` |
| E2E test | `npx playwright test tests/ui` (lần đầu: `npx playwright install chromium`). Playwright tự bật `npm run dev`; **không cần backend** vì mọi API đều được mock. |

### 0.4 Nhật ký hành động (`docs/GEMINI_ACTION_LOG.md`) — bắt buộc theo R11

**Mục đích:** người rà soát đọc file này để biết chính xác Gemini đã làm gì, ở đâu, vì sao, kết quả ra sao — không cần đoán từ diff.

**Khác với `docs/UI_UX_PROGRESS.md`:** file PROGRESS là **tóm tắt** theo sprint; file LOG là **nhật ký chi tiết từng hành động**. Cả hai đều phải có.

**Phải ghi log khi:**

| Loại (`TYPE`) | Khi nào |
|---|---|
| `CREATE` | Tạo file mới |
| `EDIT` | Sửa file (mỗi file một mục; sửa nhiều lần cùng file trong cùng một bước thì gộp một mục) |
| `MOVE` | Chuyển file (vd vào `_to_delete/`, `tests/_legacy/`) |
| `RUN` | Chạy lệnh (build, lint, tsc, vitest, playwright, npm install…) |
| `DECISION` | Tự chọn một cách làm khi plan không nói rõ hoặc có nhiều cách |
| `DEVIATION` | Làm **khác** plan (bất kể lý do) — đánh dấu `⚠️ LỆCH PLAN` |
| `ISSUE` | Phát hiện lỗi/vướng mắc chưa giải quyết được, hoặc lỗi có sẵn từ trước |
| `QUESTION` | Dừng lại hỏi người duyệt (ghi cả câu trả lời khi nhận được) |
| `CHECKPOINT` | Kết thúc sprint, trước khi chờ duyệt |

**Không cần ghi:** thao tác chỉ đọc file (`READ`), trừ khi việc đọc dẫn tới một `DECISION` hoặc `ISSUE`.

**Định dạng mỗi mục** (Markdown, ghi nối tiếp vào cuối file):

```markdown
### [#023] 2026-09-28 14:05 · Sprint 2 · Bước 2.2 · EDIT
- **File:** `src/components/common/SearchableCombobox.tsx`
- **Việc đã làm:** Chuyển dropdown sang createPortal vào document.body; thêm tính vị trí lật lên/xuống; debounce 250ms.
- **Lý do:** Theo plan C1 — dropdown bị overflow của bảng cắt mất.
- **Ảnh hưởng:** Thêm testid `combobox-dropdown`. Không đổi props cũ.
- **Kết quả:** tsc 0 lỗi mới; U-CB-01..05 pass.
```

Quy tắc định dạng:
1. Số thứ tự `[#NNN]` tăng liên tục từ `#001` trong toàn bộ file, không đánh lại theo sprint.
2. Thời gian theo giờ máy (`yyyy-MM-dd HH:mm`).
3. Đường dẫn file luôn tính từ `apps/erp-frontend/`, đặt trong dấu `` ` ``.
4. Mục `RUN` ghi **nguyên văn lệnh** và **kết quả định lượng** (vd `vitest: 18 passed, 1 failed (U-GDF-03)`, `tsc: 12 lỗi (baseline 12)`). Nếu thất bại: trích tối đa 10 dòng lỗi quan trọng nhất trong khối code.
5. Mục `DEVIATION` phải có thêm dòng `- **Plan yêu cầu:** …` và `- **Đã làm thay vào đó:** …`.
6. Mục `CHECKPOINT` liệt kê: các bước đã xong, các bước bỏ dở (nếu có), bảng kết quả tsc/lint/vitest/build/playwright so với baseline, danh sách `DEVIATION` và `ISSUE` trong sprint.
7. Không ghi mật khẩu, token hay dữ liệu nhạy cảm vào log.

**Đầu file log** (tạo ở Sprint 0) có sẵn phần mở đầu:

```markdown
# Nhật ký hành động — Gemini — UI/UX revamp erp-frontend
Plan: docs/UI_UX_PLAN_GEMINI.md · Bắt đầu: <ngày giờ> · Nhánh git: <tên nhánh hoặc "không có git">

## Nhật ký
```

**Cuối file log** (viết ở Sprint 9, sau mục cuối cùng) thêm phần `## Tổng kết để rà soát` gồm:
- Bảng tất cả file đã tạo / sửa / chuyển (cột: file, loại, sprint, số mục log liên quan).
- Danh sách `data-testid` đã thêm và đã bỏ (kèm lý do bỏ).
- Toàn bộ `DEVIATION` và `ISSUE` còn mở, mỗi dòng trỏ về số mục log `[#NNN]`.
- Kết quả kiểm tra cuối cùng so với baseline.
- Những chỗ Gemini **không chắc chắn** và muốn người rà soát xem kỹ.

---

## PHẦN 1 — CONTEXT DỰ ÁN (để không làm sai)

### 1.1 Kiến trúc tổng quát

```
erp-platform/
├─ apps/erp-frontend/        ← PHẠM VI LÀM VIỆC
├─ apps/web-public/          (không đụng)
├─ packages/api-contract/    (type sinh từ OpenAPI: src/generated/api.d.ts — chỉ đọc)
└─ services/erp-backend/     (Spring Boot — chỉ đọc để tra cứu)
```

**Frontend stack:** Vite 8, React 19.2, TypeScript 5, Tailwind 4.3.3 (qua `@tailwindcss/postcss`), react-query 5, axios, zustand (chưa dùng), recharts 3, lucide-react, react-hot-toast, vitest 4 + Testing Library + jsdom, Playwright.

**App không dùng router để điều hướng.** Mô hình là **MDI kiểu phần mềm kế toán desktop**:
- `App.tsx`: chưa đăng nhập → `<Login />`. Đã đăng nhập → `<TopRibbon />` + `<TabBar />` + `<MainContent>` hiển thị **component của tab đang active** (tab khác bị unmount).
- `context/TabContext.tsx`: `openTab(id, title, component, isClosable)` — mở tab mới hoặc focus tab đã có cùng `id`.
- `components/layout/TopRibbon.tsx`: tầng 1 là các tab nhóm (`config/menuConfig.ts`: `ChucNang` chỉ STAFF, `ThongKe` chỉ ADMIN…), tầng 2 là các nút mở module.
- Sau đăng nhập: STAFF tự mở tab `new-order` (Bán hàng), ADMIN tự mở tab `dashboard`.

**Module chứng từ (form giao dịch)** = `MdiModuleLayout` + `XxxForm`:
- `components/layout/MdiModuleLayout.tsx` (**bản đang dùng**; bản trùng tên ở `src/layout/` là bản thừa): thanh dọc bên trái 2 tab "Nội dung" / "Danh sách phiếu", thanh nút dưới cùng (Thêm F2, Sửa F3, Lưu F4, Hủy Esc, In F7, Xóa F8, Xác nhận F9, Thoát F12). Phím tắt bắt bằng `window.addEventListener('keydown')`.
- `components/sales/SalesModule.tsx` giữ `formRef` tới `SalesOrderForm` (dùng `forwardRef` + `useImperativeHandle` để lộ `handleAdd/handleEdit/handleSubmit/...`), nhận `onStateChange(mode, isLoading)` để đồng bộ nút.
- `components/sales/SalesOrderForm.tsx` giữ toàn bộ state và render `components/common/document/GenericDocumentForm.tsx` (thuần hiển thị, nhận props).

### 1.2 Auth

- `context/AuthContext.tsx`: `user = { username, role: 'ADMIN' | 'STAFF' }` lưu ở `localStorage.user`; token ở `localStorage.access_token`.
- Khi load trang, AuthContext **giải mã JWT** trong `access_token` và yêu cầu `payload.sub` là **UUID hợp lệ**; nếu không, nó **xóa phiên**. ⇒ Test E2E muốn giả lập đăng nhập phải dùng **JWT giả có `sub` là UUID** (helper ở Sprint 0).
- API login trả `{ accessToken, refreshToken, role, branchUrl }` — **không có tên chi nhánh**. Vì vậy ribbon chỉ hiển thị tên đăng nhập + vai trò.

### 1.3 API và proxy

- `vite.config.ts` proxy: `/api/v1/(auth|branches|analytics|admin|catalog|customers|suppliers)` → HQ `:8080`; các `/api` còn lại (hóa đơn, phiếu nhập, trả hàng, tồn kho, công nợ) → server chi nhánh `:8081`.
- Mọi hàm gọi API tập trung ở `src/api/ApiService.ts`, trả thẳng `res.data.data`.
- Các API liên quan trong đợt này:

| Hàm | Ghi chú quan trọng |
|---|---|
| `Catalog.searchCustomers(q)` | Tìm phía server (`?search=`). Trả `{ id, customerCode, name, phone, address, ... }` |
| `Catalog.searchProducts(q)` | Tìm phía server, kèm giá chi nhánh. Trả `{ id:number, code, name, baseUnit, price }` |
| `Catalog.getSuppliers()` | **Không có tìm kiếm phía server** → phải lọc ở client (theo `code`, `name`, `phone`). |
| `Catalog.createCustomer / createProduct / createSupplier` | Dùng cho "Thêm nhanh" |
| `SalesInvoice.create(payload)` | Backend **tạo và xác nhận luôn** trong một giao dịch. Không đổi logic này. |
| `InboundReceipt.create(payload)` | Payload đúng: `{ supplierId, note, lines: [{ productId:number, quantity, unitCost, unitOfMeasure }] }`. Trả `{ id }`. ⚠️ **Code hiện tại gửi sai** (`unitPrice`, thiếu `unitOfMeasure`) → backend báo lỗi validation. Sprint 4 phải sửa. |
| `InboundReceipt.confirm(id)` / `getAll()` / `getById(id)` | Response: `{ id, receiptCode, status:'DRAFT'|'CONFIRMED', supplierId, note, createdAt, lines:[{productId, quantity, unitCost, unitOfMeasure}] }` — **không có tên sản phẩm, tên NCC** → phải tra từ danh sách sản phẩm/NCC. |
| `GoodsReturn.create(payload)` | Payload: `{ customerId, reason?, note?, invoiceId?, lines:[{ productId:number, quantity, unitPrice, unitOfMeasure }] }`. ⚠️ Trả về **chuỗi UUID** (`data` là string, không phải object). |
| `GoodsReturn.confirm(id)` / `getAll()` / `getById(id)` | Response: `{ id, returnCode, customerId, customerName, status, totalAmount, reason, createdAt, lines:[{productId, productCode, productName, quantity, unitOfMeasure, unitPrice, lineAmount}] }`. ⚠️ `packages/api-contract` **không có** type `GoodsReturnResponseDto` / `GoodsReturnCreateResponseDto` → khai báo type cục bộ trong `src/types/documents.ts` và sửa chữ ký hàm trong `ApiService.GoodsReturn` dùng type đó. |
| `Analytics.getDashboardMetrics(branchId, start, end)` | `start`, `end` là số nguyên dạng `yyyymmdd`. Trả `{ totalRevenue, grossProfit, inventoryTurnoverRatio, totalOverdueDebt, topSellingProducts:[{productId, productName, quantitySold, revenue}] }`. |
| `Analytics.exportExcel(branchId, start, end)` | Trả blob .xlsx. |
| `Branch.getAll()` | Danh sách chi nhánh cho bộ lọc Dashboard. |

### 1.4 Nguyên nhân gốc của lỗi giao diện hiện tại

`src/index.css` đang dùng cú pháp v3 `@tailwind base; @tailwind components; @tailwind utilities;`. Ở v4 hai dòng đầu vô tác dụng ⇒ **không có preflight và không có theme mặc định** ⇒ các class `bg-white`, `p-4`, `text-sm`, `rounded-lg`, `bg-teal-600`, `shadow-lg`, `max-w-7xl`… **không sinh CSS**. Đây là lý do dropdown gợi ý "trong suốt" và bị đè, Login/Dashboard vỡ bố cục. Sprint 1 sửa việc này.

> ⚠️ Khi sửa xong, **preflight được bật** → nút mất viền/nền mặc định của trình duyệt, heading mất cỡ chữ mặc định, `cursor` của `button` thành `default`. Đây là điều mong muốn, nhưng phải rà lại từng màn hình.

### 1.5 Các bẫy đã biết

| Bẫy | Cách xử lý |
|---|---|
| Dropdown nằm trong vùng `overflow-auto` bị cắt | Render dropdown bằng `createPortal` vào `document.body` (Sprint 2). |
| Phím `Esc` trong dropdown đồng thời kích hoạt "Hủy phiếu" của `MdiModuleLayout` | Trong combobox: khi dropdown đang mở, `Esc` gọi `e.preventDefault(); e.stopPropagation()`. Trong `MdiModuleLayout`: bỏ qua sự kiện nếu `e.defaultPrevented`. |
| Tính ngày bằng `toISOString()` bị lệch 1 ngày (máy ở UTC+7, trước 7h sáng) | Dùng helper `toDateKey(date)` tính theo giờ địa phương (Sprint 1). |
| `Intl` định dạng tiền Việt dùng **khoảng trắng không ngắt** (U+00A0) trước `₫` | Trong test so sánh bằng regex hoặc `.replace(/ /g, ' ')`. |
| `toLocaleString()` không truyền locale → ra `250,000` (kiểu Mỹ) | Thay toàn bộ bằng `formatNumber` / `formatCurrency` (locale `vi-VN` → `250.000`). |
| Playwright: route đăng ký **sau** được ưu tiên **trước** | Đăng ký route "bắt tất cả" (fallback) **đầu tiên**, route cụ thể đăng ký sau. |
| AuthContext xóa phiên nếu token không phải JWT có `sub` UUID | Dùng helper `fakeJwt()` trong test E2E. |
| `vitest.config.ts` dùng setup `src/test/setup.ts` (không phải `vitest.setup.ts`) | Thêm mock chung vào `src/test/setup.ts`. |
| Tab không active bị unmount (mất dữ liệu đang nhập khi chuyển tab) | **Ngoài phạm vi đợt này** — không sửa (xem Phụ lục B). |

---

## PHẦN 2 — QUYẾT ĐỊNH ĐÃ CHỐT & DESIGN TOKENS

### 2.1 Bảng quyết định

| Mã | Quyết định |
|---|---|
| A1 | Phong cách lai: màn nghiệp vụ mật độ cao kiểu desktop nhưng dịu mắt; Login/Dashboard hiện đại hơn, cùng bảng màu. |
| A2 | Màu chủ đạo **xanh dương doanh nghiệp `#1D4ED8`**. |
| A3 | Nền app xám rất nhạt `#F5F7FA`; ribbon và vùng nhập trắng; ô nhập trắng viền `#CBD5E1`, focus viền primary. |
| A4 | Số tiền thường màu mực đậm; tổng cần chú ý màu primary; **chỉ nợ/số âm mới đỏ**. |
| A5 | Font `Segoe UI` + dự phòng `Inter, Roboto, Arial` (không dùng Google Fonts). |
| A6 | Mật độ **vừa**: chữ 13px, ô nhập 28px, dòng bảng 28px. |
| A7 | Tối ưu 1366×768 và 1920×1080. |
| A8 | Không làm dark mode. |
| A9 | Sửa cấu hình Tailwind rồi rà từng màn hình. |
| A10 | Chuyển file thừa vào `_to_delete/`, viết lại `DESIGN_SYSTEM.md`. |
| B1 | Đầu phiếu 3 khối tỷ lệ **`1 : 1.5 : 1`**, bỏ min-width cứng. |
| B2 | Nhãn bên trái, căn phải, rộng **96px**, màu `ink-muted`; bắt buộc có `*` đỏ. |
| B3 | Ô chỉ đọc hiển thị dạng chữ, gộp "Nhân viên / Người lập"; số phiếu nổi bật trên thanh tiêu đề. |
| B4 | Cột bảng: STT · Mã hàng · Tên hàng · ĐVT · Số lượng · Đơn giá · Thành tiền · 🗑. Bỏ cột Chiết khấu dòng và Ghi chú dòng. (Cột "Tồn" **hoãn** cùng tính năng tồn kho — R10.) |
| B5 | Dòng chọn: nền primary nhạt + vạch primary 3px bên trái, chữ giữ màu thường. |
| B6 | Ô số có dấu phân cách `1.500.000` cả khi nhập, căn phải, tự bôi đen khi focus. |
| B7 | Luồng bàn phím: chọn hàng → nhảy ô Số lượng → Enter sang Đơn giá → Enter ở dòng cuối tự thêm dòng mới. |
| **B8** | **Phương án B mở rộng: Phiếu nhập hàng chuyển sang `GenericDocumentForm`; mọi form giao dịch (Bán hàng, Nhập hàng, Nhập lại hàng bán) đều dùng chung khung này.** |
| C1 | Dropdown render qua portal, tự lật lên khi sát đáy, định vị lại khi cuộn. |
| C2 | Rộng tối thiểu 560px, tối đa 10 dòng, dòng cao 28px. |
| C3 | Cột sản phẩm: Mã · Tên · ĐVT · Giá bán (không có cột tồn — R10). |
| C4 | Cột khách hàng: Mã KH · Tên KH · Điện thoại · Địa chỉ. |
| C5 | Mở khi focus/click; debounce 250ms; tô đậm chữ khớp; ↑↓ Enter Tab Esc. |
| C6 | Không tìm thấy → nút "+ Thêm … mới" mở popup tạo nhanh (chỉ ADMIN). |
| C7 | Combobox dùng cho mọi phiếu; `SearchModal` giữ làm tìm kiếm nâng cao (nút 🔍 hoặc F5). |
| D1 | Login chia đôi: trái khối thương hiệu màu primary, phải form trên nền trắng. |
| D2 | Tên "ERP System" + khẩu hiệu mặc định, đặt trong `src/config/brand.ts` để chủ dự án tự sửa; logo chữ đơn giản. |
| D3 | Dòng tài khoản test chỉ hiện ở môi trường dev. |
| **D4** | **Chỉ làm:** nút hiện/ẩn mật khẩu; autofocus + Enter + spinner. **Không làm** cảnh báo Caps Lock. |
| E1 | Dashboard: thanh tiêu đề + bộ lọc → 4 KPI → biểu đồ Top sản phẩm + bảng Top sản phẩm. (Không có khối cảnh báo tồn kho — R10.) |
| E2 | Giữ 4 KPI, làm gọn, bỏ icon mờ khổng lồ. |
| E3 | Biểu đồ **cột ngang**, một màu primary, chỉ doanh thu; số lượng ở tooltip và bảng. Không thêm biểu đồ theo thời gian. |
| E4 | Bộ lọc: Hôm nay · 7 ngày qua · Tháng này (mặc định) · Quý này · Năm nay · Toàn thời gian · Tùy chọn. |
| E5 | Dashboard vẫn chỉ cho ADMIN. |
| **F** | **Toàn bộ tính năng cảnh báo tồn kho: HOÃN.** |
| G1–G8 | Làm tất cả (xem Sprint 1 và Sprint 8). |
| H1 | Sửa thẳng vào code (không làm mockup). |
| H2 | Thứ tự: Tailwind/token → Dropdown → GenericForm → form giao dịch → Login → Dashboard → mục G. |
| H3 | Giữ `data-testid`; chạy build + lint + vitest + Playwright (mock) sau mỗi sprint. |

### 2.2 Design tokens (dùng đúng các giá trị này)

**Màu ngữ nghĩa** (khai báo trong `@theme`, Tailwind v4 tự sinh `bg-*`, `text-*`, `border-*`, `ring-*`…):

| Token | Giá trị | Dùng cho |
|---|---|---|
| `--color-primary` | `#1D4ED8` | Nút chính, tab active, focus, tổng tiền cần chú ý |
| `--color-primary-hover` | `#1E40AF` | Hover nút chính |
| `--color-primary-soft` | `#EFF6FF` | Nền dòng chọn, nền icon KPI, hover dropdown |
| `--color-primary-muted` | `#DBEAFE` | Viền nhạt, chip |
| `--color-app` | `#F5F7FA` | Nền toàn app |
| `--color-surface` | `#FFFFFF` | Card, ribbon, vùng form |
| `--color-line` | `#E2E8F0` | Viền bảng, đường chia |
| `--color-line-strong` | `#CBD5E1` | Viền ô nhập, viền nút phụ |
| `--color-ink` | `#0F172A` | Chữ chính, số tiền thường |
| `--color-ink-muted` | `#475569` | Nhãn, chữ phụ |
| `--color-ink-subtle` | `#64748B` | Placeholder, chú thích |
| `--color-danger` / `--color-danger-soft` | `#DC2626` / `#FEF2F2` | Lỗi, nợ, số âm, nút xóa |
| `--color-success` / `--color-success-soft` | `#16A34A` / `#F0FDF4` | Trạng thái "Đã xác nhận" |
| `--color-warning` / `--color-warning-soft` | `#D97706` / `#FFFBEB` | Trạng thái "Nháp" |

**Chữ, kích thước, bo góc:**

| Token | Giá trị |
|---|---|
| `--font-sans` | `'Segoe UI', Inter, Roboto, 'Helvetica Neue', Arial, sans-serif` |
| Cỡ chữ nghiệp vụ | 13px (thân), 12px (nhãn, tiêu đề cột), 15px (tiêu đề phiếu) |
| Cao ô nhập / dòng bảng / nút nghiệp vụ | 28px / 28px / 28px |
| Bo góc nghiệp vụ (ô nhập, nút, bảng) | 4px |
| Bo góc card Dashboard / popup | 8px; card Login 12px |
| Bóng dropdown/popup | `0 10px 30px -8px rgb(15 23 42 / 0.25)` |
| z-index | dropdown `300`, popup/modal `200`, tab bar `10` |

**Remap token cũ `erp-*`** (để code cũ không vỡ trong lúc chuyển đổi):

```
--color-erp-bg-ribbon: #F8FAFC;          --color-erp-bg-ribbon-border: #E2E8F0;
--color-erp-bg-content: #FFFFFF;         --color-erp-bg-input: #FFFFFF;
--color-erp-border-input: #CBD5E1;       --color-erp-border-input-focus: #1D4ED8;
--color-erp-border-input-error: #DC2626; --color-erp-text-primary: #0F172A;
--color-erp-text-accent-red: #DC2626;    --color-erp-bg-table-header: #F1F5F9;
--color-erp-border-table: #E2E8F0;       --color-erp-row-selected-bg: #EFF6FF;
--color-erp-row-selected-text: #0F172A;  --color-erp-row-hover-bg: #F8FAFC;
--color-erp-btn-bg: #FFFFFF;             --color-erp-btn-border: #CBD5E1;
--color-erp-btn-hover-bg: #F1F5F9;       --color-erp-bg-disabled: #F8FAFC;
--color-erp-text-disabled: #94A3B8;      --color-erp-border-disabled: #E2E8F0;
--font-erp: var(--font-sans);
--text-erp-base: 13px; --text-erp-label: 12px; --text-erp-title: 15px;
--spacing-erp-input-height: 28px; --spacing-erp-row-height: 28px;
--spacing-erp-padding-tight: 2px; --radius-erp: 4px;
```

---

## SPRINT 0 — Chuẩn bị, đo baseline, dọn dẹp

**Mục tiêu:** có điểm xuất phát rõ ràng và hạ tầng test dùng chung.

### Các bước

1. **Tạo `docs/GEMINI_ACTION_LOG.md`** với phần mở đầu như mục 0.4. Mục log đầu tiên `[#001]` là `CREATE` chính file này. Từ đây mọi hành động đều ghi log (R11).
   Nếu có git: `git checkout -b feat/ui-ux-revamp` (ghi log `RUN`).
2. Chạy và **ghi lại kết quả baseline** (mỗi lệnh một mục log `RUN`) vào `docs/UI_UX_PROGRESS.md` (tạo mới): số lỗi `npx tsc --noEmit`, số lỗi `npm run lint`, kết quả `npx vitest run`, `npm run build` có pass không. Từ đây về sau **số lỗi tsc/lint không được tăng**.
3. Di chuyển vào `apps/erp-frontend/_to_delete/` (giữ đường dẫn con):
   - `src/App.css` (CSS mẫu của Vite, không file nào import — kiểm tra bằng tìm kiếm `App.css` trước khi chuyển)
   - `src/layout/MdiModuleLayout.tsx` (bản không dùng — kiểm tra không có import nào trỏ tới `src/layout/`)
   - Các script ở gốc `apps/erp-frontend/`: `apply_classic_erp.js`, `apply_classic_erp_grid.js`, `apply_footer.js`, `apply_ux_ui.js`, `apply_ux_ui_safe.js`, `build_ribbon.js`, `compare.js`, `fix_footer.js`, `fix_label.js`, `fix_sidebar.js`, `fix_table.js`, `fix_tests.js`, `rebuild_final_form.js`, `repair.js`, `repair2.js`, `test.js`, `ui_ux_promax.js`
   - Thêm `_to_delete/` vào `.gitignore` của frontend? **Không** — để chủ dự án tự quyết.
4. Cấu hình ESLint/tsc bỏ qua `_to_delete/`: thêm `'_to_delete'` vào `ignores` trong `eslint.config.mjs` và `"_to_delete"` vào `exclude` của `tsconfig.json`.
5. Playwright: trong `playwright.config.ts` thêm `testIgnore: ['**/_legacy/**']`. Tạo thư mục `tests/ui/` cho test mới và `tests/helpers/`.
6. Tạo `tests/helpers/session.ts`:

```ts
import type { Page } from '@playwright/test';

/** JWT giả: AuthContext yêu cầu payload.sub là UUID hợp lệ, nếu không sẽ xóa phiên. */
export function fakeJwt(sub = '11111111-1111-4111-8111-111111111111'): string {
  const b64 = (o: object) => Buffer.from(JSON.stringify(o)).toString('base64url');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({ sub })}.signature`;
}

/** Giả lập đã đăng nhập (gọi TRƯỚC page.goto). */
export async function loginAs(page: Page, role: 'ADMIN' | 'STAFF', username = role === 'ADMIN' ? 'admin' : 'staff_tp1') {
  await page.addInitScript(
    ([user, token]) => {
      localStorage.setItem('user', user);
      localStorage.setItem('access_token', token);
    },
    [JSON.stringify({ username, role }), fakeJwt()] as const,
  );
}
```

7. Tạo `tests/helpers/mockApi.ts`:

```ts
import type { Page, Route } from '@playwright/test';

export const ok = (data: unknown) => ({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data }) });

/** Đăng ký fallback TRƯỚC (Playwright ưu tiên route đăng ký sau). */
export async function mockApi(page: Page, routes: Record<string, (route: Route) => unknown>) {
  await page.route('**/api/v1/**', (route) => route.fulfill(ok([])));
  for (const [pattern, handler] of Object.entries(routes)) {
    await page.route(pattern, handler as (route: Route) => Promise<void>);
  }
}
```

8. Tạo `tests/helpers/fixtures.ts` chứa dữ liệu mock dùng chung:

```ts
export const customers = [
  { id: 'c0a80101-0000-4000-8000-000000000001', customerCode: 'KH001', name: 'Nguyễn Văn A', phone: '0901234567', address: 'Hà Nội' },
  { id: 'c0a80101-0000-4000-8000-000000000002', customerCode: 'KH002', name: 'Trần Thị B', phone: '0987654321', address: 'TP.HCM' },
];
export const products = [
  { id: 101, code: 'SP01', name: 'Sản phẩm 1', baseUnit: 'CAI', price: 50000 },
  { id: 102, code: 'SP02', name: 'Sản phẩm 2', baseUnit: 'HOP', price: 100000 },
];
export const suppliers = [
  { id: 'a0a80101-0000-4000-8000-000000000001', code: 'NCC01', name: 'Nhà cung cấp A', phone: '0281234567', active: true },
];
```

### Checkpoint Sprint 0
- `docs/UI_UX_PROGRESS.md` có số liệu baseline.
- `npm run build` vẫn pass; `npx vitest run` không kém baseline.
- Báo cáo danh sách file đã chuyển vào `_to_delete/`.

---

## SPRINT 1 — Nền tảng: Tailwind, token, tiện ích, khung app (A1–A9, G1–G4, G6, G8)

### Bước 1.1 — Viết lại `src/index.css`

Cấu trúc bắt buộc theo đúng thứ tự:

```css
@import "tailwindcss";

@theme {
  /* 1. Token ngữ nghĩa ở mục 2.2 */
  /* 2. Token erp-* remap ở mục 2.2 */
  /* 3. --font-sans */
}

@layer base {
  html, body, #root { height: 100%; }
  body { font-family: var(--font-sans); font-size: 13px; color: var(--color-ink); background: var(--color-app); -webkit-font-smoothing: antialiased; }
  button:not(:disabled), [role="button"]:not([aria-disabled="true"]), select, summary { cursor: pointer; }
  :focus-visible { outline: 2px solid var(--color-primary); outline-offset: 1px; }
  input:focus-visible, select:focus-visible, textarea:focus-visible { outline: none; }
  /* scrollbar gọn 10px, thumb #CBD5E1, hover #94A3B8, track trong suốt */
  @media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; } }
}

@layer components {
  /* Các class dùng chung — xem Bước 1.2 */
}
```

⚠️ Không đặt `font-size` trên `html` (giữ 1rem = 16px).

### Bước 1.2 — Class dùng chung trong `@layer components` (dùng `@apply`)

| Class | Mô tả |
|---|---|
| `.erp-input` | `h-7 w-full rounded-[4px] border border-line-strong bg-surface px-2 text-[13px] text-ink placeholder:text-ink-subtle outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/15 disabled:bg-slate-50 disabled:text-ink-subtle` |
| `.erp-input-error` | `border-danger bg-danger-soft focus:border-danger focus:ring-danger/15` |
| `.erp-label` | `text-[12px] font-medium text-ink-muted` |
| `.btn` | `inline-flex h-7 items-center justify-center gap-1.5 rounded-[4px] border px-3 text-[13px] font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50` |
| `.btn-primary` | `border-primary bg-primary text-white hover:bg-primary-hover` |
| `.btn-secondary` | `border-line-strong bg-surface text-ink hover:bg-slate-50` |
| `.btn-danger` | `border-danger bg-surface text-danger hover:bg-danger-soft` |
| `.btn-ghost` | `border-transparent bg-transparent text-ink-muted hover:bg-slate-100` |
| `.card` | `rounded-lg border border-line bg-surface` |
| `.erp-table` | Bảng: `w-full border-collapse text-[13px]`; `th`: `h-8 bg-slate-100 px-2 text-left text-[12px] font-semibold text-ink-muted border-b border-line`; `td`: `h-7 px-2 border-b border-line`; `tbody tr:hover`: `bg-slate-50` (viết bằng selector lồng `.erp-table th { @apply ... }`) |
| `.num` | `text-right tabular-nums` |
| `.badge`, `.badge-success`, `.badge-warning`, `.badge-danger`, `.badge-neutral` | Chip trạng thái: `inline-flex h-5 items-center rounded px-1.5 text-[11px] font-semibold` + màu soft/đậm tương ứng |

### Bước 1.3 — Tiện ích định dạng `src/shared/utils/format.ts`

```ts
export const formatNumber = (v: number | null | undefined, maxFractionDigits = 0): string
  // new Intl.NumberFormat('vi-VN', { maximumFractionDigits }).format(v ?? 0)  → '1.500.000'
export const formatCurrency = (v: number | null | undefined): string
  // Intl vi-VN style currency VND → '1.500.000 ₫'
export const parseNumber = (s: string): number
  // bỏ mọi ký tự không phải số, dấu '-' đầu, dấu ',' thập phân → number; chuỗi rỗng → 0
export const formatDate = (v: string | Date | null | undefined): string
  // → 'dd/MM/yyyy' theo giờ địa phương; giá trị rỗng/không hợp lệ → ''
export const toDateKey = (d: Date): number
  // → yyyymmdd theo GIỜ ĐỊA PHƯƠNG (getFullYear/getMonth/getDate), KHÔNG dùng toISOString
export const toInputDate = (d: Date): string
  // → 'yyyy-MM-dd' theo giờ địa phương (dùng cho <input type="date">)
```

### Bước 1.4 — Cấu hình khác

- `src/config/env.ts`: `export const isDev = import.meta.env.DEV;` (để test mock được).
- `src/config/brand.ts`:
  ```ts
  export const BRAND = {
    name: 'ERP System',
    tagline: 'Quản lý bán hàng, kho và công nợ đa chi nhánh',
    mark: 'E',
    copyright: '© 2026 ERP System',
  };
  ```
- `index.html`: `lang="vi"`, `<title>` dùng tên hệ thống.

### Bước 1.5 — `MainContent` và trang không phải MDI (G1)

- `MainContent.tsx`: `<main className="flex-1 min-h-0 overflow-hidden bg-app"><div className="h-full">{children}</div></main>` — **bỏ** `p-6`, `max-w-7xl`, `space-y-6`.
- Tạo `src/shared/components/Page/PageContainer.tsx`: `<div className="h-full overflow-auto"><div className="mx-auto max-w-[1440px] space-y-4 p-5">{children}</div></div>` — dùng cho Dashboard và các trang danh mục (Sprint 7, 8).
- Tạo `src/shared/components/Page/PageHeader.tsx`: props `title`, `subtitle?`, `actions?: ReactNode`. Tiêu đề 18px semibold màu `ink`, subtitle 13px `ink-muted`, actions căn phải, xuống dòng khi hẹp.
- Module MDI (`MdiModuleLayout`) phải chiếm **toàn bộ chiều cao** tab (`h-full`).
- **Tạm thời** bọc các trang danh mục hiện có và Dashboard bằng `PageContainer` và bỏ `p-6` / `min-h-screen` ở thẻ ngoài cùng của chúng (chưa đổi giao diện bên trong — Sprint 7, 8 làm) để chúng không mất thanh cuộn.

### Bước 1.6 — TabBar (G2) trong `App.tsx`

- Nền `bg-surface`, viền dưới `border-line`, cao 34px.
- Tab: `min-w-[140px] max-w-[220px] px-3 text-[13px]`. Tab active: `text-primary font-semibold`, vạch dưới 2px `bg-primary`, nền trắng. Tab thường: `text-ink-muted hover:bg-slate-50`.
- Nút đóng (X 14px): **luôn hiện ở tab active**, tab khác chỉ hiện khi hover. `aria-label="Đóng tab"`.
- Nhấp **chuột giữa** (`onAuxClick`, `e.button === 1`) lên tab có thể đóng → đóng tab.
- Thêm `data-testid={`tab-${tab.id}`}` cho mỗi tab.

### Bước 1.7 — TopRibbon (G3, G4)

- Tầng 1 (nền `bg-slate-50`, viền dưới `border-line`): các tab nhóm bên trái; **bên phải** là cụm người dùng:
  - Icon `UserCircle2` + `username` (semibold) + chip vai trò (`ADMIN` → "Quản trị", `STAFF` → "Nhân viên") — `data-testid="ribbon-user"`.
  - Nút **Đăng xuất** (`.btn-ghost`, icon `LogOut`) — `data-testid="ribbon-logout"`, gọi `logout()`.
- Tab nhóm active: nền trắng, chữ `text-primary`, vạch trên 2px primary. Giữ `data-testid="ribbon-tab-<id>"`.
- Tầng 2 (nền trắng, cao 64px): nút lớn icon 20px trên, nhãn 12px dưới; hover `bg-primary-soft`, bo 4px. Màu icon: tất cả dùng `text-primary`, riêng Đăng xuất (nếu còn) `text-danger`.
- **Bỏ** nút Đăng xuất trong tab "Hệ thống" (đã có ở góc phải).
- **Ẩn** nút "Xuất Trả Hàng Mua" (chưa có chức năng) — xóa khỏi JSX, để lại comment `{/* TODO: Xuất trả hàng mua — chưa có API */}`.
- Nút "Nhập Lại Hàng Bán": thêm `data-testid="ribbon-btn-return"` (component được thay ở Sprint 5).

### Bước 1.8 — Thay định dạng số (G6) ở những chỗ đơn giản

Tìm toàn bộ `toLocaleString(` và `Intl.NumberFormat(` trong `src/` → thay bằng `formatNumber` / `formatCurrency`. Ngày hiển thị → `formatDate`. (Các file sẽ viết lại ở sprint sau thì để sprint đó làm.)

### Test Sprint 1

**Unit — `src/shared/utils/__tests__/format.test.ts`**

| ID | Kiểm tra | Kỳ vọng |
|---|---|---|
| U-FMT-01 | `formatNumber(1500000)` | `'1.500.000'` |
| U-FMT-02 | `formatCurrency(250000).replace(/ /g,' ')` | chứa `'250.000'` và `'₫'` |
| U-FMT-03 | `parseNumber('1.500.000')` | `1500000` |
| U-FMT-04 | `toDateKey(new Date(2026, 8, 27, 1, 0))` | `20260927` |
| U-FMT-05 | `formatDate('2026-09-27')` | `'27/09/2026'` |

**E2E — `tests/ui/shell.spec.ts`** (dùng `loginAs` + `mockApi`)

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-SHELL-01 | STAFF vào `/` | Thấy `ribbon-user` chứa `staff_tp1` và `Nhân viên`; tab `tab-new-order` hiển thị |
| E-SHELL-02 | STAFF, bấm `ribbon-tab-ChucNang` | **Không** thấy chữ "Xuất Trả Hàng Mua"; thấy `ribbon-btn-inbound` và `ribbon-btn-return` |
| E-SHELL-03 | Bấm `ribbon-logout` | Thấy `login-form` |
| E-SHELL-04 | Kiểm tra CSS đã nạp: `page.evaluate` lấy `getComputedStyle(document.body).backgroundColor` | `rgb(245, 247, 250)` |

### Checkpoint Sprint 1
Build/lint/tsc không kém baseline; unit + E2E sprint 1 pass. **Chụp màn hình** (hoặc mô tả) từng màn hình chính sau khi bật preflight, liệt kê chỗ còn vỡ để sprint sau xử lý.

---

## SPRINT 2 — Dropdown tìm kiếm gợi ý (C1–C7)

**File:** viết lại `src/components/common/SearchableCombobox.tsx`; sửa `SearchModal.tsx`; tạo `QuickCreate*`.

### Bước 2.1 — API của component (giữ tương thích ngược)

```ts
export interface ColumnDef {
  header: string;
  field: string;
  width?: string;               // vd '90px' hoặc '40%'
  align?: 'left' | 'right';
  format?: (value: any, row: any) => React.ReactNode;
  highlight?: boolean;          // tô đậm phần chữ khớp query (mặc định true với cột text)
}

export interface SearchableComboboxProps<T> {
  value: string;                              // chữ hiển thị khi đóng
  placeholder?: string;
  disabled?: boolean;
  fetchData: (query: string) => Promise<T[]>;
  columns: ColumnDef[];
  onSelect: (item: T) => void;
  renderEmpty?: (query: string) => React.ReactNode;
  onCreateNew?: (query: string) => void;      // có prop này → hiện nút "+ Thêm mới"
  createLabel?: string;                       // vd 'Thêm khách hàng mới'
  autoFocus?: boolean;
  error?: boolean;
  variant?: 'field' | 'cell';                 // 'cell' = ô trong bảng: không viền, nền trong suốt, cao 100%
  inputRef?: React.Ref<HTMLInputElement>;     // để form focus từ ngoài
  minDropdownWidth?: number;                  // mặc định 560
  'data-testid'?: string;
}
```

Các chỗ đang dùng (`SalesOrderForm`) phải chạy được mà không cần sửa ngoài việc thêm prop mới.

### Bước 2.2 — Hành vi bắt buộc

1. **Mở:** khi focus hoặc click vào ô (nếu không `disabled`). Khi mở, ô nhập hiển thị `query` (bắt đầu rỗng); khi đóng hiển thị `value`.
2. **Tìm:** gọi `fetchData('')` ngay khi mở; khi gõ → **debounce 250ms** rồi gọi `fetchData(query)`. Bỏ qua kết quả của lần gọi cũ nếu đã có lần gọi mới hơn (dùng biến đếm `requestId` trong `useRef`).
3. **Portal:** danh sách render bằng `createPortal(..., document.body)`, `position: fixed`, `z-index: 300`, `data-testid="combobox-dropdown"`, `role="listbox"`.
4. **Định vị:** tính từ `inputEl.getBoundingClientRect()`:
   - `width = max(rect.width, minDropdownWidth)`; nếu tràn phải màn hình thì dịch trái cho vừa (`left = min(rect.left, innerWidth - width - 8)`, tối thiểu 8).
   - Chiều cao danh sách tối đa = header 32px + 10 dòng × 28px = 312px.
   - `spaceBelow = innerHeight - rect.bottom`, `spaceAbove = rect.top`. Nếu `spaceBelow < 312 + 8` **và** `spaceAbove > spaceBelow` → **mở lên trên** (`bottom = innerHeight - rect.top + 4`), ngược lại mở xuống (`top = rect.bottom + 4`). `maxHeight = min(312, space - 12)`.
   - Tính lại khi: mở, kết quả thay đổi, `resize`, `scroll` (listener `window.addEventListener('scroll', fn, true)` — tham số `true` để bắt cuộn của mọi vùng con). Nếu ô nhập đã cuộn ra khỏi màn hình → đóng.
5. **Click ngoài:** `mousedown` trên `document`; chỉ đóng nếu target **không nằm trong** wrapper ô nhập **và không nằm trong** phần tử dropdown (2 ref).
6. **Bàn phím:** `↓/↑` di chuyển dòng highlight (cuộn dòng vào tầm nhìn bằng `scrollIntoView({ block: 'nearest' })`); `Enter` hoặc `Tab` chọn dòng highlight (Tab **không** `preventDefault` để focus vẫn đi tiếp — nhưng với form phiếu, bước B7 sẽ tự chuyển focus); `Esc` đóng và **`e.preventDefault(); e.stopPropagation()`** (tránh kích hoạt "Hủy phiếu"). Khi mở, dòng đầu tiên được highlight sẵn.
7. **Chọn:** gọi `onSelect(item)`, đóng, xóa query.
8. **Hiển thị:** nền `bg-surface`, viền `border-line`, bo 6px, bóng như mục 2.2. Header cột sticky `bg-slate-50 text-[12px] font-semibold text-ink-muted h-8`. Dòng cao 28px, `text-[13px]`; dòng highlight `bg-primary-soft`. Phần chữ khớp query bọc `<mark className="bg-transparent font-semibold text-primary">`. Mỗi dòng có `role="option"` và `aria-selected`.
9. **Trạng thái:** đang tải → dòng "Đang tìm…" với icon `Loader2` quay; lỗi → "Không tải được dữ liệu" màu danger (không crash); rỗng → "Không tìm thấy “{query}”" + nếu có `onCreateNew` thì nút `+ {createLabel}` (`data-testid="combobox-create-new"`).
10. **Ô nhập `variant="field"`:** dùng `.erp-input`, icon `Search` 14px bên phải (khi đóng) hoặc nút `X` xóa query (khi mở và có chữ); lỗi → `.erp-input-error`. **`variant="cell"`:** `h-full w-full border-0 bg-transparent px-2 focus:bg-surface focus:ring-2 focus:ring-inset focus:ring-primary/30`.
11. `aria-expanded`, `aria-controls`, `aria-autocomplete="list"` trên ô nhập.

### Bước 2.3 — `SearchModal` (tìm kiếm nâng cao)

- Sửa debounce: dùng `useRef` giữ timeout, `clearTimeout` trước khi đặt mới (hiện đang tạo nhiều `setTimeout` chồng nhau).
- Giao diện theo token: card bo 8px, header 44px, ô tìm `.erp-input` cao 32px, danh sách dòng hover `bg-primary-soft`. `Esc` đóng (có `stopPropagation`). z-index 200.

### Bước 2.4 — Popup "Thêm nhanh" (C6)

Tạo `src/components/common/quick-create/`:
- `QuickCreateDialog.tsx`: khung popup dùng chung (tiêu đề, nội dung, nút Hủy / Lưu, `Esc` đóng, Enter lưu).
- `QuickCreateCustomer.tsx`: trường Tên (bắt buộc), Điện thoại, Địa chỉ → `ApiService.Catalog.createCustomer({ name, phone, address })` (`CustomerCreateDto` chỉ bắt buộc `name`; không gửi `customerCode` để backend tự sinh).
- `QuickCreateProduct.tsx`: Tên (bắt buộc), Đơn vị tính (mặc định `CAI`) → `createProduct({ code: 'PRD-'+Date.now(), name, categoryId: 1, baseUnit, isActive: true })` (giống `ProductList.handleSave`).
- `QuickCreateSupplier.tsx`: Tên (bắt buộc), Điện thoại, Địa chỉ → `createSupplier({ code: 'NCC-' + Date.now(), name, phone, address })` (`SupplierCreateDto` bắt buộc `code` và `name`).
- Sau khi tạo thành công: `toast.success`, gọi `onCreated(entity)` để form **tự chọn luôn** đối tượng vừa tạo.
- Chỉ truyền `onCreateNew` vào combobox khi `hasRole('ADMIN')` (giống quyền thêm mới ở các trang danh mục).

> ⚠️ ADMIN không có tab "Chức năng" nên thực tế không mở được form giao dịch từ ribbon. Vẫn viết đúng điều kiện quyền; không tự đổi quyền.

### Bước 2.5 — `MdiModuleLayout`

Trong handler `keydown`: dòng đầu `if (e.defaultPrevented) return;`.

### Test Sprint 2

**Unit — `src/components/common/__tests__/SearchableCombobox.test.tsx`** (dùng timer thật, `findBy*` chờ debounce)

| ID | Kiểm tra | Kỳ vọng |
|---|---|---|
| U-CB-01 | Render, click ô nhập | `fetchData` được gọi với `''`; `screen.findByTestId('combobox-dropdown')` tồn tại và **`parentElement === document.body`** |
| U-CB-02 | Gõ `Nguy` | Sau ≤ 1s, `fetchData` được gọi với `'Nguy'`; thấy dòng "Nguyễn Văn A" |
| U-CB-03 | Mở, nhấn `ArrowDown` rồi `Enter` | `onSelect` được gọi với item thứ 2; dropdown đóng |
| U-CB-04 | Mở, nhấn `Escape`; có `window` keydown listener giả | Dropdown đóng; listener của window **không** nhận sự kiện |
| U-CB-05 | `fetchData` trả `[]`, có `onCreateNew` | Thấy `combobox-create-new`; click → `onCreateNew` được gọi với query hiện tại |

**E2E — `tests/ui/combobox.spec.ts`** (STAFF, mock `**/api/v1/customers?search=*` và `**/api/v1/catalog/products?*` trả fixtures)

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-CB-01 | Click `sales-customer-combo`, gõ `Nguy` | `combobox-dropdown` hiển thị; `getComputedStyle(dropdown).backgroundColor` = `rgb(255, 255, 255)` (**chứng minh hết lỗi trong suốt**) |
| E-CB-02 | Chọn "Nguyễn Văn A" | Ô `sales-customer-combo` có value `Nguyễn Văn A` |
| E-CB-03 | Bấm `sales-add-line` 12 lần; click ô hàng hóa ở **dòng cuối** | `combobox-dropdown` hiển thị và `boundingBox` nằm trọn trong viewport (`y >= 0` và `y + height <= viewport.height`) |

### Checkpoint Sprint 2
Báo cáo + ảnh chụp dropdown ở dòng đầu và dòng cuối bảng.

---

## SPRINT 3 — `GenericDocumentForm` dùng chung + Phiếu bán hàng (B1–B7, B8)

**Mục tiêu:** biến `GenericDocumentForm` thành khung cấu hình được, dùng cho mọi phiếu; chuyển `SalesOrderForm` sang API mới **mà không đổi logic nghiệp vụ**.

### Bước 3.1 — Type dùng chung `src/types/documents.ts`

```ts
export type FormMode = 'VIEW' | 'ADD' | 'EDIT';
export type DocStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED';

export interface DocumentLine {
  id: string;                 // id tạm phía client
  productId: string;
  productCode?: string;
  productName: string;
  unitOfMeasure?: string;
  quantity: number;
  unitPrice: number;          // với phiếu nhập: đây là giá nhập (map sang unitCost khi gửi)
}

export interface InfoField   { key: string; label: string; value: React.ReactNode; testId?: string }
export interface PartnerField {
  key: string; label: string; value: string;
  onChange?: (v: string) => void;   // không có → chỉ đọc (hiển thị dạng chữ)
  required?: boolean; placeholder?: string; testId?: string;
}
export interface SummaryField {
  key: string; label: string; value: number;
  onChange?: (v: number) => void;   // có → ô NumberInput
  tone?: 'default' | 'primary' | 'danger';   // A4
  strong?: boolean; testId?: string;
}

/* Type trả trả hàng (api-contract thiếu) */
export interface GoodsReturnLineResponse { id?: string; productId: number; productCode?: string; productName?: string; quantity: number; unitOfMeasure?: string; unitPrice: number; lineAmount?: number }
export interface GoodsReturnResponse { id: string; returnCode?: string; branchId?: number; customerId?: string; customerName?: string; invoiceId?: string; status?: DocStatus; totalAmount?: number; refundAmount?: number; reason?: string; createdAt?: string; lines?: GoodsReturnLineResponse[] }
```

Giữ `export type OrderItem = DocumentLine` và `export type FormMode` trong `GenericDocumentForm.tsx` để không vỡ import cũ.

### Bước 3.2 — `NumberInput` (B6) — `src/shared/components/Form/NumberInput.tsx`

- Props: `value: number`, `onChange(v:number)`, `disabled?`, `className?`, `allowNegative?` (mặc định false), `min?`, `data-testid?`, `onKeyDown?`, `inputRef?`, `variant?: 'field' | 'cell'`.
- `<input type="text" inputMode="numeric">`, căn phải, `tabular-nums`.
- Hiển thị `formatNumber(value)`; khi gõ: `parseNumber(e.target.value)` → `onChange`. Giữ vị trí con trỏ ở cuối là đủ (không cần xử lý phức tạp).
- `onFocus` → `e.target.select()`.

> ⚠️ Test cũ dùng `input[type="number"]` sẽ hỏng — test mới dùng `data-testid`.

### Bước 3.3 — API mới của `GenericDocumentForm`

```ts
export interface GenericDocumentFormProps {
  mode: FormMode;
  docTitle: string;                    // 'Phiếu bán hàng' | 'Phiếu nhập hàng' | 'Phiếu nhập lại hàng bán'
  docCode: string;                     // 'AUTO-GENERATE' → hiển thị '(Tự động)'
  status?: DocStatus;                  // hiển thị badge
  error?: string | null;
  errors?: Record<string, string>;     // key: 'partner', `line_${index}_product`, `line_${index}_quantity`

  info: InfoField[];                   // khối trái (chỉ đọc) — Ngày được xử lý riêng:
  createdDate: string; onCreatedDateChange?: (v: string) => void;

  partner: {
    label: string;                     // 'Khách hàng' | 'Nhà cung cấp'
    required?: boolean;
    renderCombobox: (hasError: boolean) => React.ReactNode;   // dùng khi ADD/EDIT
    displayName: string;               // dùng khi VIEW
    onAdvancedSearch?: () => void;     // nút 🔍 mở SearchModal
    fields: PartnerField[];
  };

  summary: SummaryField[];             // khối phải

  lines: {
    testIdPrefix: 'sales' | 'inbound' | 'return';
    items: DocumentLine[];
    priceLabel?: string;               // mặc định 'Đơn giá'; phiếu nhập: 'Giá nhập'
    onAdd: () => void;
    onRemove: (id: string) => void;
    onUpdate: (id: string, field: keyof DocumentLine, value: unknown) => void;
    renderProductCombobox: (line: DocumentLine, index: number, hasError: boolean) => React.ReactNode;
  };
}
```

### Bước 3.4 — Bố cục (B1, B2, B3)

```
┌ Thanh tiêu đề (40px, bg-surface, border-b border-line) ─────────────────────────┐
│ [Phiếu bán hàng]  [badge trạng thái]                     Số: HD000123 (primary) │
├ Header 3 khối: grid-cols-[1fr_1.5fr_1fr] gap-x-6 p-3 bg-surface border-b ───────┤
│ Khối trái (Thông tin)   │ Khối giữa (Đối tác)            │ Khối phải (Tổng tiền) │
├ Bảng hàng hóa (flex-1, overflow-auto) ──────────────────────────────────────────┤
│ thead sticky · tbody · tfoot sticky (TỔNG CỘNG)                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

- Thanh tiêu đề: `docTitle` 15px semibold; badge `DRAFT` → `.badge-warning` "Nháp", `CONFIRMED` → `.badge-success` "Đã xác nhận", `CANCELLED` → `.badge-danger` "Đã hủy"; không truyền status → không hiện. Bên phải: `Số:` + mã 15px semibold `text-primary`, `data-testid="doc-code"`; mã `AUTO-GENERATE` hiển thị `(Tự động)` màu `ink-subtle`.
- Mỗi khối là lưới `grid-cols-[96px_minmax(0,1fr)] gap-x-2 gap-y-1.5 items-center`. Nhãn `.erp-label text-right`; bắt buộc thêm `<span className="text-danger">*</span>`.
- **Khối trái:** dòng "Ngày" là `<input type="date" className="erp-input">` (khi VIEW hoặc không có `onCreatedDateChange` → chữ `formatDate`); các `info` còn lại hiển thị **dạng chữ** `text-[13px] text-ink h-7 flex items-center truncate` (không viền).
- **Khối giữa:** dòng đầu là đối tác: ADD/EDIT → `renderCombobox` + nút 🔍 (`.btn-secondary w-7 px-0`, `aria-label="Tìm kiếm nâng cao"`, chỉ hiện nếu có `onAdvancedSearch`); VIEW → chữ đậm `displayName`. Các `fields` sau: có `onChange` và không VIEW → `.erp-input`; ngược lại → chữ.
- **Khối phải:** mỗi `summary`: có `onChange` và không VIEW → `NumberInput`; ngược lại → chữ `num`. Màu theo `tone`: `default` → `text-ink`, `primary` → `text-primary font-semibold`, `danger` → `text-danger font-semibold` **chỉ khi value > 0**, bằng 0 thì `text-ink`. `strong` → `text-[15px] font-semibold`.
- Header không còn `overflow-x-auto` (nguyên nhân cắt dropdown). Toàn form: `flex h-full flex-col bg-surface`; dưới 1100px thì vùng form cuộn ngang (`min-w-[1100px]` đặt ở **một** wrapper bên trong vùng cuộn duy nhất).

### Bước 3.5 — Bảng hàng hóa (B4, B5)

- Dùng **một `<table>` duy nhất** với `<colgroup>` để thead/tbody/tfoot thẳng cột (bỏ bảng tổng cộng tách rời hiện tại).

| Cột | Rộng | Nội dung |
|---|---|---|
| STT | 44px | số thứ tự, căn giữa, `ink-subtle` |
| Mã hàng | 110px | `productCode` (chữ) |
| Tên hàng | tự giãn | ADD/EDIT: `renderProductCombobox(line, i, hasError)` với `variant="cell"`; VIEW: `productName` |
| ĐVT | 70px | `unitOfMeasure` căn giữa |
| Số lượng | 96px | `NumberInput variant="cell"`, `data-testid="{prefix}-line-quantity"` |
| Đơn giá / Giá nhập | 128px | `NumberInput variant="cell"`, `data-testid="{prefix}-line-price"` |
| Thành tiền | 140px | `formatNumber(qty*price)`, `num font-medium`, `data-testid="{prefix}-line-total"` |
| (xóa) | 36px | nút `Trash2` 14px `text-ink-subtle hover:text-danger`, `aria-label="Xóa dòng"`, `data-testid="{prefix}-line-delete"`; ẩn khi VIEW |

- `tr`: `data-testid="{prefix}-line-row"`, `h-7 border-b border-line`; hover `bg-slate-50`; dòng chọn: `bg-primary-soft shadow-[inset_3px_0_0_var(--color-primary)]`, **chữ giữ màu thường**.
- Ô có lỗi: `ring-1 ring-inset ring-danger bg-danger-soft`.
- Dòng cuối tbody (khi không VIEW): nút `+ Dòng mới` (`.btn-ghost text-primary`), `data-testid="{prefix}-add-line"`. Không có dòng nào → một dòng mô tả "Chưa có hàng hóa. Bấm “Dòng mới” hoặc nhấn Enter để thêm." màu `ink-subtle`.
- `thead` sticky top, `bg-slate-100`, chữ 12px semibold `ink-muted`.
- `tfoot` sticky bottom, `bg-slate-50 border-t-2 border-line-strong`, cao 32px: "TỔNG CỘNG" (colspan tới hết cột ĐVT), tổng số lượng (`num`), ô trống, tổng thành tiền (`num text-primary font-semibold text-[14px]`, `data-testid="{prefix}-grand-total"`), ô trống.

### Bước 3.6 — Luồng bàn phím (B7)

- Mỗi ô nhập trong bảng gắn `data-line-id={line.id}` và `data-field="product" | "quantity" | "price"`.
- Tạo helper `focusCell(lineId, field)` = `document.querySelector<HTMLInputElement>(`[data-line-id="${lineId}"][data-field="${field}"]`)?.focus()` (gọi trong `requestAnimationFrame`).
- Chọn sản phẩm xong (form cha gọi trong `onSelect`) → `focusCell(line.id, 'quantity')`.
- `Enter` ở Số lượng → `focusCell(id, 'price')`.
- `Enter` ở Đơn giá: nếu là dòng cuối → `onAdd()` rồi focus ô `product` của dòng mới (dùng `useEffect` theo dõi số dòng tăng); nếu không → focus `product` của dòng kế tiếp.
- Không chặn các phím tắt F2–F12 của `MdiModuleLayout`.

### Bước 3.7 — `MdiModuleLayout` (giao diện + ẩn nút không có handler)

- Thanh dọc trái: rộng 32px, nền `bg-slate-50`, tab active nền trắng + vạch trái 2px primary, chữ `text-primary font-semibold`.
- Thanh nút dưới: cao 40px, `bg-surface border-t border-line`, nút dùng `.btn .btn-secondary`; **Lưu** dùng `.btn-primary`; **Xóa** dùng `.btn-danger`; phím tắt hiển thị dạng `<kbd>` nhỏ `text-[11px] text-ink-subtle`.
- **Chỉ render nút khi có handler tương ứng** (`onAdd`, `onEdit`, `onDelete`, `onConfirm`, `onPrint`, `onSave`, `onCancel`, `onExit`) — phím tắt cũng bỏ qua khi handler không có.
- Thêm prop `hideConfirm?: boolean` (ẩn nút Xác nhận khi phiếu đã CONFIRMED).
- Giữ nguyên toàn bộ `data-testid` (`subview-form`, `subview-list`, `btn-add`, `btn-edit`, `btn-save`, `btn-cancel`, `btn-delete`, `btn-confirm`, `btn-print`, `btn-exit`).

### Bước 3.8 — Chuyển `SalesOrderForm` sang API mới

- **Không đổi**: state, `handleSubmit`, payload, `normalizeInitialData`, `useSalesInvoice`, ConfirmDialog, thông báo toast, công thức `totalAmount / finalAmount / invoiceRemaining / remainingBalance`.
- Đổi key lỗi dòng `item_${index}_*` → `line_${index}_*`.
- `info`: `Kho xuất` = branch; `Lấy giá` = "Bán hàng theo khách"; `Nhân viên` = creator (**gộp** Nhân viên/Người lập thành một dòng, nhãn "Nhân viên").
- `partner`: label "Khách hàng", required; combobox `data-testid="sales-customer-combo"`, placeholder "Nhập mã, tên hoặc SĐT khách hàng…", cột C4 (Mã KH 90px · Tên KH · Điện thoại 110px · Địa chỉ 30%); `onAdvancedSearch` → mở `SearchModal` khách hàng đã có; `onCreateNew` (ADMIN) → `QuickCreateCustomer`. `fields`: Người liên hệ (contactPerson), Điện thoại, Địa chỉ, Ghi chú — đều sửa được.
- `summary` (A4): Nợ trước (`oldDebt`, sửa được như cũ), Tiền hàng (`totalAmount`), Chiết khấu (`discount`, sửa được), VAT (`tax`, sửa được), Trả trước (`advancePayment`, sửa được), **Còn của đơn** (`invoiceRemaining`, tone primary, strong), **Nợ tổng mới** (`remainingBalance`, tone danger, strong). testId: `sum-old-debt`, `sum-total`, `sum-discount`, `sum-tax`, `sum-advance`, `sum-invoice-remaining`, `sum-new-debt`.
- Combobox sản phẩm: `data-testid="sales-product-combo-{lineId}"`, **giữ placeholder "Nhấn để chọn..."** (test cũ dùng), cột C3 (Mã 90px · Tên · ĐVT 70px · Giá bán 120px phải, `formatCurrency`); `onSelect` cập nhật `productId`, `productCode`, `productName`, `unitPrice`, `unitOfMeasure` rồi `focusCell(line.id,'quantity')`; `onCreateNew` (ADMIN) → `QuickCreateProduct`.
- `docTitle="Phiếu bán hàng"`, `status={invoiceStatus}` chỉ khi đã có `currentInvoiceId`.
- Modal "Preview In Phiếu": đổi class sang token (card bo 8px, header `bg-slate-50`), không đổi nội dung `PrintInvoice`.

### Test Sprint 3

**Unit**

| File | ID | Kiểm tra | Kỳ vọng |
|---|---|---|---|
| `shared/components/Form/__tests__/NumberInput.test.tsx` | U-NUM-01 | Render `value=0`, gõ `1500000` (component được điều khiển bởi state cha trong test) | Ô hiển thị `1.500.000`; `onChange` nhận `1500000` |
| `components/common/document/__tests__/GenericDocumentForm.test.tsx` | U-GDF-01 | Render mode ADD, partner label "Khách hàng", 2 dòng, prefix `sales` | Thấy nhãn "Khách hàng"; có 2 phần tử `sales-line-row`; `sales-grand-total` = tổng đúng dạng `x.xxx` |
| | U-GDF-02 | Click `sales-add-line` | `lines.onAdd` được gọi 1 lần |
| | U-GDF-03 | Render mode VIEW | Không có `sales-add-line`, không có `sales-line-delete` |
| | U-GDF-04 | `docCode='AUTO-GENERATE'` | `doc-code` chứa "(Tự động)" |
| `components/layout/__tests__/MdiModuleLayout.test.tsx` | U-MDI-01 | Không truyền `onDelete` | Không có `btn-delete` |
| | U-MDI-02 | mode ADD, nhấn phím `F4` | `onSave` được gọi |

**E2E — `tests/ui/sales.spec.ts`** (STAFF; mock customers, products, `**/api/v1/receivable-debts/*/balance` → `ok(0)`, và `POST **/api/v1/sales-invoices` trả `{ id: 'inv-1', invoiceCode: 'HD0001' }`)

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-SALE-01 | Mở `/` | Thấy `doc-code` chứa "(Tự động)", nút `btn-save` |
| E-SALE-02 | Chọn khách "Nguyễn Văn A"; `sales-add-line`; ở ô hàng hóa gõ `SP01`, chọn "Sản phẩm 1" | Ô `sales-line-quantity` đang được focus (`toBeFocused`) |
| E-SALE-03 | Nhập số lượng `5` | `sales-line-total` chứa `250.000`; `sales-grand-total` chứa `250.000` |
| E-SALE-04 | Bấm `btn-save`, bắt request POST | Payload có `customerId` của KH001, `lines[0]` = `{ productId: 101, quantity: 5, unitPrice: 50000, unitOfMeasure: 'CAI' }`; sau đó `doc-code` chứa `HD0001` |
| E-SALE-05 | Form trống bấm `btn-save` | Thấy toast "Đơn hàng phải có ít nhất 1 sản phẩm" |

### Checkpoint Sprint 3
Ảnh chụp phiếu bán hàng ở 1366×768 và 1920×1080 (ADD và VIEW).

---

## SPRINT 4 — Phiếu nhập hàng dùng `GenericDocumentForm` (B8)

**Mục tiêu:** `InboundReceiptModule` có cùng khung với bán hàng: `MdiModuleLayout` + `InboundReceiptForm` + `InboundReceiptList`. **Sửa luôn payload sai.**

### Các bước

1. Tạo `src/components/inventory/InboundReceiptForm.tsx` theo mẫu `SalesOrderForm` (`forwardRef` + `useImperativeHandle` lộ `handleAdd, handleSubmit, handleCancel, handleConfirm, handleExit`; **không** lộ `handleEdit`, `handleDelete`, `handlePrint` vì backend không có API sửa/xóa/in phiếu nhập).
2. State: `supplierId`, `supplierName`, `supplierPhone`, `supplierAddress`, `note`, `createdDate`, `lines: DocumentLine[]`, `receiptId`, `receiptCode`, `status`, `mode`, `isLoading`, `fieldErrors`.
3. Danh sách NCC: `useQuery({ queryKey: ['suppliers'], queryFn: ApiService.Catalog.getSuppliers })`; `fetchData` của combobox **lọc client** theo `code`, `name`, `phone` (không phân biệt hoa thường, bỏ dấu tiếng Việt bằng `normalize('NFD').replace(/[̀-ͯ]/g,'')`). Chỉ lấy NCC `active !== false`.
4. Combobox NCC: `data-testid="inbound-supplier-combo"`, cột Mã NCC 90px · Tên NCC · Điện thoại 110px; `onCreateNew` (ADMIN) → `QuickCreateSupplier`; `onAdvancedSearch` → `SearchModal` NCC (dùng lại danh sách đã lọc).
5. Combobox sản phẩm: giống bán hàng (`ApiService.Catalog.searchProducts`), `data-testid="inbound-product-combo-{lineId}"`; khi chọn: `unitPrice` = `product.price ?? 0` (giá tham khảo, người dùng sửa thành giá nhập).
6. Cấu hình `GenericDocumentForm`:
   - `docTitle="Phiếu nhập hàng"`, `status` (khi đã có `receiptId`), `docCode = receiptCode || 'AUTO-GENERATE'`.
   - `info`: Kho nhập (chi nhánh — dùng cùng nguồn giá trị như `SalesOrderForm` đang dùng cho `branch`), Người lập (`user.username`).
   - `partner`: label "Nhà cung cấp", required; `fields`: Điện thoại (chỉ đọc), Địa chỉ (chỉ đọc), Ghi chú (sửa được, `testId: 'inbound-note'`).
   - `summary`: Tổng số lượng (`sum-total-qty`), **Tổng tiền hàng** (tone primary, strong, `sum-total`).
   - `lines.testIdPrefix='inbound'`, `priceLabel='Giá nhập'`.
7. Validate khi Lưu: chưa chọn NCC → `errors.partner`; không có dòng → toast "Phiếu nhập phải có ít nhất 1 sản phẩm."; dòng thiếu sản phẩm / số lượng ≤ 0 → lỗi dòng + toast "Vui lòng kiểm tra lại thông tin nhập.".
8. **Payload đúng** (khác hẳn code cũ):
   ```ts
   { supplierId, note, lines: lines.map(l => ({
       productId: Number(l.productId), quantity: l.quantity,
       unitCost: l.unitPrice, unitOfMeasure: l.unitOfMeasure || 'CAI' })) }
   ```
   Thành công → lưu `receiptId = res.id`, `status='DRAFT'`, mode VIEW, toast **"Lưu phiếu nhập thành công"**. Sau đó gọi `getById(res.id)` để lấy `receiptCode` (nếu lỗi thì bỏ qua, không báo lỗi).
9. Xác nhận (F9): chỉ khi `receiptId` và `status==='DRAFT'` → `InboundReceipt.confirm` → `status='CONFIRMED'`, toast **"Xác nhận phiếu nhập thành công"**. Phiếu CONFIRMED → `hideConfirm`.
10. Hủy/Thoát: dùng `ConfirmDialog` như `SalesOrderForm` (**không** dùng `window.confirm/alert`). Hủy khi đang ADD → đóng tab; Thêm (F2) ở VIEW → reset form sang ADD.
11. `src/components/inventory/InboundReceiptList.tsx`: `useQuery(['inbound-receipts'], InboundReceipt.getAll)`; bảng `.erp-table`: Số phiếu · Ngày tạo (`formatDate`) · Nhà cung cấp (tra tên từ danh sách NCC theo `supplierId`) · Trạng thái (badge) · Ghi chú. Double-click dòng → `getById` → map `lines` sang `DocumentLine` (tên/mã sản phẩm tra từ `useQuery(['products'], Catalog.getProducts)`; `unitPrice = unitCost`) → mở form ở VIEW. `data-testid="inbound-list-row"`.
12. Viết lại `InboundReceiptModule.tsx` theo mẫu `SalesModule` (giữ export tên `InboundReceiptModule` và prop `mode` để `TopRibbon` không phải sửa).
13. `hooks/useInboundReceipt.ts`: nếu không còn nơi nào dùng → chuyển vào `_to_delete/`.
14. Test Playwright cũ `tests/inbound-receipt.spec.ts` dùng testid đã bỏ (`inbound-save-draft`, `inbound-confirm`, `inbound-supplier-search`, `inbound-add-product`) → **chuyển vào `tests/_legacy/`**, test mới thay thế ở dưới.

### Test Sprint 4

**E2E — `tests/ui/inbound.spec.ts`** (STAFF; mock `**/api/v1/suppliers*`, `**/api/v1/catalog/products*`, `POST **/api/v1/inventory/inbound` → `{ id: 'rcpt-1' }`, `GET **/api/v1/inventory/inbound/rcpt-1` → `{ id:'rcpt-1', receiptCode:'PN0001', status:'DRAFT', lines: [] }`, `POST **/confirm` → `null`)

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-INB-01 | `ribbon-tab-ChucNang` → `ribbon-btn-inbound` | Thấy tiêu đề "Phiếu nhập hàng" và `inbound-supplier-combo` |
| E-INB-02 | Chọn "Nhà cung cấp A"; `inbound-add-line`; chọn "Sản phẩm 1"; số lượng `5`; giá nhập `40000` | `inbound-line-total` chứa `200.000` |
| E-INB-03 | Bấm `btn-save`, bắt request POST | Payload `lines[0]` = `{ productId: 101, quantity: 5, unitCost: 40000, unitOfMeasure: 'CAI' }`; **không** có khóa `unitPrice`; thấy toast "Lưu phiếu nhập thành công"; `doc-code` chứa `PN0001`; thấy badge "Nháp" |
| E-INB-04 | Bấm `btn-confirm` | Toast "Xác nhận phiếu nhập thành công"; badge "Đã xác nhận"; nút `btn-confirm` biến mất |
| E-INB-05 | Form trống bấm `btn-save` | Toast "Phiếu nhập phải có ít nhất 1 sản phẩm." |

---

## SPRINT 5 — Phiếu nhập lại hàng bán dùng cùng khung (B8)

**Mục tiêu:** thay trang giữ chỗ "Module đang được phát triển…" bằng module thật, cùng khung với 2 phiếu trên.

### Các bước

1. Sửa `ApiService.GoodsReturn`: `getAll(): Promise<GoodsReturnResponse[]>`, `getById(id): Promise<GoodsReturnResponse>`, `create(payload): Promise<string>` (**data là UUID string**), `confirm(id): Promise<void>` — dùng type ở `src/types/documents.ts`. Payload type cục bộ:
   ```ts
   export interface GoodsReturnCreatePayload { customerId: string; reason?: string; note?: string; invoiceId?: string;
     lines: { productId: number; quantity: number; unitPrice: number; unitOfMeasure: string }[] }
   ```
2. Tạo `GoodsReturnForm.tsx`, `GoodsReturnList.tsx`, viết lại `GoodsReturnModule.tsx` — **y hệt cấu trúc Sprint 4**, khác ở:
   - `docTitle="Phiếu nhập lại hàng bán"`, `testIdPrefix='return'`, `priceLabel='Đơn giá'`.
   - Đối tác: "Khách hàng" (`data-testid="return-customer-combo"`, dùng `searchCustomers`, cột C4); `fields`: Điện thoại (chỉ đọc), Địa chỉ (chỉ đọc), **Lý do trả** (sửa được, `testId: 'return-reason'`), Ghi chú (`return-note`).
   - `info`: Kho nhập, Người lập.
   - `summary`: Tổng số lượng, **Tổng tiền trả** (tone primary, strong, `sum-total`).
   - Payload: `{ customerId, reason, note, lines: [{ productId:Number, quantity, unitPrice, unitOfMeasure }] }`.
   - Toast: **"Lưu phiếu trả thành công"**, **"Xác nhận trả hàng thành công"**; lỗi "Phiếu trả phải có ít nhất 1 sản phẩm.".
   - Sau lưu: `returnId = res` (string); gọi `getById` lấy `returnCode`.
   - List: Số phiếu · Ngày tạo · Khách hàng (`customerName`) · Tổng tiền (`formatNumber(totalAmount)`) · Trạng thái · Lý do; `data-testid="return-list-row"`; `lines` đã có `productName/productCode` sẵn.
3. `TopRibbon`: nút "Nhập Lại Hàng Bán" mở `openTab('goods-return', 'NHẬP LẠI HÀNG BÁN', <GoodsReturnModule mode="ADD" />)`.
4. `tests/goods-return.spec.ts` cũ dùng testid không tồn tại (`return-customer-search`, `return-add-product`, `return-save-draft`) → chuyển vào `tests/_legacy/`.

### Test Sprint 5

**E2E — `tests/ui/goods-return.spec.ts`** (STAFF; mock customers, products, `POST **/api/v1/goods-returns` → `ok('ret-1')`, `GET **/api/v1/goods-returns/ret-1` → `{ id:'ret-1', returnCode:'TH0001', status:'DRAFT', lines: [] }`, `POST **/confirm` → `null`)

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-RET-01 | `ribbon-tab-ChucNang` → `ribbon-btn-return` | Thấy "Phiếu nhập lại hàng bán" và `return-customer-combo` |
| E-RET-02 | Chọn KH "Trần Thị B"; thêm dòng; chọn "Sản phẩm 2"; số lượng `2`; gõ lý do "Hàng lỗi" vào `return-reason` | `return-grand-total` chứa `200.000` |
| E-RET-03 | Bấm `btn-save`, bắt request | Payload có `customerId` của KH002, `reason: 'Hàng lỗi'`, `lines[0]` = `{ productId: 102, quantity: 2, unitPrice: 100000, unitOfMeasure: 'HOP' }`; toast "Lưu phiếu trả thành công"; `doc-code` chứa `TH0001` |
| E-RET-04 | Bấm `btn-confirm` | Toast "Xác nhận trả hàng thành công"; badge "Đã xác nhận" |

### Checkpoint Sprint 4 + 5
Ảnh chụp 3 phiếu (bán, nhập, trả) cạnh nhau để chứng minh cùng một khung.

---

## SPRINT 6 — Màn hình đăng nhập (D1–D4)

**File:** `src/components/auth/Login.tsx`. **Không đổi** logic `handleSubmit` / `login()`.

### Bố cục

```
┌──────────────── 55% (ẩn dưới 1024px) ────────────┬────────── 45% ──────────┐
│ nền bg-primary, gradient nhẹ xuống #1E3A8A        │ nền trắng, căn giữa      │
│ [E] ERP System                                    │   Đăng nhập              │
│ Quản lý bán hàng, kho và công nợ đa chi nhánh    │   Chào mừng trở lại…     │
│  🛒 Bán hàng nhanh, in hóa đơn                    │   Tên đăng nhập [……]      │
│  📦 Nhập kho & theo dõi tồn                       │   Mật khẩu     [……] 👁    │
│  💳 Công nợ khách hàng                            │   [   Đăng nhập   ]      │
│  📊 Báo cáo doanh thu                             │   (dev) tài khoản test   │
│ © 2026 ERP System                                 │                         │
└───────────────────────────────────────────────────┴─────────────────────────┘
```

### Các bước

1. Khung: `grid min-h-screen lg:grid-cols-[55%_45%] bg-surface`.
2. Panel trái (`hidden lg:flex flex-col justify-between p-12 text-white`), nền `bg-primary` với `bg-gradient-to-b from-primary to-[#1E3A8A]` (cùng tông xanh — **không** tím/hồng, **không** đốm blur). Logo: ô 40×40 bo 8px nền trắng, chữ `BRAND.mark` màu primary đậm; tên `BRAND.name` 22px semibold; tagline 15px `text-white/80`; 4 dòng tính năng dùng icon lucide (`ShoppingCart`, `PackageCheck`, `Wallet`, `BarChart3`) 18px trong ô tròn `bg-white/10`, chữ 14px `text-white/90`; chân trang `BRAND.copyright` 12px `text-white/60`.
3. Panel phải: `flex items-center justify-center p-6`; form `w-full max-w-[380px]`. Màn hình < 1024px: hiện logo + tên ở đầu form.
4. Tiêu đề "Đăng nhập" 24px semibold `text-ink`; phụ đề "Nhập tài khoản được cấp để tiếp tục" 14px `ink-muted`.
5. Trường nhập (kích thước riêng cho login, lớn hơn mật độ nghiệp vụ): nhãn trên 13px medium; ô cao 40px, bo 6px, viền `line-strong`, focus viền primary + `ring-4 ring-primary/10`, icon trái `User` / `Lock` 18px `ink-subtle`, chữ 14px.
6. **Tên đăng nhập:** `autoFocus`, `autoComplete="username"`, giữ `data-testid="login-username"`, placeholder "Tên đăng nhập".
7. **Mật khẩu:** `autoComplete="current-password"`, giữ `data-testid="login-password"`; nút mắt bên phải (`Eye`/`EyeOff`), `type="button"`, `aria-label` "Hiện mật khẩu"/"Ẩn mật khẩu", `data-testid="login-toggle-password"`, bấm đổi `type` giữa `password` ↔ `text`.
8. **Lỗi:** khối `bg-danger-soft border border-danger/30 text-danger text-[13px] rounded-md p-3` + icon `AlertCircle`, giữ `data-testid="login-error"`, `role="alert"`.
9. **Nút:** `type="submit"`, rộng 100%, cao 40px, `bg-primary hover:bg-primary-hover text-white font-semibold rounded-md`; khi loading: `disabled`, icon `Loader2 animate-spin` + chữ "Đang đăng nhập…"; giữ `data-testid="login-submit"`. Enter trong form tự submit (form HTML chuẩn).
10. **Tài khoản test (D3):** chỉ khi `isDev` (từ `src/config/env.ts`): khối `bg-slate-50 border border-line rounded-md p-3 text-[12px] text-ink-muted`, `data-testid="login-dev-hint"`, nội dung giữ nguyên thông tin tài khoản test hiện có.
11. **Không** làm cảnh báo Caps Lock (D4).
12. Giữ `data-testid="login-form"` trên thẻ `<form>`.

### Test Sprint 6

**Unit — `src/components/auth/__tests__/Login.test.tsx`** (mock `useAuth` trả `{ login: vi.fn() }`, mock `../../../config/env`)

| ID | Kiểm tra | Kỳ vọng |
|---|---|---|
| U-LOGIN-01 | Bấm `login-toggle-password` | `login-password` có `type="text"`; bấm lần nữa → `type="password"` |
| U-LOGIN-02 | `isDev = true` | Có `login-dev-hint` |
| U-LOGIN-03 | `isDev = false` | Không có `login-dev-hint` |
| U-LOGIN-04 | Nhập user/pass, submit | `login` được gọi với đúng 2 giá trị |

**E2E — `tests/ui/login.spec.ts`**

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-LOGIN-01 | Mở `/` (chưa đăng nhập) | Thấy `login-form`; `login-username` được focus |
| E-LOGIN-02 | Mock `POST **/api/v1/auth/login` → `ok({ accessToken: fakeJwt(), refreshToken: 'r', role: 'ADMIN' })`; nhập admin/password; nhấn **Enter** | Thấy `ribbon-user` chứa "admin" |
| E-LOGIN-03 | Mock login trả `401` `{ success:false, message:'Sai thông tin đăng nhập' }` | Thấy `login-error`; `login-submit` enabled trở lại |

---

## SPRINT 7 — Dashboard (E1–E5)

**File:** `src/components/sales/Dashboard.tsx`; tạo `src/components/sales/dashboardPeriod.ts`.

### Bước 7.1 — Hàm khoảng thời gian (`dashboardPeriod.ts`)

```ts
export type PeriodKey = 'today' | '7d' | 'month' | 'quarter' | 'year' | 'all' | 'custom';
export const PERIOD_OPTIONS: { key: PeriodKey; label: string }[] = [
  { key: 'today', label: 'Hôm nay' }, { key: '7d', label: '7 ngày qua' },
  { key: 'month', label: 'Tháng này' }, { key: 'quarter', label: 'Quý này' },
  { key: 'year', label: 'Năm nay' }, { key: 'all', label: 'Toàn thời gian' },
  { key: 'custom', label: 'Tùy chọn…' },
];
export function getPeriodRange(key: PeriodKey, now = new Date(), custom?: { start: string; end: string }): { start: number; end: number }
```

- Dùng `toDateKey` (giờ địa phương). `today`: hôm nay→hôm nay. `7d`: hôm nay − 6 ngày → hôm nay. `month`: ngày 1 tháng này → hôm nay. `quarter`: ngày 1 của tháng đầu quý → hôm nay. `year`: 01/01 → hôm nay. `all`: `20200101` → `20301231` (giữ như code cũ). `custom`: chuyển `yyyy-MM-dd` → số.
- Mặc định: **`month`**.

### Bước 7.2 — Bố cục (trong `PageContainer`, `data-testid="dashboard-page"`)

1. **Thanh tiêu đề** (`PageHeader`): title "Tổng quan kinh doanh", subtitle "Doanh thu, lợi nhuận và công nợ theo chi nhánh". Actions (cùng hàng, cao 32px):
   - select chi nhánh (`.erp-input h-8 w-[200px]`, `data-testid="dashboard-branch"`),
   - select kỳ (`data-testid="dashboard-period"`),
   - khi `custom`: 2 ô `type="date"` (`dashboard-date-start`, `dashboard-date-end`) + nút "Áp dụng" (`.btn-secondary`, `dashboard-filter-btn`) — **chỉ gọi API khi bấm Áp dụng**,
   - nút làm mới (icon `RefreshCw`, `.btn-secondary w-8 px-0`, quay khi `isFetching`, `aria-label="Làm mới"`),
   - nút "Xuất Excel" (`.btn-primary`, icon `Download`, `data-testid="dashboard-export-btn"`); khi đang xuất: disabled + spinner; lỗi → `toast.error('Xuất Excel thất bại')`.
2. **4 thẻ KPI**: `grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4`. Mỗi thẻ `.card p-4`: hàng trên = nhãn 12px uppercase `ink-muted` + icon 16px trong ô 32×32 bo 6px `bg-primary-soft text-primary`; giá trị 22px semibold `text-ink tabular-nums`; dòng phụ 12px `ink-subtle`. **Bỏ** icon mờ 100px ở nền, bỏ hiệu ứng scale.
   - `metric-revenue` Tổng doanh thu (`formatCurrency`), phụ "Tổng tiền các hóa đơn trong kỳ".
   - `metric-profit` Lợi nhuận gộp (`formatCurrency`), phụ "Doanh thu trừ giá vốn".
   - `metric-turnover` Vòng quay tồn kho (`formatNumber(v, 2)`), phụ "Hệ số quay vòng vốn kho".
   - `metric-debt` Công nợ quá hạn (`formatCurrency`); nếu > 0: giá trị `text-danger` + `.badge-danger` "Cần thu hồi"; icon ô màu `bg-danger-soft text-danger`.
3. **Hàng dưới**: `grid grid-cols-1 lg:grid-cols-12 gap-4`.
   - **Biểu đồ** (`lg:col-span-7 .card p-4`, `data-testid="top-products-chart"`): tiêu đề "Top sản phẩm theo doanh thu" 15px semibold. Recharts `BarChart layout="vertical"`, cao `max(240, 44 × số SP)` px; `YAxis type="category" dataKey="productName" width={160}` với `tickFormatter` cắt tên > 22 ký tự thêm "…"; `XAxis type="number"` định dạng rút gọn (`1,5 tr`, `850 N` — viết hàm `formatCompact` trong `format.ts`); **một** `Bar dataKey="revenue"` màu `var(--color-primary)`, `radius={[0,4,4,0]}`, `barSize={18}`; `CartesianGrid horizontal={false} stroke="var(--color-line)"`; Tooltip tùy biến (card trắng bo 6px): tên SP, "Doanh thu: …", "Số lượng: … SP". **Bỏ** Legend, bỏ trục phải, bỏ `Cell` nhiều màu.
   - **Bảng Top sản phẩm** (`lg:col-span-5 .card`, `data-testid="top-products-table"`): header card "Chi tiết top sản phẩm" + chip "Top N"; bảng `.erp-table`: # (40px) · Sản phẩm · SL bán (`num`) · Doanh thu (`num`, `formatNumber`). Dòng `data-testid="top-product-row"`.
   - Không có dữ liệu → trạng thái trống (icon `PackageOpen`, "Chưa có dữ liệu bán hàng trong kỳ này") ở cả hai khối.
4. **Không** có khối cảnh báo tồn kho (R10). **Không** `min-h-screen`, **không** `space-y-8`.
5. Dữ liệu: giữ `useQuery` hiện có nhưng `queryKey: ['dashboardMetrics', branchId, start, end]`.

### Test Sprint 7

**Unit — `src/components/sales/__tests__/dashboardPeriod.test.ts`**

| ID | Kiểm tra | Kỳ vọng |
|---|---|---|
| U-DASH-01 | `getPeriodRange('month', new Date(2026, 8, 27))` | `{ start: 20260901, end: 20260927 }` |
| U-DASH-02 | `getPeriodRange('7d', new Date(2026, 8, 27))` | `{ start: 20260921, end: 20260927 }` |
| U-DASH-03 | `getPeriodRange('quarter', new Date(2026, 8, 27))` | `start: 20260701` |
| U-DASH-04 | `getPeriodRange('custom', new Date(), { start: '2026-01-01', end: '2026-01-31' })` | `{ start: 20260101, end: 20260131 }` |

**E2E — `tests/ui/dashboard.spec.ts`** (ADMIN; mock `**/api/v1/branches*` → `[{id:1,name:'Chi nhánh TP1'}]`, `**/api/v1/analytics/dashboard*` → `{ totalRevenue: 50000000, grossProfit: 15000000, inventoryTurnoverRatio: 1.25, totalOverdueDebt: 500000, topSellingProducts: [{productId:101, productName:'Sản phẩm 1', quantitySold: 120, revenue: 30000000}, {productId:102, productName:'Sản phẩm 2', quantitySold: 80, revenue: 20000000}] }`)

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-DASH-01 | ADMIN vào `/` | `dashboard-page` hiển thị; `metric-revenue` chứa `50.000.000`; `metric-debt` chứa `500.000` và "Cần thu hồi" |
| E-DASH-02 | — | `top-product-row` có 2 dòng; dòng đầu chứa "Sản phẩm 1" |
| E-DASH-03 | Bắt request dashboard đầu tiên | URL có `startDateKey=<yyyymm01 của tháng hiện tại>` (tính trong test bằng `new Date()` giờ địa phương) |
| E-DASH-04 | Chọn kỳ "Tùy chọn…" | Thấy `dashboard-date-start`, `dashboard-date-end`, `dashboard-filter-btn`; nhập 2026-01-01 → 2026-01-31, bấm Áp dụng → request mới chứa `20260101` và `20260131` |
| E-DASH-05 | Mock `**/api/v1/analytics/export/excel*` trả file; bấm `dashboard-export-btn` | Có sự kiện `download` |

---

## SPRINT 8 — Đồng bộ các màn hình danh sách & hoàn thiện (G5–G8)

**File:** `ProductList`, `CustomerList`, `SupplierList`, `StockList`, `DebtList`, `DebtStatement`, `BranchList`, `SalesList`, `StockMovementList`, `ConfirmDialog`, `DataState` (+ `EmptyState`, `ErrorState`, `LoadingState`), `ErrorBoundary`.

### Mẫu chung cho mọi trang danh mục

```
PageContainer
 └ PageHeader  title="Danh Mục …" (GIỮ NGUYÊN chữ)  actions=[ô tìm kiếm 280px, nút "+ Thêm" .btn-primary nếu có quyền]
 └ .card overflow-hidden
    └ table.erp-table   (header sticky, cột số dùng .num, ngày dùng formatDate, tiền dùng formatNumber)
    └ DataState (loading/empty/error) nằm TRONG card, ngay dưới header bảng
 └ Popup thêm/sửa: dùng lại QuickCreateDialog (khung) — form 2 cột nhãn trên ô, ô .erp-input h-8
```

### Các bước

1. Làm lần lượt từng trang theo mẫu trên. **Không đổi** logic query/mutation, quyền `isAdmin`, text tiêu đề, `data-testid` hiện có (vd `customer-page`).
2. Ô tìm kiếm: `.erp-input h-8 pl-8` + icon `Search` 16px `ink-subtle` bên trái; placeholder "Tìm theo mã, tên…".
3. Hành động từng dòng (Sửa/Xóa): nút icon `.btn-ghost w-7 px-0` với `aria-label`; Xóa hover `text-danger`.
4. `StockList`: cột Số lượng dùng `.num`; số âm hiển thị `text-danger` (đây là định dạng số âm theo A4, **không phải** tính năng cảnh báo — không thêm lọc/badge).
5. `SalesList` (subview "Danh sách phiếu" của bán hàng): cùng mẫu bảng, cột tiền `.num`, trạng thái dùng badge; bỏ padding thừa `p-4` bọc ngoài trong `SalesModule` nếu gây cuộn đôi.
6. `ConfirmDialog`: card bo 8px, nút xác nhận `.btn-primary` (hoặc `.btn-danger` khi là hành động xóa), nút hủy `.btn-secondary`; giữ nguyên text nút ("Xác nhận", "Hủy") vì test cũ dựa vào.
7. `DataState` & con: dùng token; LoadingState dạng skeleton 5 dòng cao 28px `bg-slate-100 animate-pulse`; EmptyState icon 32px `ink-subtle` + tiêu đề 14px + mô tả 13px; ErrorState có nút "Thử lại" `.btn-secondary`.
8. `ErrorBoundary`: card giữa màn hình, nút "Tải lại trang" `.btn-primary`.
9. **Rà lỗi màu cấm (R8):** tìm trong `src/` các chuỗi `teal-`, `indigo-`, `emerald-`, `purple-`, `pink-`, `rose-`, `blue-`, `green-`, `orange-`, `red-`, `bg-white` → thay bằng token (`bg-surface`, `text-danger`…). Kết quả tìm kiếm cuối cùng phải **rỗng** (trừ `_to_delete/` và `PrintInvoice.tsx` — file in giữ nguyên).
10. **Rà định dạng (G6):** tìm `toLocaleString(`, `toISOString().slice` trong `src/` → không còn (trừ nơi gửi payload lên API cần ISO).
11. **Rà focus/cursor (G8):** tab bằng bàn phím qua ribbon → tab bar → form: mọi phần tử tương tác đều có viền focus nhìn thấy được.
12. Viết lại `DESIGN_SYSTEM.md` (**UTF-8**) theo Phần 2 của tài liệu này: bảng token, class dùng chung, quy tắc số tiền (A4), mật độ (A6), cách dùng `GenericDocumentForm` (ví dụ cấu hình rút gọn), quy tắc testid.

### Test Sprint 8

**Unit** — cập nhật `ProductList.test.tsx`, `CustomerList.test.tsx`, `StockMovementList.test.tsx` nếu hỏng do cấu trúc mới (giữ nguyên ý nghĩa kiểm tra: tiêu đề + dữ liệu hiển thị).

**E2E — `tests/ui/lists.spec.ts`** (ADMIN; mock `**/api/v1/catalog/products*` → fixtures)

| ID | Bước | Kỳ vọng |
|---|---|---|
| E-LIST-01 | `ribbon-tab-DanhMuc` → nút "Sản Phẩm" | Thấy "Danh Mục Sản Phẩm", 2 dòng sản phẩm |
| E-LIST-02 | Gõ `SP02`/"Sản phẩm 2" vào ô tìm | Chỉ còn 1 dòng "Sản phẩm 2" |
| E-LIST-03 | Mock `**/api/v1/inventory/stock*` trả 1 dòng `quantity: -3` | Ở trang Tồn kho, ô số lượng hiển thị `-3` và có class chứa `text-danger` |

---

## SPRINT 9 — Kiểm thử tổng & bàn giao

1. Chạy toàn bộ: `npx tsc --noEmit`, `npm run lint`, `npx vitest run`, `npm run build`, `npx playwright test tests/ui`. So với baseline Sprint 0: tsc/lint **không tăng lỗi**, mọi test mới **pass**.
2. Test Playwright cũ ở `tests/` gốc (không phải `tests/ui`): chạy thử; test nào hỏng **vì đổi cấu trúc/định dạng số** (vd chờ `250,000`, dùng `input[type="number"]`, dùng `mock-access-token`) → chuyển vào `tests/_legacy/` và liệt kê trong báo cáo kèm lý do. **Không sửa logic nghiệp vụ để chiều test cũ.**
3. Kiểm tra tay theo checklist (ghi kết quả vào `docs/UI_UX_PROGRESS.md`):
   - [ ] 1366×768: phiếu bán hàng không có thanh cuộn ngang; header 3 khối đọc được.
   - [ ] Dropdown khách hàng/sản phẩm luôn thấy đầy đủ, ở dòng đầu và dòng cuối bảng.
   - [ ] `Esc` khi dropdown đang mở chỉ đóng dropdown, **không** hiện hộp thoại hủy phiếu.
   - [ ] Nhập nhanh bằng bàn phím: chọn hàng → Số lượng → Enter → Đơn giá → Enter → dòng mới.
   - [ ] 3 phiếu bán/nhập/trả nhìn cùng một khung.
   - [ ] Login đẹp ở 1366 và ở 800px (chỉ còn form).
   - [ ] Dashboard: 4 KPI thẳng hàng, biểu đồ đọc được tên sản phẩm, bộ lọc hoạt động.
   - [ ] Tất cả danh mục cùng một kiểu bảng.
4. Cập nhật `docs/UI_UX_PROGRESS.md`: tóm tắt từng sprint, danh sách file đã đổi, test đã chuyển `_legacy`, việc còn tồn.

---

## PHỤ LỤC A — Danh sách `data-testid` phải giữ

`login-form`, `login-username`, `login-password`, `login-submit`, `login-error`, `ribbon-tab-<id>`, `ribbon-btn-inbound`, `subview-form`, `subview-list`, `btn-add`, `btn-edit`, `btn-save`, `btn-cancel`, `btn-delete`, `btn-confirm`, `btn-print`, `btn-exit`, `sales-customer-combo`, `sales-product-combo-<lineId>`, `sales-add-line`, `sales-line-row`, `sales-line-quantity`, `sales-line-price`, `sales-line-total`, `sales-error-msg`, `customer-page`, và mọi testid khác đang có trong các trang danh mục (tìm bằng `data-testid` trước khi sửa từng file).

**Testid mới thêm trong đợt này:** `tab-<id>`, `ribbon-user`, `ribbon-logout`, `ribbon-btn-return`, `combobox-dropdown`, `combobox-create-new`, `doc-code`, `sum-*`, `<prefix>-line-delete`, `<prefix>-grand-total`, `inbound-*`, `return-*`, `inbound-list-row`, `return-list-row`, `login-toggle-password`, `login-dev-hint`, `dashboard-page`, `dashboard-branch`, `dashboard-period`, `dashboard-date-start`, `dashboard-date-end`, `dashboard-filter-btn`, `dashboard-export-btn`, `metric-revenue`, `metric-profit`, `metric-turnover`, `metric-debt`, `top-products-chart`, `top-products-table`, `top-product-row`.

## PHỤ LỤC B — Ghi nhận nhưng KHÔNG làm trong đợt này

1. **Cảnh báo tồn kho thấp/âm** (khối Dashboard, badge ribbon, cột "Tồn" trong phiếu và dropdown, cảnh báo bán vượt tồn). Backend đã có bảng `inventory_alert_config` và `LowStockAlertJob` nhưng chưa có API đọc — sẽ làm ở đợt sau.
2. **Chuyển tab làm mất dữ liệu đang nhập:** `App.tsx` chỉ render tab đang active nên tab khác bị unmount. Sửa đúng cách cần giữ các tab mounted **và** chỉ cho `MdiModuleLayout` của tab đang active bắt phím tắt — để đợt sau.
3. **Xuất trả hàng mua:** chưa có API, nút đã ẩn.
4. Chiết khấu / VAT trên phiếu bán hàng hiện **không được gửi lên backend** (payload không có trường này) — giữ nguyên hành vi, ghi nhận để xử lý nghiệp vụ sau.
5. Sửa/Xóa phiếu nhập và phiếu trả: backend chưa có API — nút không hiển thị.
