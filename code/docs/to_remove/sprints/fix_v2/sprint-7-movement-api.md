# Sprint 7: Movement API
**Ngày hoàn thành:** 16/09/2026

## 1. Mục tiêu Sprint (Goal)
- Cung cấp API để lấy danh sách lịch sử công nợ (Movement) của một khách hàng, từ đó Frontend có thể xây dựng giao diện "Bảng sao kê công nợ" chi tiết.
- Đảm bảo dữ liệu trả về được sắp xếp theo thời gian mới nhất (descending) nhằm phục vụ hiển thị chính xác dòng tiền ra/vào và số dư.

## 2. Thành quả đạt được (Done)
- Tạo DTO `ReceivableDebtMovementResponseDto` phản chiếu các trường dữ liệu cần thiết từ `ReceivableDebtMovement`.
- Cập nhật Repository `ReceivableDebtMovementRepository` bằng cách thêm hàm `findByCustomerIdAndBranchIdOrderByCreatedAtDesc`.
- Mở rộng logic `ReceivableDebtService` với phương thức `getMovements(customerId, branchId)` để fetch data một cách an toàn.
- Cung cấp HTTP Endpoint `GET /api/v1/receivable-debts/{customerId}/movements` bên trong `ReceivableDebtController`.
- Pass 100% (129/129) Integration/Unit Tests bao gồm cả phần config MockBean.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
- **Quyết định:** Trả về `List<ReceivableDebtMovementResponseDto>` không phân trang ở thời điểm hiện tại thay vì `Page`.
- **Lý do:** Branch module hiện thiết kế lượng giao dịch công nợ trên mỗi khách hàng ở mức vừa (khách hàng tại một chi nhánh thường có chu kỳ đối soát công nợ theo tháng, số lượng dòng phát sinh không quá 10,000 dòng). Trả về List giúp Frontend dễ dàng render biểu đồ hoặc filter theo Date Range (nếu sau này mở rộng thêm param). Ở tương lai gần có thể upgrade lên Pagination khi có requirement cụ thể.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `ReceivableDebtMovementResponseDto.java`: Cấu trúc dữ liệu API trả về.
2. `ReceivableDebtMovementRepository.java`: Custom query list và order by Date.
3. `ReceivableDebtService.java`: Hàm `getMovements` gọi DB.
4. `ReceivableDebtController.java`: Nơi thiết lập HTTP Route.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Khả năng payload bị lớn dần theo thời gian nếu một khách hàng có tuổi đời giao dịch dài. Có thể dẫn đến memory overhead ở Spring và timeout. Nên bổ sung optional parameters `?startDate=&endDate=` ở các Sprint sau nếu quy mô scale lên.

## 6. Việc chưa làm / Out of scope
- Phân trang (Pagination) chưa được setup. 
- API filter nâng cao (theo MovementType: chỉ lấy PAYMENT hoặc chỉ lấy INVOICE) hiện chưa có.

## 7. Cách chạy & Cách verify (Reproduce)
- Chạy `.\mvnw test`.
- Dùng Postman `GET /api/v1/receivable-debts/{customerId}/movements` với Auth Token chuẩn. Mong đợi nhận về JSON array thuộc `data`.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Hoàn thành toàn bộ Backend Contract (gồm cả Logic Ledger và API list).
- Sprint tiếp theo (Sprint 8: Frontend contract) sẽ chuyển trọng tâm sang **erp-frontend**, cập nhật các Service File, Type file và OpenAPI Client để đồng bộ hoàn toàn với cấu trúc backend hiện tại (các DTO mới, API mới).
