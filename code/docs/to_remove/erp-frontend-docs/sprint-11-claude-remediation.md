# Sprint 11: Sửa lỗi sau review (Claude)

> Ngày: 27/09/2026 · Đầu vào: `docs/CODE_REVIEW_FRONTEND.md` · Phạm vi: chỉ `apps/erp-frontend` (không sửa backend, không sửa `packages/api-contract`).

## 1. Mục tiêu
Sửa toàn bộ lỗi nghiêm trọng / cao / trung bình trong báo cáo review, hoàn thiện các phần của plan còn thiếu (ô sửa giá, luồng bàn phím, Dashboard, bảng danh mục), và có bộ test E2E chứng minh các luồng chính chạy đúng.

## 2. Kết quả kiểm tra (chạy trên bản sạch, cài lại `node_modules`)

| Hạng mục | Trước (Sprint 10 thực tế) | Sau Sprint 11 |
|---|---|---|
| `npm run typecheck` | 2 lỗi | **0 lỗi** |
| `npm run lint` | 0 lỗi / 210 warning | **0 lỗi / 95 warning** (baseline Sprint 0: 132) |
| `npx vitest run` | 27/28, 2 unhandled error | **44/44**, không lỗi |
| `npm run build` | pass (không typecheck) | **pass, có typecheck** |
| `npx playwright test tests/ui` | 21/25 | **35/35**; lặp 3 lần: **105/105** |

## 3. Đối chiếu từng mục review

| Mục | Trạng thái | Cách sửa |
|---|---|---|
| C1 `formatNumber` chưa import | ✅ | Bổ sung import; build giờ chạy `tsc` trước (P4) nên lỗi kiểu này bị chặn. |
| C2 Chọn SP ở phiếu nhập/trả mất dữ liệu | ✅ | Hook `useDocumentLines()` — mọi cập nhật dòng là functional; chọn SP dùng **một** `patchLine(productToLinePatch(p))`. |
| C3 Không sửa được đơn giá / giá nhập | ✅ | `NumberInput` cho cột giá (`<prefix>-line-price`); test E-INB khôi phục đúng plan (giá nhập 40.000 → `unitCost: 40000`). |
| C4 Gõ `(` làm sập app | ✅ | `escapeRegExp()` khi tô đậm; test U-CB-06, E-CB-04. |
| H1 Nút Xác nhận không ẩn | ✅ | Form báo `status` qua `onStateChange`; module dùng `hideConfirm={status !== 'DRAFT'}`. |
| H2 Sai toast xác nhận trả hàng | ✅ | "Xác nhận trả hàng thành công". |
| H3 Không đóng được tab | ✅ | Đóng bằng `activeTabId`; `handleExit` có hỏi xác nhận khi đang nhập; tab Nhập hàng cho phép đóng. |
| H4 Danh sách/xem phiếu nhập trống | ✅ | Tra tên NCC, tên/mã SP từ danh mục; tự tính tổng tiền = Σ SL × giá nhập. |
| H5 "+ Thêm mới" không làm gì | ✅ | Gắn `QuickCreateCustomer/Product/Supplier` (chỉ ADMIN); tạo xong tự chọn luôn. |
| H6 Tìm kiếm nâng cao không gán khách | ✅ | Dùng chung `selectCustomer()` cho combobox, popup tìm kiếm và thêm nhanh; bỏ popup tìm SP bị hỏng. |
| H7 Key lỗi dòng lệch | ✅ | Thống nhất `lineErrorKey(i, 'product' \| 'quantity')`; ô lỗi tô đỏ (test E-INB-06, E-SALE-08). |
| H8 Nguy cơ tạo phiếu trùng | ✅ | Sau `create` thành công chuyển VIEW ngay; `getById` lỗi thì bỏ qua. Nút Lưu bị khóa khi đang lưu. |
| H9 Badge phiếu bán | ✅ | Dùng `invoiceStatus`. |
| M1 Dashboard | ✅ | Cột ngang, 1 màu primary, 1 trục, không legend; bảng Top SP; toast khi xuất Excel lỗi; kiểm tra khoảng ngày tùy chọn. |
| M2 Dòng chọn + luồng Enter | ✅ | Nền nhạt + vạch trái; chọn SP → SL → Enter → Đơn giá → Enter → dòng mới (test U-GDF-07/08, E-SALE-07). |
| M3 "5 ₫" ở số lượng | ✅ | Bảng dùng `formatNumber`; số lượng hỗ trợ 3 chữ số thập phân. |
| M4 Dải trống 40px / nhãn F8 | ✅ | Bỏ khối nút trong GenericDocumentForm; thanh nút chỉ ở `MdiModuleLayout` và **chỉ hiện nút có handler**. |
| M5 Class không tồn tại | ✅ | Thay bằng token có thật; thêm utility `scrollbar-hide`. |
| M6 Cột dropdown lệch | ✅ | Cột không có width tự giãn (`flex: 1`); SP trong bảng dùng `variant="cell"`. |
| M7 Không nhập được 1,5 / dấu "-" | ✅ | `NumberInput` giữ chuỗi đang gõ (`formatDraftNumber`). |
| M8 Ngày chứng từ | ✅ | Mặc định theo giờ địa phương; hiển thị chỉ đọc vì API không nhận ngày. |
| M9 Nút Xóa giả | ✅ | Ẩn Sửa/Xóa ở phiếu bán (backend chưa có API). |
| M10 "Cần của đơn" | ✅ | "Còn của đơn". |
| M11 Danh mục chưa đồng bộ | ✅ | Component `ListTable` cho 9 màn danh sách; tìm kiếm không dấu + theo mã; Khách hàng có nút Sửa/Xóa và hộp thoại xóa (trước đây bấm Xóa không làm gì). SalesList: ô tìm kiếm giờ lọc thật, bỏ nút "Lọc"/"In" không chức năng. |
| M12 Combobox gọi lại API | ✅ | `fetchData` giữ trong ref. |
| P1 Rác | ✅ | 71 script `.py` + `temp.txt` → `_to_delete/gemini-scripts/`; `DESIGN_SYSTEM.md` cũ (UTF-16) → `_to_delete/`; `src/_to_delete/*`, `src/components/returns/*` → `_to_delete/src/`. Còn 4 thư mục rỗng (`src/_to_delete`, `src/layout`, `src/state`, `src/components/returns`) vì phiên này không có quyền xóa — bạn tự xóa. |
| P2/P3 Log & tài liệu Sprint 10 | ⚠️ Giữ nguyên | Không sửa lịch sử log của Gemini (theo R11). Nội dung sai đã ghi trong `CODE_REVIEW_FRONTEND.md`. |
| P4 Build không typecheck | ✅ | `"build": "npm run typecheck && vite build"`, thêm script `typecheck`, `test:e2e`. |
| P5 Test chưa sạch | ✅ | Sửa `CustomerList` import tĩnh; viết lại `lists.spec.ts` (mock đúng `/api/v1/inventory/stock`, locator theo testid). |
| P6 Lint tăng | ✅ | 95 warning (< 132 baseline). |

