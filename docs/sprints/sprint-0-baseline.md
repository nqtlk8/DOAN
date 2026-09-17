# Sprint 0: Baseline & Contract Freeze
Ngày hoàn thành: 2026-09-16

## 1. Mục tiêu Sprint (Goal)
- Audit hiện trạng các contract giữa Frontend và Backend. Xác định chính xác các mismatch đang tồn tại (Debt, Stock, Sales, Inbound) và ghi nhận vào ma trận kiểm tra.
- Không sửa code business hay UI, chỉ đóng băng hợp đồng (contract freeze) để làm cơ sở (baseline) cho các sprint tiếp theo.

## 2. Thành quả đạt được (Done)
- Tạo thành công tài liệu ma trận contract: [frontend-backend-contract-matrix.md](../../docs/audit/frontend-backend-contract-matrix.md).
- Đã rà soát chi tiết response/request của Debt, Stock, Sales, Inbound, Supplier, Product.
- Đã rà soát flow tạo SalesInvoice, xác nhận field `advancePayment` hoàn toàn vắng mặt ở payload, entity và logic tính debt.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- Quyết định: Cập nhật lại toàn bộ DTO ở Backend thay vì chỉnh sửa linh tinh ở Frontend.
- Lý do: Đảm bảo Backend DTO là source of truth duy nhất, giúp sinh OpenAPI chính xác và Frontend sử dụng `openapi-typescript` để loại bỏ `any`.
- Đã cân nhắc: Thêm `fallback` hiển thị trên UI (ví dụ `orderId || id`). Đã bỏ vì không giải quyết triệt để vấn đề mất tính strongly-typed ở API boundary.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- `docs/audit/frontend-backend-contract-matrix.md`: Xem ma trận lỗi mismatch hiện tại.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- File `openapi.json` hiện đang stale (chưa update ApiResponse wrapper). Cần fix sớm ở Sprint 2 để tránh codegen sai.
- `DashboardApi.ts` là code rác (duplicate), cần xóa ở Sprint 2.

## 6. Việc chưa làm / Out of scope
- Chưa sửa bất kỳ code nào trong source, đúng như yêu cầu của Sprint 0 (chỉ Audit và Freeze).

## 7. Cách chạy & Cách verify (Reproduce)
- Đọc file `frontend-backend-contract-matrix.md` để thấy rõ bằng chứng mismatch lấy từ codebase. (Ví dụ `ReceivableDebt` không có `partnerName`).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 1 sẽ bắt đầu bằng việc thiết kế lại các Backend Response DTO (Debt, Stock, Sales) theo chuẩn được mô tả trong Matrix.

