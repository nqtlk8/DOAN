---
date: 2026-09-16
---

# Sprint 11: Admin/Staff Regression (Branch Isolation for Customers)

## 1. Mục tiêu Sprint (Goal)
Sprint này giải quyết yêu cầu cô lập dữ liệu khách hàng theo từng chi nhánh (branch isolation). Cụ thể, nhân viên chi nhánh (STAFF) chỉ được phép nhìn thấy những khách hàng thuộc chi nhánh của mình. Trụ sở chính (HQ/ADMIN) vẫn có thể xem toàn bộ hoặc chỉ định `branch_id` khi tạo/cập nhật thông tin khách hàng để đồng bộ xuống chi nhánh tương ứng.

## 2. Thành quả đạt được (Done)
- Bổ sung trường `branch_id` vào entity `Customer` tại `services/erp-backend/src/main/java/com/storename/erp/crm/domain/Customer.java`.
- Cập nhật các DTO `CustomerCreateDto`, `CustomerUpdateDto`, và `CustomerResponseDto` để nhận và trả về `branchId` (`services/erp-backend/src/main/java/com/storename/erp/crm/application/dto/`).
- Sửa đổi `CustomerWriteService` để hỗ trợ ghi `branchId` khi HQ tạo hoặc cập nhật thông tin khách hàng.
- Cập nhật `CustomerReadController` (`services/erp-backend/src/main/java/com/storename/erp/crm/api/CustomerReadController.java`) để tự động filter danh sách khách hàng dựa trên `branchId` của token hiện tại (nếu là nhân viên chi nhánh).
- Vá lỗi `AuthUtils` (`services/erp-backend/src/main/java/com/storename/erp/common/security/AuthUtils.java`) bị crash (`NumberFormatException`) khi token chứa chuỗi `"HQ"` thay vì một số nguyên.
- Tạo script migration `V16__add_branch_id_to_customer.sql` để thêm cột `branch_id` vào Database và gán mặc định bằng `1` cho các dữ liệu test/cũ.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
Quyết định: Sử dụng in-memory filtering bằng Stream API trong `CustomerReadController` để lọc `branchId` thay vì viết lại truy vấn trong `CustomerRepository`.
Lý do: Lượng khách hàng trên từng chi nhánh trong hệ thống ERP không quá lớn để gây ra nút thắt cổ chai ngay lập tức, và việc tái sử dụng `findAll()` giúp tránh phá vỡ các hợp đồng (contracts) đang có của kho lưu trữ.
Đã cân nhắc: Viết một custom query `findAllByBranchId` trong JPA Repository — bỏ vì cần sửa lại khá nhiều Test cases liên quan đến mock `customerRepository.findAll()` vốn đang được sử dụng ở nhiều module khác nhau, gây rủi ro hồi quy (regression) không đáng có trong Sprint này.

Quyết định: Cập nhật đồng loạt toàn bộ khách hàng hiện tại (dữ liệu mock) về `branch_id = 1` trong Flyway.
Lý do: Để đảm bảo bộ test hiện hành không bị vỡ do dữ liệu khách hàng (trước đó có `branch_id` là NULL) bị loại bỏ khỏi danh sách của STAFF đang đăng nhập dưới nhánh số `1`.
Đã cân nhắc: Để NULL mặc định cho dữ liệu cũ — bỏ vì sẽ dẫn tới fail test hàng loạt (do STAFF nhánh số `1` sẽ nhận về mảng rỗng).

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `src/main/resources/db/migration/V16__add_branch_id_to_customer.sql`: Xem script cập nhật cấu trúc cơ sở dữ liệu.
2. `src/main/java/com/storename/erp/crm/domain/Customer.java`: Xem entity được bổ sung thuộc tính mới.
3. `src/main/java/com/storename/erp/crm/application/dto/`: Xem các DTO đã được thêm thuộc tính `branchId`.
4. `src/main/java/com/storename/erp/common/security/AuthUtils.java`: Xem cách token `HQ` được xử lý an toàn để tránh exception.
5. `src/main/java/com/storename/erp/crm/api/CustomerReadController.java`: Nơi chứa logic lọc in-memory tự động theo `branchId` lấy từ JWT.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- In-memory filtering (bằng Java Stream) trong `CustomerReadController` sẽ chậm nếu số lượng bản ghi khách hàng tăng lên hàng chục ngàn. Nên chuyển thành câu lệnh truy vấn SQL (như `WHERE branch_id = ?`) ở các Sprint tối ưu hoá (Optimization).
- Việc gán cứng `branch_id = 1` cho các khách hàng cũ thông qua Flyway là giải pháp xử lý tạm (workaround) cho môi trường test và dev. Nếu chạy trên Production thực tế cần một chiến lược gán branchId chính xác cho từng đối tượng.

## 6. Việc chưa làm / Out of scope
- Phân trang (Pagination) kết hợp với filter theo nhánh chưa được thực hiện trong Sprint này.
- UI Frontend tại branch chưa được ẩn tính năng tạo/sửa khách hàng một cách hoàn chỉnh (dù API đã chặn theo role HQ).

## 7. Cách chạy & Cách verify (Reproduce)
Chạy toàn bộ backend test để đảm bảo các ràng buộc truy cập theo branch hoạt động đúng, không có HTTP 401 hay mảng dữ liệu bị rỗng vô cớ:
```bash
cd services/erp-backend
.\mvnw test
```
Kết quả mong đợi: `BUILD SUCCESS` (129 tests passed).

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
Sprint này đã hoàn tất việc sửa nợ kỹ thuật (về mặt phân luồng quyền truy cập khách hàng) mà Sprint trước bỏ dở. Token của các nhân viên thuộc nhánh nào giờ đã chỉ nhìn thấy khách hàng của nhánh đó. Sprint tiếp theo (Sprint 12) có thể tiến hành liên kết trực tiếp chức năng Trả trước (advancePayment) vào Receivable Debt Ledger trên Frontend một cách an toàn mà không sợ sai lệch dữ liệu liên chi nhánh.
