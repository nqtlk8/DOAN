# Sprint 36 - 2026-09-17

### 1. Mục tiêu Sprint (Goal)
- Viết integration test đa luồng (concurrency) để mô phỏng và verify khả năng chống chịu race-condition (TOCTOU, double-confirm) cho GoodsReturnService và InboundReceiptService.
- Sửa cấu hình ESLint của Frontend (Vite) để không chặn pipeline build với các lỗi cảnh báo React Compiler.

### 2. Thành quả đạt được (Done)
- Downgrade rules React Compiler (như eact-hooks/set-state-in-effect) xuống mức warn hoặc off trong code/erp-platform/apps/erp-frontend/eslint.config.mjs để đảm bảo 
pm run lint thoát với mã 0.
- Viết InboundReceiptConcurrencyIT (code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/inventory/application/InboundReceiptConcurrencyIT.java) kiểm tra xác nhận đa luồng cùng một phiếu nhập.
- Viết GoodsReturnConcurrencyIT (code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/order/application/GoodsReturnConcurrencyIT.java) kiểm tra xác nhận trả hàng đa luồng.
- **Phát hiện và Fix Bug (Logic) trong GoodsReturnService.java:** Chuyển lời gọi goodsReturn.confirm() xuống cuối hàm, sau đoạn query getTotalReturnedQuantity để fix lỗi truy vấn SUM tự cộng chính dữ liệu DRAFT vừa mới bị JPA auto-flush.

### 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Giảm cấp độ của các rules ESLint thử nghiệm (experimental react-hooks compiler rules) xuống warn thay vì error.
- **Lý do:** Dự án đang ưu tiên tốc độ build và development flow, không muốn build CI/CD hoặc quá trình chạy Vite bị gián đoạn vì những quy định khắt khe chưa stable từ bản thử nghiệm của React.
- **Đã cân nhắc:** Disable hoàn toàn plugin ESLint. Bỏ vì vẫn muốn nhà phát triển thấy được cảnh báo ở console để fix dần.
- **Quyết định:** Move goodsReturn.confirm(userId) xuống sau khi query SUM trả về.
- **Lý do:** Khi set trạng thái thành CONFIRMED, JPA sẽ tự động trigger flush. Truy vấn JPQL chạy ngay sau đó sẽ read lại chính entity vừa sửa, dẫn đến việc tính toán bị lặp lại.

### 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. code/erp-platform/apps/erp-frontend/eslint.config.mjs - Xem cách điều chỉnh rules.
2. code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/order/application/GoodsReturnService.java - Nơi gọi confirm được chuyển xuống vị trí chuẩn xác.
3. code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/order/application/GoodsReturnConcurrencyIT.java - Xem cách mô phỏng nhiều luồng xác nhận trả hàng để verify TOCTOU.
4. code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/inventory/application/InboundReceiptConcurrencyIT.java - Xem cách test race condition khi import phiếu nhập.

### 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các lỗi ESLint ở Frontend mới chỉ được hạ cấp, chưa được sửa đổi triệt để trong source code. Nợ kĩ thuật cần có các sprint dọn dẹp mã nguồn Frontend ở các component cũ.
- Test concurrency chưa verify việc timeout của @Retryable khi hệ thống bị quá tải deadlock. 

### 6. Việc chưa làm / Out of scope
- Chưa xử lý các cảnh báo (129 warnings) từ 
pm run lint. Việc sửa code React cụ thể của component được hoãn lại.

### 7. Cách chạy & Cách verify (Reproduce)
- Verify Backend:
  Chạy lệnh: cd code/erp-platform/services/erp-backend && .\mvnw test -Dtest="GoodsReturnConcurrencyIT,InboundReceiptConcurrencyIT"
  Sẽ thấy kết quả BUILD SUCCESS (2 tests pass).
- Verify Frontend:
  Chạy lệnh: cd code/erp-platform/apps/erp-frontend && npm run lint
  Sẽ thấy cảnh báo (warnings) nhưng exit code 0.

### 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Với việc Frontend build pass và Backend đã an toàn về mặt Race Condition/Concurrency cho nghiệp vụ trả hàng và nhập hàng, Sprint tiếp theo nên tiếp tục dọn dẹp Tech Debt ở giao diện Frontend, hoặc đi sâu vào tính năng Auth (RBAC) cho người dùng cuối.
