# Báo cáo Sprint 4 - Fix Admin Access to Catalog
Ngày hoàn thành: 2026-09-16

## 1. Mục tiêu Sprint
Khắc phục lỗi "Silent 500" hoặc chặn truy cập khi Admin (tài khoản không gán chi nhánh) cố gắng lấy danh sách sản phẩm. Các tài khoản Admin không có `branchId` hợp lệ, dẫn đến `NumberFormatException` hoặc lỗi phân quyền trong `ProductReader`.

## 2. Thành quả đạt được
- Cập nhật `ProductReader.java` để xử lý an toàn trường hợp người dùng không thuộc chi nhánh nào (`branchId` null).
- Khi `branchId == null` (ví dụ: Admin HQ), logic tính giá riêng cho chi nhánh bị bỏ qua (fallback về giá gốc).

## 3. Quyết định kiến trúc & Lý do
- Quyết định: Cập nhật hàm `getCurrentBranchId` trong `ProductReader` để trả về `null` thay vì ném exception.
- Lý do: Tài khoản Admin ở HQ quản lý Master Data, không nhất thiết phải có `branchId`. Logic đọc bảng Product có thể trả về giá cơ bản nếu không cung cấp chi nhánh.
- Đã cân nhắc: Ép Admin phải có `branchId` giả — bỏ vì vi phạm ngữ nghĩa dữ liệu HQ.

## 4. Hướng dẫn đọc code theo thứ tự
1. `ProductReader.java`: Xem phương thức `getCurrentBranchId()` và `mapToResponseWithPrice()`.
2. `ProductReaderTest.java`: Xác nhận Admin (null branchId) có thể lấy dữ liệu sản phẩm bình thường.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Logic lấy thông tin nhánh đang được thực hiện cục bộ bằng cách parse từ `JwtAuthDetails`. Việc này hơi lặp code nếu sau này nhiều class khác cũng cần lấy `branchId`.

## 6. Việc chưa làm / Out of scope
- Chưa tạo Context object toàn cục để lấy `branchId` (ví dụ `SecurityContextHelper.getCurrentBranchId()`).

## 7. Cách chạy & Cách verify
- Chạy unit test: `mvn test -Dtest=ProductReaderTest`
- Kết quả mong đợi: `testGetAllProductsWithBranchPrice_AdminRole_ShouldNotThrowException` trả về thành công.

## 8. Điểm nối cho Sprint tiếp theo
Sẽ tiếp tục xử lý các nghiệp vụ Analytics (Dashboard) khi Admin (HQ) cần tổng hợp dữ liệu trên toàn hệ thống (không lọc theo branch cụ thể).
