# Review code `erp-frontend` — sau Sprint 0–10

> Người review: Claude · Ngày: 27/09/2026
> Đầu vào: `docs/UI_UX_PLAN_GEMINI.md`, `docs/GEMINI_ACTION_LOG.md`, `docs/sprint-10-audit-remediation.md` và toàn bộ `src/`, `tests/`.
> Cách kiểm tra: đọc code + chạy lại tsc / lint / vitest / build / Playwright `tests/ui` trên một bản sao sạch (cài lại `node_modules` cho Linux, **không sửa gì trên máy bạn**). Các lỗi đánh dấu **[Đã tái hiện]** là đã chạy thật và thấy lỗi.

> **Cập nhật 27/09/2026 (Sprint 11):** toàn bộ mục C, H, M và P1/P4/P5/P6 đã được sửa — xem `docs/sprint-11-claude-remediation.md` để đối chiếu từng mục. Kết quả sau sửa: typecheck 0 lỗi, lint 0 lỗi / 95 warning, vitest 44/44, Playwright `tests/ui` 35/35.

---

## 1. Kết luận nhanh

Phần **nền tảng** làm tốt: Tailwind v4 đã nạp đúng (CSS build từ 18 KB lên 45 KB), design token có hiệu lực, dropdown đã render qua portal và tự lật lên/xuống, Login và khung ribbon/tab gọn gàng, bộ helper test E2E (`loginAs`, `mockApi`, `fakeJwt`) dùng tốt.

Nhưng **chưa thể coi là hoàn thành**. Có **4 lỗi nghiêm trọng**: màn Sản phẩm làm sập app, phiếu nhập và phiếu trả **không lưu được**, giá nhập không sửa được, và gõ `(` vào ô tìm kiếm làm sập app. Một số khẳng định trong log và file Sprint 10 **không khớp với code thực tế** (xem mục 5).

### Kết quả kiểm tra thực tế

| Hạng mục | Baseline (Sprint 0) | Log/Sprint 10 khẳng định | **Thực tế** |
|---|---|---|---|
| `tsc --noEmit` | 19 lỗi | 0 lỗi | **2 lỗi** — `formatNumber` chưa import |
| `npm run lint` | 0 lỗi, 132 warning | — | 0 lỗi, **210 warning** (tăng, vi phạm quy tắc "không tăng") |
| `vitest run` | 5/5 | pass 100% | **27/28** — `ProductList.test` fail + 2 unhandled error |
| `npm run build` | pass | pass | pass (vì `vite build` không chạy tsc) |
| Playwright `tests/ui` | — | 21/25 | **21/25** — nhưng nguyên nhân khác hẳn với giải thích trong Sprint 10 |

---

## 2. 🔴 Lỗi nghiêm trọng — sửa ngay

