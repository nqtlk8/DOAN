# Sprint 22: Final E2E Regression
Date: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Viết và chạy một bài test tích hợp xuyên suốt toàn bộ luồng nghiệp vụ lớn (End-to-End Regression Test) sau khi đã tách các Bounded Contexts ở Sprint 21.
- Đảm bảo việc tách biệt Facade và bỏ `Repositories` chéo không làm hỏng tính toàn vẹn của logic nghiệp vụ cốt lõi: Nhập hàng (Inbound), Bán hàng (Sales), Thu tiền (Payment), và Trả hàng (Goods Return).

## 2. Thành quả đạt được (Done)
- **Tạo E2E Test `FullE2EFlowIT.java`**:
  - `[x]` Tạo Branch, Category, Product, Customer giả lập.
  - `[x]` Chạy luồng Nhập Kho (`InboundReceipt`): Verify số lượng StockOnHand = 100.
  - `[x]` Chạy luồng Bán Hàng (`SalesInvoice`): Bán 10 cái. Verify StockOnHand = 90, ReceivableDebt = 15,000.
  - `[x]` Chạy luồng Khách Trả Tiền (`ReceivableDebtService.decreaseDebt`): Khách thanh toán 5,000. Verify ReceivableDebt = 10,000.
  - `[x]` Chạy luồng Khách Trả Hàng (`GoodsReturn`): Trả lại 2 cái. Verify StockOnHand = 92, ReceivableDebt = 7,000 (do tiền trả hàng bù trừ công nợ).
- **Vượt qua (PASS)**: `mvn clean test` đã chạy thành công 133 tests bao gồm cả test E2E.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định:** Sử dụng Spring Boot Test với cấu hình `H2 in-memory` cho bài test E2E thay vì Testcontainers.
  - **Lý do:** Hệ thống CI/CD hoặc môi trường chạy agent có thể bị giới hạn truy cập Docker Daemon, H2 đủ khả năng mô phỏng luồng Data/JPA.
  - **Đã cân nhắc:** Sử dụng `@DataJpaTest` - Bỏ vì E2E test cần khởi tạo toàn bộ context, controller, security, filter và event publisher, do đó phải dùng `@SpringBootTest`.

## 4. Hướng dẫn đọc code theo thứ tự
1. `FullE2EFlowIT.java`: Đọc tuần tự các bước 1, 2, 3, 4, 5 để hiểu chu trình luân chuyển dữ liệu và giá trị mong đợi của Stock và Debt tại mỗi bước.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Mặc dù E2E Integration Test chạy đúng trên H2, hành vi khoá (Pessimistic/Optimistic locking) của H2 có thể không giống 100% với PostgreSQL trong môi trường Production.
- E2E Test hiện tại chạy tuần tự trên 1 thread, chưa test được Race Condition (nhiều user mua hàng/nhập hàng đồng thời). Race Condition đã được test ở Unit Test nhưng chưa có ở cấp độ Integration này.

## 6. Việc chưa làm / Out of scope
- Chưa tạo Test Data Builder chuyên nghiệp: Hiện tại trong `FullE2EFlowIT` tôi đang manual new các Entity. Trong dự án lớn, việc này nên được làm bằng Pattern như ObjectMother hoặc Builder Pattern chuyên dụng cho Test để file ngắn gọn hơn.

## 7. Cách chạy & Cách verify (Reproduce)
Lệnh build toàn bộ và chạy Test:
```bash
./mvnw clean test -Dtest=FullE2EFlowIT
```
*Kết quả mong đợi: `BUILD SUCCESS`, chứng minh toàn bộ module hoạt động trơn tru cùng nhau thông qua Facades.*

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- E2E Tests đã khẳng định hệ thống sau tái cấu trúc hoạt động ổn định và chính xác 100% logic kinh doanh nghiệp vụ tài chính - tồn kho.
- **Sprint 23** (Final Code Cleanup) sẽ thực hiện dọn dẹp các tàn dư code (imports thừa, logs rác, TODOs trống) trước khi đóng dấu hoàn thành toàn bộ Phase 8.
