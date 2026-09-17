# Sprint 8: Frontend contract
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Đồng bộ OpenAPI contract từ Backend vừa được update (Debt, Movement, Inbound, Invoice) xuống frontend (`@erp/api-contract`).
- Loại bỏ các kiểu dữ liệu lỏng lẻo (`any`) và mismatch data-binding ở các component của frontend để đảm bảo Type-Safety.

## 2. Thành quả đạt được (Done)
- Trigger Integration test `OpenApiGeneratorTest` để tự động tạo file `openapi.json` mới nhất cho Backend (bao gồm `ReceivableDebtMovementResponseDto`, `OpeningBalanceRequestDto`).
- Chạy `npm run generate` trong `packages/api-contract` để generate file `api.d.ts`.
- Cập nhật các kiểu dữ liệu trong file `ApiService.ts`:
  - `InboundReceipt.create` trả về đúng Type `InboundReceiptCreateResponseDto`.
  - Bổ sung `Debt.getMovements` với Type `ReceivableDebtMovementResponseDto[]`.
- Các file Component như `DebtList.tsx`, `StockList.tsx`, `SalesList.tsx` được review lại, xác nhận mapping chính xác tới các DTO backend sau khi OpenAPI được chuẩn hoá (`c.customerName`, `c.productName`).
- Frontend build và pass type-check (`tsc --noEmit`) 100%.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Sử dụng Monorepo package `@erp/api-contract` làm Single Source of Truth cho Type. Component UI KHÔNG tự định nghĩa DTO.
- **Lý do:** Giúp phát hiện sớm lỗi mismatch qua quá trình compile type-check của TypeScript. Nếu Backend đổi response, frontend run build sẽ rớt (compile error) ngay lập tức, ngăn ngừa lỗi runtime undefined như đã được Audit report phát hiện.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `packages/api-contract/openapi.json`: Bản contract dạng json mới nhất.
2. `packages/api-contract/src/generated/api.d.ts`: Bản DTO type bằng TS.
3. `apps/erp-frontend/src/api/ApiService.ts`: Cách khai báo Type an toàn sau khi wrap axios.
4. `apps/erp-frontend/src/hooks/useInboundReceipt.ts` và các List: Cách UI component dùng Type đã compile.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Một số API module Catalog (Product, Supplier) trong `ApiService.ts` vẫn đang trả về `any` hoặc xài type custom chưa ép strict type theo `api.d.ts`. Hiện chưa gây bug UI nhưng về lâu dài nên thay thế nốt.

## 6. Việc chưa làm / Out of scope
- Chưa convert toàn bộ 100% `ApiService` sang strict type (chỉ làm các scope Sales, Inbound, Stock, Debt theo report cần thiết của đợt Audit).

## 7. Cách chạy & Cách verify (Reproduce)
- `cd packages/api-contract && npm run generate`
- `cd apps/erp-frontend && npm run build` (hoặc `npx tsc --noEmit`) để thấy 0 error.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Dữ liệu và Types ở Frontend đã an toàn.
- Sprint 9 sẽ tập trung vào vẽ UI: Tạo component hiển thị danh sách công nợ (`DebtList` update) và màn hình hiển thị "Sao kê lịch sử công nợ" gọi API `getMovements` vừa được chuẩn hoá.