**Sửa thêm ngoài review:** `ConfirmDialog` nút thiếu class `btn` (không có viền), Esc để hủy, `autoFocus` nút xác nhận; tất cả hộp thoại có `role="dialog"` và chặn phím tắt form khi mở; `useSalesInvoice` bỏ query danh sách hóa đơn chạy thừa mỗi lần mở form (và bỏ `getById` vi phạm rules-of-hooks); `GoodsReturnList` tiêu đề cột "Nhà cung cấp" → "Khách hàng"; `ApiService.GoodsReturn` có kiểu dữ liệu đúng (create trả UUID dạng chuỗi).

## 4. Quyết định kiến trúc
- **`documentLines.ts` là nơi duy nhất thao tác dòng hàng.** Ba form phiếu dùng chung `useDocumentLines`, `validateLines`, `productToLinePatch`, `focusLineCell`. Lý do: lỗi C2 xảy ra vì mỗi form tự viết `setLines(lines.map…)`.
- **Form báo trạng thái chứng từ lên Module** (`onStateChange(mode, isLoading, status)`), Module quyết định nút nào hiện. Không truyền props nút xuống GenericDocumentForm nữa (tránh trùng testid).
- **Form được giữ mounted khi xem "Danh sách phiếu"** (ẩn bằng `hidden`) để không mất dữ liệu đang nhập.

## 5. Hướng dẫn đọc code
1. `src/components/common/document/documentLines.ts` → `GenericDocumentForm.tsx`
2. `src/components/inventory/InboundReceiptForm.tsx` (mẫu chuẩn cho form phiếu) → `InboundReceiptModule.tsx`
3. `src/components/sales/SalesOrderForm.tsx` (giữ nguyên payload và công thức tính tiền)
4. `src/shared/components/Form/NumberInput.tsx`, `src/shared/utils/format.ts`
5. `tests/ui/inbound.spec.ts` (mẫu test E2E đầy đủ: lưu → xác nhận → danh sách → đóng tab)

## 6. Rủi ro / nợ kỹ thuật còn lại
- Chiết khấu, VAT, ngày chứng từ trên phiếu bán **vẫn không gửi lên backend** (API không có trường) — giữ nguyên hành vi cũ.
- Chuyển sang **tab khác trên thanh tab** (không phải "Danh sách phiếu") vẫn unmount form → mất dữ liệu (Phụ lục B của plan).
- "Kho xuất" ở phiếu bán vẫn là chữ cố định "CN Trung Tâm"; phiếu nhập/trả hiển thị "Chi nhánh đang đăng nhập" vì API đăng nhập không trả tên chi nhánh.
- 95 warning lint còn lại chủ yếu là `any` trong `ApiService.ts` / `axiosInstance.ts` (code cũ).
- Test Playwright trong `tests/_legacy/` không chạy (đã bị loại khỏi config từ Sprint 0).

## 7. Cách chạy
```bash
cd apps/erp-frontend
npm run typecheck
npm run lint
npx vitest run
npm run build
npm run test:e2e        # không cần backend, API được mock
```

## 8. Bàn giao
Bước tiếp theo đề xuất: chạy thử với backend thật (đặc biệt luồng phiếu nhập → xác nhận → tồn kho tăng), rồi mới làm tính năng cảnh báo tồn kho (cần thêm API ở backend).
