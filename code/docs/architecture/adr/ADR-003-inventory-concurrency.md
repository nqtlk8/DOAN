# ADR-003: Optimistic Locking cho Inventory Concurrency

**Ngày quyết định:** 2026-08-20
**Trạng thái:** Accepted
**Ngữ cảnh:** Sprint 2.1 - Inventory

## 1. Bối cảnh
Khi thiết kế module tồn kho cho hệ thống ERP ngành VLXD & TTNT (N-instance architecture), một trong những bài toán phức tạp nhất là xử lý đồng thời (concurrency) khi nhiều người dùng/kênh cùng cố gắng xuất kho một mã sản phẩm tại cùng một chi nhánh (VD: N=20 request đồng thời).
Cần có cơ chế khóa (locking) hiệu quả để đảm bảo tồn kho không bao giờ bị âm (`quantity >= 0`).

## 2. Các phương án cân nhắc

### Phương án A: Pessimistic Locking (`SELECT FOR UPDATE`)
- **Nguyên lý:** Khóa row ngay tại DB cho đến khi transaction kết thúc.
- **Nhược điểm:** Dễ gây deadlock khi một đơn hàng xuất kho cùng lúc nhiều mã sản phẩm (nếu các giao dịch khóa các row theo thứ tự khác nhau). Hiệu năng ghi giảm mạnh.

### Phương án B: Optimistic Locking (`@Version`) + Retry logic
- **Nguyên lý:** Đọc số lượng hiện tại kèm `version`. Khi update, câu lệnh SQL sẽ có thêm điều kiện `WHERE version = ?`. Nếu row đã bị giao dịch khác thay đổi (version không khớp), DB sẽ không cập nhật row nào, JPA ném ra `OptimisticLockException`. Ứng dụng catch exception này và retry lại từ đầu.

## 3. Quyết định
Chọn **Phương án B: Optimistic Locking + Service-level Retry**.

## 4. Lý do
- **Đặc thù nghiệp vụ:** Tại một chi nhánh VLXD (Branch instance), lượng nhân viên thao tác đồng thời trên cùng một mã sản phẩm là có nhưng không quá cao (tỷ lệ conflict thực tế thấp). Optimistic locking hoạt động cực kỳ nhẹ nhàng cho phần lớn các trường hợp không conflict.
- **Tránh Deadlock:** Loại bỏ hoàn toàn rủi ro deadlock ở DB khi bán các đơn hàng có danh sách sản phẩm dài.
- **Bảo vệ 3 lớp (Defense in depth):**
  1. **Domain Logic:** `StockOnHand.decrease()` kiểm tra điều kiện invariant (throws `StockInsufficientException` nếu qty < requested).
  2. **JPA Layer:** Cơ chế `@Version` kế thừa từ `BaseEntity` chặn ghi đè version cũ.
  3. **Database Layer:** Schema database (V3 Flyway) được bổ sung constraint cứng `CHECK (quantity >= 0)` làm chốt chặn cuối cùng bảo vệ tính toàn vẹn dữ liệu.
- **Retry Mechanism:** Service layer được thiết kế để catch `ObjectOptimisticLockingFailureException` và retry tối đa 3 lần với thời gian backoff 50ms, giúp hệ thống tự phục hồi mà không quăng lỗi ra phía người dùng cuối trong các ca conflict nhỏ.