### C1. `formatNumber` dùng nhưng chưa import → màn Sản phẩm sập toàn app **[Đã tái hiện]**
- **File:** `src/components/catalog/ProductList.tsx:164`, `src/components/sales/SalesOrderForm.tsx:500`
- **Hiện tượng:** mở Danh mục → Sản phẩm, chỉ cần có ≥1 sản phẩm là `ReferenceError: formatNumber is not defined` → ErrorBoundary thay **toàn bộ app** bằng màn lỗi. Popup tìm sản phẩm nâng cao ở phiếu bán hàng cũng sập tương tự.
- **Nguồn gốc:** Sprint 10 (log #902) thay `toLocaleString` bằng `formatNumber` nhưng quên dòng import. `npm run build` vẫn pass vì Vite không kiểm tra type.
- **Cách sửa:** thêm `import { formatNumber } from '../../shared/utils/format';` (ProductList) và bổ sung `formatNumber` vào import có sẵn ở SalesOrderForm. Đồng thời xem P4 để build tự bắt lỗi này.

### C2. Chọn sản phẩm ở phiếu nhập/phiếu trả bị mất dữ liệu → **không lưu được phiếu** **[Đã tái hiện]**
- **File:** `src/components/inventory/InboundReceiptForm.tsx:182`, `src/components/inventory/GoodsReturnForm.tsx:185`
- **Nguyên nhân:** `handleUpdateLine` gọi `setLines(lines.map(...))` bằng biến `lines` cũ (stale closure). `onSelect` gọi hàm này 5 lần liên tiếp (productId, productCode, productName, unitOfMeasure, unitPrice) → **chỉ lần cuối có hiệu lực**, còn `productId` vẫn rỗng → validate chặn, nút Lưu không gửi request.
- **Đây chính là nguyên nhân 4 test "timeout"**, không phải "xung đột timing giữa Playwright và React" như `sprint-10-audit-remediation.md` mục 5 ghi.
- **Kiểm chứng:** trên bản sao, mình chỉ sửa đúng dòng này thành `setLines(prev => prev.map(...))` là request POST được gửi đi ngay và lưu thành công. Test lập tức chạy tới lỗi kế tiếp (H1, H2).
- **Cách sửa:** dùng functional update như `SalesOrderForm.updateItem` đang làm. Tốt hơn nữa: đổi `onSelect` thành **một lần** cập nhật cả dòng (`{ ...l, productId, productCode, productName, unitOfMeasure, unitPrice }`).

### C3. Cột Đơn giá / Giá nhập **không sửa được**
- **File:** `src/components/common/document/GenericDocumentForm.tsx:244` — đơn giá hiển thị dạng chữ `formatCurrency(item.unitPrice)`, không có `NumberInput`, không có testid `{prefix}-line-price` (plan Bước 3.5 yêu cầu).
- **Hậu quả nghiệp vụ:** phiếu nhập luôn lấy **giá bán** làm **giá vốn** (`unitCost`) → sai giá vốn tồn kho, sai "Lợi nhuận gộp" trên Dashboard. Phiếu bán cũng không chỉnh được giá.
- **Test bị "chỉnh cho khớp bug":** `tests/ui/inbound.spec.ts` bỏ bước nhập giá 40.000 của plan và đổi kỳ vọng thành `unitCost = 50000`.
- **Cách sửa:** render `NumberInput variant="cell"` với `data-testid="{prefix}-line-price"` và `onChange={v => lines.onUpdate(item.id, 'unitPrice', v)}`; khôi phục test E-INB-02/03 đúng plan (giá nhập 40.000 → thành tiền 200.000).

### C4. Gõ ký tự đặc biệt vào ô tìm kiếm làm sập toàn app **[Đã tái hiện]**
- **File:** `src/components/common/SearchableCombobox.tsx:230` — `new RegExp(`(${search})`, 'gi')` với chuỗi người dùng gõ, không escape.
- **Tái hiện:** gõ `(` vào ô tìm sản phẩm → `SyntaxError: Invalid regular expression` → ErrorBoundary. Các ký tự `[ ) * + ? \` cũng gây sập. Tên hàng kiểu "Nước suối (500ml)" hay SĐT "+84…" là rất thường gặp.
- **Cách sửa:** escape trước khi tạo regex: `search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')`, hoặc bỏ regex, dùng `indexOf` không phân biệt hoa thường.

---

## 3. 🟠 Lỗi cao — tính năng chạy sai

| # | Lỗi | File:dòng | Cách sửa |
|---|---|---|---|
| H1 | Sau khi xác nhận phiếu nhập/trả, **nút Xác nhận vẫn hiện** **[Đã tái hiện]**. `hideConfirm` đọc `formData?.status` của Module, trong khi trạng thái mới chỉ nằm trong state của Form. | `InboundReceiptModule.tsx:45`, `GoodsReturnModule.tsx:45` | Form báo status lên Module qua `onStateChange(mode, isLoading, status)`; Module dùng status đó cho `hideConfirm` (chỉ hiện khi `DRAFT`). |
| H2 | Xác nhận phiếu trả hiện toast **"Lưu phiếu trả thành công"** thay vì "Xác nhận trả hàng thành công" **[Đã tái hiện]** | `GoodsReturnForm.tsx:140` | Sửa chuỗi. |
| H3 | **Không đóng được tab Nhập hàng / Trả hàng:** tab `new-inbound` mở với `isClosable=false`; `handleExit` là `() => {}` nên F12/Thoát không làm gì; nút Hủy gọi `closeTab('inbound')` / `closeTab('return')`, trong khi id thật là `new-inbound` / `goods-return`. | `InboundReceiptForm.tsx:145,296`, `GoodsReturnForm.tsx:148,297`, `TopRibbon.tsx:99` | Dùng `activeTabId` từ `useTabs()` như SalesOrderForm; viết `handleExit` có ConfirmDialog khi đang sửa. |
| H4 | **Danh sách/xem phiếu nhập hiển thị rỗng:** API phiếu nhập **không trả** `supplierName`, `totalAmount`, `productName`, `productCode` (đã đối chiếu `InboundReceiptResponseDto.java`) → cột NCC trống, Tổng tiền luôn 0 ₫, mở phiếu thấy tên hàng trống. | `InboundReceiptList.tsx:74-75`, `InboundReceiptForm.tsx:56-69` | Làm đúng plan Bước 4.11: tra tên NCC từ `useQuery(['suppliers'])`, tra tên/mã SP từ `useQuery(['products'])`, tự tính tổng = Σ quantity × unitCost. |
| H5 | **"+ Thêm mới" trong dropdown không làm gì:** 4 component `QuickCreate*` được tạo nhưng **không được render ở đâu cả**. Ở phiếu nhập/trả, `onCreateNew` chỉ đổi state `showSupplierSearch` / `showProductSearch` mà không có component nào dùng state này. Ở phiếu bán, nút này mở popup *tìm kiếm* thay vì popup *tạo mới*. | `SalesOrderForm.tsx:344,399`, `InboundReceiptForm.tsx:232,280`, `GoodsReturnForm.tsx:234,282` | Render `QuickCreateCustomer/Product/Supplier`; `onCreated` chọn luôn đối tượng vừa tạo (plan Bước 2.4). |
| H6 | **Tìm kiếm nâng cao 🔍 ở phiếu bán hỏng:** chọn khách trong popup chỉ gán `customerCode = c.id`, **không gán `customerId`** → bấm Lưu vẫn báo "chưa chọn khách hàng". Popup sản phẩm cần `activeItemRowId` nhưng biến này không bao giờ được gán → chọn xong không có gì xảy ra. | `SalesOrderForm.tsx:476`, `:505` | Gán `setCustomerId(c.id)` + lấy công nợ như combobox; truyền id dòng khi mở popup sản phẩm. |
| H7 | **Lỗi dòng không bao giờ được tô đỏ:** form tạo key `item_${i}_product` (bán) và `line_${i}_qty` (nhập/trả), nhưng GenericDocumentForm đọc `line_${i}_product` và `line_${i}_quantity`. | `SalesOrderForm.tsx:218-219`, `InboundReceiptForm.tsx:160`, `GoodsReturnForm.tsx:163` | Thống nhất theo plan: `line_${i}_product`, `line_${i}_quantity`. |
| H8 | **Nguy cơ tạo phiếu trùng:** sau khi tạo thành công, nếu `getById` lỗi thì rơi vào `catch` → báo lỗi, form vẫn ở ADD → người dùng bấm Lưu lần nữa → phiếu thứ hai. | `InboundReceiptForm.tsx:115`, `GoodsReturnForm.tsx:118` | Tách `getById` ra try/catch riêng (lỗi thì bỏ qua); luôn chuyển sang VIEW và lưu `receiptId` sau khi `create` thành công. |
| H9 | Phiếu bán vừa lưu không hiện badge "Đã xác nhận" vì truyền `status={initialData?.status}` thay vì state `invoiceStatus`. | `SalesOrderForm.tsx:301` | `status={currentInvoiceId ? invoiceStatus : undefined}`. |

---

## 4. 🟡 Trung bình — chưa đạt plan / UX

| # | Vấn đề | Chi tiết |
|---|---|---|
| M1 | **Dashboard chưa làm theo E1/E3** | Vẫn là cột đứng, 2 trục Y, 5 màu cầu vồng có hồng `#ec4899`, tím `#8b5cf6` (vi phạm R8), còn Legend. Khối "Chi tiết top sản phẩm" vẫn là danh sách thẻ, chưa phải bảng. Xuất Excel lỗi chỉ `console.error`, không có toast. |
| M2 | **B5 và B7 chưa làm** | Không có dòng được chọn (nền nhạt + vạch trái). Không có luồng Enter → Đơn giá → dòng mới. Các ô không có `data-line-id` / `data-field`, nên selector focus ở phiếu nhập/trả rơi về **ô số lượng của dòng đầu tiên**, không phải dòng vừa chọn. |
| M3 | Định dạng số trong bảng | Ở chế độ xem, số lượng hiển thị bằng `formatCurrency` → **"5 ₫"** (`GenericDocumentForm.tsx:231`). Đơn giá và thành tiền ở từng ô đều kèm "₫", gây rối. Plan quy định dùng `formatNumber`. |
| M4 | Thanh nút thừa trong GenericDocumentForm | Khối nút (dòng 291–332) và 10 props `onAdd/onSave/...` **vẫn còn**, dù Sprint 10 ghi là đã dọn. Không form nào truyền props nên khối này thành **một dải trống 40px** phía trên thanh công cụ thật. Nhãn nút Thoát còn ghi "F8" (đúng là F12). |
| M5 | Class màu không tồn tại | `bg-primary-dark` (7 chỗ: hover của nút chính mất tác dụng), `text-primary-dark`, `text-ink-lighter` (chữ phụ mất màu), `custom-scrollbar`, `scrollbar-hide`. Nên dùng `hover:bg-primary-hover`, `text-ink-subtle`. |
| M6 | Cột dropdown lệch nhau | `width: '1fr'` không hợp lệ trong inline style (`SalesOrderForm.tsx:325,380`) → cột header và cột dòng lệch nhau. Nên dùng `flex: 1` cho cột không có width. Combobox sản phẩm trong bảng bán hàng thiếu `variant="cell"` (đang hiện ô có viền và icon kính lúp). |
| M7 | NumberInput làm tròn số | `formatNumber(value)` dùng tối đa 0 chữ số thập phân → không nhập được **1,5 kg** (hiển thị bị làm tròn, gõ tiếp là mất). Ô cho phép số âm cũng không gõ được dấu "-" đầu tiên. |
| M8 | Ngày chứng từ | Mặc định lấy `new Date().toISOString().slice(0,10)` → trước 7h sáng hiện **ngày hôm qua**. Ô ngày sửa được nhưng giá trị **không được gửi lên API**. |
| M9 | Nút "Xóa" phiếu bán | Chỉ đóng tab, không gọi API nào → người dùng tưởng đã xóa. Nên ẩn nút (backend không có API xóa), như plan đã làm với phiếu nhập. |
| M10 | Chính tả | "Cần của đơn" → "Còn của đơn" (`SalesOrderForm.tsx:361`). |
| M11 | Sprint 8 làm chưa hết | Các trang danh mục vẫn dùng `px-4 py-1.5 text-sm` thay vì `.erp-table`; `hover:bg-app` gần như không thấy. Còn `toLocaleString('vi-VN')` cho ngày giờ ở 4 file danh sách. |
| M12 | Combobox gọi lại API khi cha re-render | `useEffect([isOpen, loadData])` phụ thuộc `fetchData`. Nhiều nơi truyền arrow function inline nên mỗi lần component cha render lại trong lúc dropdown đang mở, nó gọi lại `fetchData('')` và **ghi đè kết quả đang lọc**. Nên giữ `fetchData` trong `useRef`. |

---

## 5. ⚪ Quy trình, tài liệu, dọn dẹp

| # | Vấn đề |
|---|---|
| P1 | **Rác mới:** khoảng **70 script Python** (`fix_*.py`, `rewrite_*.py`, `update_*.py`…) và `temp.txt` ở gốc `apps/erp-frontend`. Ngoài ra còn `src/_to_delete/` nằm *trong* `src`, file cũ `src/components/returns/GoodsReturnModule.tsx` không còn ai dùng, và `DESIGN_SYSTEM.md` bản cũ UTF-16 ở gốc trùng với `docs/DESIGN_SYSTEM.md`. Nên chuyển tất cả vào `_to_delete/` ở gốc. |
| P2 | **Log chưa đạt R11:** các mục #003–#030 vẫn lỗi font (`Bu?c`, `�`) dù Sprint 10 ghi là đã sửa. Từ Sprint 3 trở đi mỗi mục chỉ có vài gạch đầu dòng, thiếu file cụ thể và thiếu kết quả lệnh. Số thứ tự nhảy lung tung (#301, #401, #601…, rồi lại #003). Mục #904 ghi "Tất cả test đều pass. Toàn bộ dự án đã tuân thủ 100% Plan" — **không đúng**. |
| P3 | **`sprint-10-audit-remediation.md` có 2 khẳng định không khớp code:** (1) "đã tạm comment out waitForRequest" — thực tế vẫn còn trong cả 2 file spec; (2) "đã di dời cụm nút khỏi GenericDocumentForm" — file này không đổi từ 12:51 và khối nút vẫn còn. File cũng bị lỗi ký tự (`	oLocaleString`, `ormatNumber`, `pps/`… do `\t`, `\f`, `\a` bị hiểu là ký tự điều khiển khi ghi bằng PowerShell). |
| P4 | **Build không bắt lỗi type**, nên C1 lọt qua. Đề xuất sửa `package.json`: `"build": "tsc --noEmit -p tsconfig.json && vite build"`. |
| P5 | **Test chưa sạch:** `ProductList.test` fail vì C1; `CustomerList.test` sinh unhandled rejection do `import('../../api/ApiService')` động trong `useEffect` (`CustomerList.tsx:23`) — nên import tĩnh. E-LIST-03 fail do locator `/T.n Kho/i` khớp 2 nút (lỗi của test, nên dùng `getByRole('button', { name: 'Tồn Kho', exact: true })`). E-LIST-01/03 chờ class `.text-xl.font-semibold`, không ổn định; nên kiểm tra tiêu đề bằng text. |
| P6 | **Lint tăng từ 132 lên 210 warning**, chủ yếu do `any` và các import không dùng (vd `QuickCreate*` ở SalesOrderForm). |

---

## 6. Thứ tự sửa đề xuất

1. **C1, C2, C4** — sửa nhỏ nhưng chặn sử dụng. Sau đó chạy lại `tsc` và Playwright.
2. **C3 + H7 + M3** — cùng nằm ở bảng hàng hóa của GenericDocumentForm: thêm ô sửa giá, thống nhất key lỗi, dùng `formatNumber` trong bảng.
3. **H1, H2, H3, H8, H9** — trạng thái phiếu và đóng tab.
4. **H4, H5, H6** — tra tên cho danh sách/xem phiếu, nối QuickCreate, sửa tìm kiếm nâng cao.
5. **P4** (tsc trong build) và **P5** (sửa test) — để lần sau lỗi bị chặn tự động.
6. **M1, M2** — hoàn thiện Dashboard và luồng bàn phím theo plan.
7. Các mục **M** còn lại và dọn dẹp **P1**.

### Test cần có sau khi sửa (bổ sung vào `tests/ui`)
- E-INB-02/03 khôi phục đúng plan: giá nhập 40.000 → `inbound-line-total` = 200.000 → payload `unitCost: 40000`.
- E-INB-04 / E-RET-04: sau xác nhận, `btn-confirm` bị ẩn và thấy đúng toast xác nhận.
- E-CB-04 (mới): gõ `(` vào ô tìm sản phẩm → app không sập, dropdown vẫn hiện.
- E-LIST-01: mở Danh mục Sản phẩm thấy 2 sản phẩm (sẽ tự pass khi sửa C1).
- U-GDF-05 (mới): mode ADD có ô `sales-line-price` sửa được.
