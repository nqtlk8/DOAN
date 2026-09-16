# Báo cáo Sprint 3 - JWT Branch Isolation
Ngày hoàn thành: 2026-09-16

## 1. Mục tiêu Sprint
Thiết lập cơ chế "cách ly nhánh cứng" ngay tại lớp mạng thay vì phụ thuộc vào Controller. Đảm bảo nhân viên chi nhánh không thể truy cập API trên instance của chi nhánh khác hoặc trên HQ, ngay cả khi họ có token hợp lệ từ HQ.

## 2. Thành quả đạt được
- Thêm biến môi trường `branch-id` vào cấu hình (`application-branch.yml`).
- Cập nhật `JwtAuthenticationFilter` để so sánh `branchId` trong Token với `branch-id` đang chạy. Nếu role của instance là BRANCH mà ID không khớp, từ chối request (HTTP 403).
- Deprecated `@BranchScoped` và `BranchScopedAspect` vì cơ chế filter đã bao phủ toàn cục và an toàn hơn.

## 3. Quyết định kiến trúc & Lý do
- Quyết định: Dùng `JwtAuthenticationFilter` để chặn ngay ở tầng bảo mật thay vì dùng AOP (`@BranchScoped`) trên từng API.
- Lý do: Đảm bảo nguyên tắc "Fail Fast". Lỗi phân quyền bị chặn trước khi chạm vào bất kỳ logic nghiệp vụ nào, và không lo lập trình viên quên gắn annotation `@BranchScoped` cho API mới.
- Đã cân nhắc: Sửa đổi Aspect `@BranchScoped` — bỏ vì AOP vẫn cho request đi vào filter chain khá sâu.

## 4. Hướng dẫn đọc code theo thứ tự
1. `JwtAuthenticationFilter.java`: Thêm check cứng `instance.role` và `branch-id`.
2. `JwtAuthenticationFilterTest.java`: Thử nghiệm chi tiết các luồng cách ly.
3. `ArchitectureV3BranchTest.java`: Sửa cấu hình cho test để phản ánh thuộc tính `branch-id`.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- `BranchScopedAspect` chỉ đang đánh `@Deprecated` nhưng vẫn hoạt động. Nếu phương thức còn annotation này ném `SecurityException`, nó sẽ được bắt bởi handler và trả 403. Nhưng về lâu dài nên xóa hẳn.

## 6. Việc chưa làm / Out of scope
- Chưa xóa vật lý file `BranchScoped` và các annotation ở controller.

## 7. Cách chạy & Cách verify
- Chạy: `mvn test -Dtest=JwtAuthenticationFilterTest`
- Kết quả mong đợi: Filter từ chối và trả về 403 Forbidden nếu branch ID không khớp.

## 8. Điểm nối cho Sprint tiếp theo
Sẽ cần xem xét logic lấy dữ liệu (như ProductReader) khi người dùng là Admin (có branchId = null) vì logic cũ đang cố chặn hoặc ném lỗi nếu thiếu branch ID.
