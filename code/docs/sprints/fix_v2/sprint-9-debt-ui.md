# Sprint 9: Debt UI
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Xây dựng giao diện "Sao kê công nợ" cho phép người dùng (STAFF/ADMIN) xem chi tiết biến động công nợ (nhập mở đầu, phát sinh mua hàng, trả tiền, hoàn hàng) của từng khách hàng.
- Nâng cấp màn hình `DebtList` để có thể điều hướng (Double click hoặc ấn nút Xem) sang màn hình Sao kê theo Tab Context.

## 2. Thành quả đạt được (Done)
- Tạo mới Component `DebtStatement.tsx`:
  - Component nhận `customerId` và `customerName` qua props.
  - Sử dụng `useQuery` để gọi `ApiService.Debt.getMovements(customerId)`.
  - Hiển thị bảng dữ liệu (Table) với các cột: Thời gian, Loại nghiệp vụ, Chứng từ (Ref ID), Phát sinh (Tăng/Giảm nợ) và Dư nợ.
  - Áp dụng các định dạng hiển thị tiền tệ (VND) và màu sắc nhãn nghiệp vụ (VD: Đỏ cho tăng nợ, Xanh cho giảm nợ).
- Cập nhật Component `DebtList.tsx`:
  - Import và sử dụng Hook `useTabs` để mở Tab mới (hàm `handleViewStatement`).
  - Thêm thuộc tính hover, nút `Sao kê` (icon `Eye`) và sự kiện `onDoubleClick` vào mỗi dòng của danh sách công nợ.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Sử dụng chung kiến trúc Tab (`TabContext`) của hệ thống thay vì mở Modal hay chuyển trang (React Router).
- **Lý do:** Hệ thống ERP này đang được thiết kế giao diện theo dạng Tabs như một Workspace Desktop, giúp người dùng mở nhiều Sao kê công nợ của nhiều khách hàng khác nhau cùng lúc mà không bị mất state của trang Danh sách chính.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `apps/erp-frontend/src/components/crm/DebtList.tsx`: Xem cách xử lý `handleViewStatement` sử dụng `openTab` và truyền param.
2. `apps/erp-frontend/src/components/crm/DebtStatement.tsx`: Xem cách Fetch Data từ API và render giao diện Bảng sao kê, logic format số tiền.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Màn hình `DebtStatement` hiện tại render toàn bộ danh sách Movement (không phân trang). Nếu dữ liệu > 10,000 dòng có thể gây đơ (DOM lag). Sprint tương lai có thể cần implement Pagination hoặc Virtualized List ở frontend khi Backend có hỗ trợ phân trang.

## 6. Việc chưa làm / Out of scope
- Chưa có tính năng filter biến động công nợ theo khoảng thời gian (Date Range) do Backend chưa hỗ trợ.

## 7. Cách chạy & Cách verify (Reproduce)
- Khởi động backend (`mvnw spring-boot:run` profile test hoặc dev).
- Khởi động frontend (`npm run dev`).
- Vào Tab "Công Nợ", click đúp vào một dòng khách hàng bất kỳ. Tab mới mang tên "SAO KÊ: [TÊN KH]" sẽ hiện ra chứa lịch sử biến động.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Giao diện Công nợ cơ bản (Danh sách và Sao kê) đã hoàn tất.
- Sprint 10 (Sales Payment UI) sẽ tập trung vào việc hiện thực hóa nghiệp vụ Thanh toán trước (Advance Payment) ngay tại màn hình Tạo Đơn Hàng (Sales Order Form) để tạo ra dòng dữ liệu "Giảm nợ" thực tế vào Ledger.
