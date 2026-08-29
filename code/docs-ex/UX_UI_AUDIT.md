# UX/UI AUDIT

Tài liệu này ghi nhận hiện trạng các vấn đề UX/UI của ứng dụng `erp-frontend` trước khi bắt đầu dự án Remediation (Phase 1 Baseline).

## Danh sách các vấn đề (Issues)

| Component | File | Current behavior | Problem | Severity | Proposed solution | Phase |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **API & Auth** | `src/api/axiosInstance.ts` | Khi refresh token thất bại, hệ thống tự gọi `window.location.reload()`. | Người dùng bị văng ra trang login đột ngột mà không có cảnh báo hay thông báo phiên hết hạn. | P0 | Sử dụng Notification service để báo lỗi "Phiên đăng nhập hết hạn" rồi mới redirect. | Phase 2 |
| **Global Hooks** | Tất cả file có `useMutation` (VD: `ProductList.tsx`, `CustomerList.tsx`) | Thiếu handler `onError` khi gọi API (tạo/sửa/xoá). | Khi thao tác lỗi (backend báo 400, 500), UI không phản hồi, im lặng hoàn toàn. Thao tác trông như bị kẹt. | P0 | Bổ sung `onError` kết hợp Notification service hoặc dùng Global Error Boundary cho mutations. | Phase 4 |
| **Global Hooks** | Tất cả file có `useQuery` (VD: `ProductList.tsx`, `CustomerList.tsx`) | Chỉ destructure `data` và `isLoading`, bỏ qua `isError` và `error`. | Khi tải danh sách lỗi, bảng dữ liệu rơi vào trạng thái rỗng ("Không có dữ liệu") thay vì báo lỗi kết nối. | P0 | Xây dựng component `DataState` để quản lý `isError`, thay thế Empty state bằng Error state. | Phase 3 |
| **List Views** | `ProductList.tsx`, `CustomerList.tsx`, `DistributorList.tsx` | Nút xoá gọi hàm `window.confirm('Xác nhận xóa?')`. | Làm gián đoạn trải nghiệm người dùng bằng hộp thoại native xấu xí của trình duyệt, không nhất quán. | P2 | Tạo và sử dụng custom component `ConfirmDialog` thay thế native confirm. | Phase 4 |
| **Sales Dashboard** | `src/components/sales/Dashboard.tsx` | Trong hàm `fetchMetrics`, nếu lỗi thì bắt `catch` và chỉ `console.error`. | UI không báo lỗi cho người dùng, các chart không có dữ liệu (Empty) thay vì báo Error State. | P1 | Sử dụng `ErrorState` widget. | Phase 7 |
| **Form Inputs** | `PurchaseOrderForm.tsx`, `SalesOrderForm.tsx` | Nếu người dùng ấn Huỷ, gọi native `confirm('Bạn có chắc chắn muốn thoát?')`. | Workaround bằng native JS block UI. | P2 | Dùng custom Modal để cảnh báo thay đổi chưa được lưu. | Phase 5 |
| **Form Inputs** | `PurchaseOrderForm.tsx`, `SalesOrderForm.tsx` | Khi lưu (save) lỗi, gọi trực tiếp `toast.error(err.message)`. | `err.message` thường là nguyên văn Axios error (VD: Request failed with status code 500), không business-friendly. | P1 | Sử dụng helper `normalizeApiError` trước khi toast lỗi. | Phase 2 |
| **Common/Search** | `SearchModal.tsx`, `SearchableCombobox.tsx` | Tìm kiếm lỗi (fail request) thì `console.error` và hiển thị trống. | Cảm giác không tìm thấy kết quả thay vì lỗi kết nối. | P1 | Thêm Error state cho combobox. | Phase 6 |

## Đánh giá tổng quan
Hệ thống hiện tại đang lạm dụng `console.error` thay cho feedback thực tế tới người dùng, và chưa phân biệt rạch ròi giữa trạng thái Empty (0 record) với trạng thái Error (Failed API). Việc thiếu Error Handling ở các Form (mutation) cũng tạo ra UX "fake success" (nút nhấn không có phản hồi nhưng API thì lỗi).

## Baseline Code Quality
- Đã tồn tại Tailwind CSS, Vite và React Router DOM.
- Tầng Data fetching sử dụng Tanstack React Query nhưng chưa tối ưu lifecycle.
- Cấu trúc thư mục chia theo Feature/Domain (catalog, sales, purchasing) là một điểm tốt.
