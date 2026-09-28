# Ngày: 2026-09-17
# Sprint 16: Idempotency & Concurrency Integration Test

## 1. Mục tiêu Sprint (Goal)
- Viết integration test chịu tải để xác nhận tính an toàn của hệ thống ERP khi có nhiều luồng (threads) cùng xác nhận (confirm) một chứng từ tài chính / kho.
- Đảm bảo Optimistic Locking (`@Version`), Retry mechanism và ràng buộc Unique Index phối hợp đúng đắn để không bị duplicate giao dịch, không bị âm kho trái phép, và không bị lệch công nợ.

## 2. Thành quả đạt được (Done)
- ✅ Đã tạo `IdempotencyConcurrencyIT.java` test trực tiếp `SalesInvoiceService.confirmInvoice` dưới môi trường multi-threading.
- ✅ Giả lập 5 threads đồng thời gọi `confirmInvoice` cho cùng 1 đơn hàng trạng thái DRAFT.
- ✅ Xác nhận chính xác 1 thread thành công và ghi sổ (Ledger / Stock), 4 threads còn lại bị từ chối với lỗi hợp lệ (`IllegalStateException`).
- ✅ Xác nhận không có duplicate data sinh ra trong các bảng `stock_movement` và `receivable_debt_movement`.

**Các file chính đã thay đổi:**
- [NEW] `services/erp-backend/src/test/java/com/storename/erp/order/application/IdempotencyConcurrencyIT.java`

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định:** Sử dụng Integration Test (`@SpringBootTest`, `@ActiveProfiles("test")`) kết hợp `CountDownLatch` và `ExecutorService` thay vì Unit Test.
- **Lý do:** Unit test với Mockito không thể tái hiện được Optimistic Locking của JPA/Hibernate cũng như các lỗi Unique Constraint của Database (`DataIntegrityViolationException`). Việc test đa luồng bắt buộc phải chạm đến Database thật (H2 in-memory) để đảm bảo Transaction Management và `@Retryable` interceptor hoạt động đúng như thực tế.
- **Đã cân nhắc:** Sử dụng Jmeter hoặc API testing tool ở vòng ngoài — bỏ vì setup nặng nề trong CI/CD, chạy chậm, khó can thiệp vào state nội bộ của database để assert chính xác số lượng movement.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. Đọc `IdempotencyConcurrencyIT.java`: Chú ý cách dùng `CountDownLatch` để 5 threads cùng xuất phát 1 lúc. Phân tích phần `catch (IllegalStateException e)` để thấy cách 4 threads bị văng lỗi.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các bài test concurrency chạy multi-thread có thể gây flaky (đôi khi pass/đôi khi fail) trên các máy chủ CI quá yếu, do H2 Database xử lý lock table thay vì row-level lock như Postgres. Cần theo dõi khi đẩy lên Github Actions.
- Mới chỉ cover cho `SalesInvoiceService`. Cần nhân rộng pattern test này cho `InboundReceiptService` và `PaymentService` ở các sprint sau.

## 6. Việc chưa làm / Out of scope
- Chưa mở rộng test concurrency cho các màn hình Receipt, Payment hay Return. Việc này có thể thực hiện sau khi pattern test đã được review.
- Chưa tạo kịch bản test stress API (Load test) vì nằm ngoài scope của Integration Test cục bộ.

## 7. Cách chạy & Cách verify (Reproduce)
Chạy lệnh Maven sau tại thư mục `services/erp-backend`:
```bash
./mvnw test -Dtest=IdempotencyConcurrencyIT
```
**Kết quả mong đợi:** 
Log sẽ hiển thị nhiều Exception do Optimistic Lock / DataIntegrityViolation nhưng quá trình test cuối cùng phải báo `BUILD SUCCESS` (vì các exception đã được catch và assert đúng số lượng).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Core Financial đã hoàn thiện và chứng minh được khả năng chịu tải chống duplicate transaction.
- Sprint tiếp theo (Phase 8) có thể tiếp tục triển khai các chỉnh sửa giao diện Frontend (Hiển thị Phone/Address ở Debt List, Hiển thị Reference Code thay vì UUID) hoặc sửa lại semantic của Payment/Invoice movement như đã báo cáo trong audit.
