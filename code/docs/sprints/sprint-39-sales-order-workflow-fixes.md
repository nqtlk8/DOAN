# Sprint 39: Sửa các lỗi trong Workflow Tạo Đơn Bán Hàng

**Ngày hoàn thành**: 2026-09-17

### 1. Mục tiêu Sprint
Sprint này giải quyết các lỗi nghiêm trọng trong workflow tạo đơn bán hàng, đặc biệt là lỗi 404 khi nhân viên chi nhánh truy cập sai cổng, lỗi thiếu thông tin giá sản phẩm, lỗi không tìm thấy khách hàng dùng chung, và lỗi logic về hiển thị chiết khấu/thuế.

### 2. Thành quả đạt được
- **Sửa lỗi 404 cho STAFF**: Cấu hình `internal_url` cho các chi nhánh trong DB (qua migration `V20__branch_portal_url_and_shared_customers.sql`). Cập nhật `AuthContext.tsx` để bắt buộc nhân viên chi nhánh phải dùng đúng cổng của chi nhánh, và từ chối đăng nhập nếu sai cổng.
- **Sửa lỗi sản phẩm không có giá**: Cập nhật `ApiService.ts` (API `searchProducts`) và `SalesOrderForm.tsx` để lấy giá sản phẩm theo chi nhánh (`withBranchPrice=true`).
- **Sửa lỗi khách hàng dùng chung**: Migration `V20` cấu hình lại `branch_id = NULL` cho các khách hàng dùng chung. Cập nhật `CustomerRepository.java` để cho phép `branchId` là NULL. Thêm dropdown chọn chi nhánh trong form thêm khách hàng (`CustomerList.tsx`).
- **Thêm người tạo cho stock movement**: Cập nhật `SalesInvoiceService.java` để truyền `userId` vào `recordSaleAndGetCost`.
- **Đăng xuất khi token không hợp lệ**: Cập nhật `AuthContext.tsx` và `axiosInstance.ts` để xóa session nếu payload token không chứa `sub` là UUID hoặc API trả về 403 do user identity missing.
- **Xử lý hiển thị chiết khấu/thuế**: Quyết định vô hiệu hóa ô chiết khấu/thuế trên frontend để đồng bộ với backend chưa hỗ trợ, tránh sai lệch công nợ.

### 3. Quyết định kiến trúc & Lý do
- **Quyết định**: Vô hiệu hóa (disable) tính năng chiết khấu và thuế ở Frontend cho đơn bán hàng (Cách làm tạm thời 7a).
  - **Lý do**: Backend hiện chưa có trường `discount` và `tax` cho `SalesInvoice`. Việc Frontend gửi không có 2 trường này nhưng UI lại hiển thị và tính toán dựa trên chúng sẽ dẫn đến sai lệch lớn về mặt hiển thị so với dữ liệu công nợ lưu thực tế.
  - **Đã cân nhắc**: Nâng cấp Backend thêm 2 trường này (Cách 7b) — bỏ vì yêu cầu sửa đổi lớn ở DB, DTO và logic tính toán, nằm ngoài thời gian của đợt fix nhanh này.

### 4. Hướng dẫn đọc code theo thứ tự
1. `db/migration/V20__branch_portal_url_and_shared_customers.sql`: Migration cập nhật URL chi nhánh và reset `branch_id` cho khách hàng dùng chung.
2. `apps/erp-frontend/src/context/AuthContext.tsx`: Logic xác thực cổng cho STAFF và kiểm tra UUID trong JWT.
3. `apps/erp-frontend/src/api/ApiService.ts` & `apps/erp-frontend/src/components/sales/SalesOrderForm.tsx`: Fix API lấy giá sản phẩm và cập nhật logic UI form bán hàng.
4. `services/erp-backend/src/main/java/com/storename/erp/crm/infrastructure/CustomerRepository.java`: Query lấy khách hàng dùng chung.
5. `services/erp-backend/src/main/java/com/storename/erp/order/application/SalesInvoiceService.java`: Logic truyền `userId` cho `stock_movement` và check sớm sự tồn tại của khách hàng.

### 5. Rủi ro / Nợ kỹ thuật đã biết
- **Nợ kỹ thuật**: Tính năng chiết khấu và thuế chưa được hỗ trợ ở backend, cần bổ sung trong tương lai để nghiệp vụ ERP hoàn chỉnh.
- **Rủi ro**: Việc khách hàng dùng chung dựa vào `branch_id IS NULL` có thể gây chậm DB nếu số lượng khách hàng quá lớn và index chưa cover trường hợp NULL hợp lý.

### 6. Việc chưa làm / Out of scope
- Nâng cấp tính toán chiết khấu và thuế (Discount/Tax) vào backend cho đơn bán hàng.
- Nâng cấp UI quản lý khách hàng dùng chung sâu hơn ngoài form thêm cơ bản.

### 7. Cách chạy & Cách verify
- **Backend**: Chạy `mvn test` trong thư mục `services/erp-backend` để đảm bảo các test của `SalesInvoiceService` pass.
- **Frontend**: Chạy `npx vitest run` và `npx playwright test tests/login.spec.ts` trong `apps/erp-frontend`.
- **E2E/Thực tế**:
  1. Đăng nhập STAFF tại `http://localhost` -> sẽ bị chặn. Đăng nhập tại `http://localhost:81` thành công.
  2. Tạo đơn bán hàng: Tìm khách hàng sẽ thấy khách dùng chung (VD: KH-001). Tìm sản phẩm sẽ có giá.
  3. Bấm Lưu đơn: Đơn được tạo và confirm luôn, không bị lỗi 400.

### 8. Điểm nối cho Sprint tiếp theo
Sprint sau có thể bắt đầu với việc implement đầy đủ chiết khấu và thuế (Discount/Tax) từ Backend đến Frontend để tính năng tạo đơn bán hàng hoàn chỉnh về mặt tài chính. Đồng thời tiếp tục rà soát các vấn đề sync dữ liệu qua replication.
