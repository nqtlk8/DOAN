# Báo cáo Sprint 2 - Auth Exception Standardization
Ngày hoàn thành: 2026-09-16

## 1. Mục tiêu Sprint
Chuẩn hóa cách xử lý ngoại lệ trong luồng Authentication. Trước đây hệ thống trả về HTTP 500 do catch `RuntimeException` chung chung, làm lộ stacktrace và khó kiểm thử. Mục tiêu là dùng domain-specific exceptions và xử lý tập trung.

## 2. Thành quả đạt được
- Tạo các exception domain cụ thể (`NoRoleAssignedException`, `SigningKeyNotConfiguredException`) trong layer identity.
- Cập nhật `AuthService` ném ra các exception cụ thể thay vì ném `RuntimeException`.
- Cập nhật `GlobalExceptionHandler` bắt và trả về HTTP 401 kèm format JSON chuẩn thay vì 500 cho các lỗi này.

## 3. Quyết định kiến trúc & Lý do
- Quyết định: Di chuyển việc map HTTP Status Code sang `GlobalExceptionHandler`, không để rải rác trong `AuthController`.
- Lý do: Tách bạch Business Logic (AuthService ném Exception) và Presentation Logic (ControllerAdvice map HTTP).
- Đã cân nhắc: Bắt exception trực tiếp bằng `try/catch` trong Controller — bỏ vì sẽ duplicate logic nếu có luồng auth khác sau này.

## 4. Hướng dẫn đọc code theo thứ tự
1. `AuthService.java`: Ném các custom exception.
2. `GlobalExceptionHandler.java`: Bắt `NoRoleAssignedException`, `JwtException` và trả HTTP 401.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Hiện tại một số lỗi cấu hình key RSA ném `SigningKeyNotConfiguredException` bị map thành 500 (bắt bởi `Exception.class`). Việc này tạm ổn vì lỗi thiếu key là lỗi cấu hình server, không phải lỗi từ Client.

## 6. Việc chưa làm / Out of scope
- Chưa tích hợp hệ thống log phân tán.

## 7. Cách chạy & Cách verify
- Chạy unit test: `mvn test -Dtest=AuthServiceTest`
- Chạy integration test: `mvn test -Dtest=AuthIntegrationTest`

## 8. Điểm nối cho Sprint tiếp theo
Luồng auth đã sạch lỗi exception. Bước tiếp theo là cập nhật luồng xác thực nhánh (Branch isolation) vì branch id vẫn đang so sánh bằng chuỗi cứng trong controller.
