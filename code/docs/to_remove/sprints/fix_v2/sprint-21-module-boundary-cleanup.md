# Sprint 21: Module Boundary Cleanup
Date: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Dọn dẹp các ranh giới kiến trúc (Module Boundaries) giữa các domain như `crm`, `catalog`, `order`, `branch`, `inventory`.
- Cấm các module inject trực tiếp các Repository của module khác để thực hiện truy vấn DB. Các module phải giao tiếp thông qua Facade Pattern.
- Sprint này tiếp tục chuẩn bị nền tảng sạch để dự án có thể dễ dàng tách thành các Microservices trong tương lai.

## 2. Thành quả đạt được (Done)
- **Tạo Facades**:
  - `BranchFacade` (branch module): lấy Map Branch Name.
  - `CatalogFacade` (catalog module): lấy Map ProductBasicInfo (Code, Name).
  - `CrmFacade` (crm module): lấy Map Customer Name, kiểm tra tồn tại Customer.
  - `OrderFacade` (order module): lấy Map Invoice Codes và Return Codes.
- **Tách Repository trong `StockService` (Inventory)**:
  - Bỏ `ProductRepository`, `BranchRepository`.
  - Thay bằng `CatalogFacade` và `BranchFacade`.
- **Tách Repository trong `GoodsReturnService` (Order)**:
  - Bỏ `CustomerRepository`.
  - Thay bằng `CrmFacade`. Đổi mapping liên kết Hibernate từ `@ManyToOne Customer customer` sang `UUID customerId` thuần tuý trong bảng `GoodsReturn` để cắt đứt liên kết foreign-key ngầm cấp ORM giữa Order và CRM.
- **Tách Repository trong `SalesInvoiceService` (Order)**:
  - Bỏ `CustomerRepository`.
  - Thay bằng `CrmFacade` để truy xuất CustomerName.
- **Tách Repository trong `ReceivableDebtController` (CRM)**:
  - Bỏ `BranchRepository`, `SalesInvoiceRepository`, `GoodsReturnRepository`.
  - Sử dụng `OrderFacade` và `BranchFacade`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định:** Chuyển `@ManyToOne Customer customer` thành `UUID customerId` trong bảng `GoodsReturn`.
  - **Lý do:** Trước đây Order Module chứa liên kết cứng mức DB sang CRM Module (phụ thuộc vào Java Object `Customer`). Thay thế bằng `UUID` giúp Domain Model của `Order` độc lập hoàn toàn với `CRM`. Việc lấy chi tiết được delegate sang `CrmFacade`.
  - **Đã cân nhắc:** Giữ nguyên `@ManyToOne` và dùng `customerRepository.getReferenceById` — Bỏ vì vẫn phải import Java Class `Customer` sang `order` package, vi phạm nguyên lý cô lập Bounded Context.
- **Quyết định:** Sử dụng Facade trả về `Map<ID, Value>` để batch fetch.
  - **Lý do:** Đây là pattern phổ biến khi giao tiếp liên services hoặc liên modules để tránh N+1. Mỗi Facade cung cấp hàm nhận vào `Collection<ID>` và trả về dữ liệu phẳng (DTO hoặc Map) để Service gọi dễ dàng ánh xạ vào bộ nhớ.

## 4. Hướng dẫn đọc code theo thứ tự
1. Các class `*Facade.java` mới tạo trong thư mục `api` của các module.
2. `StockService.java`: Xem cách lấy thông tin Product, Branch thông qua Facade.
3. `GoodsReturnService.java` & `GoodsReturn.java`: Xem cách chuyển đổi từ `Customer` entity sang `customerId` và sử dụng `CrmFacade`.
4. `SalesInvoiceService.java`: Xem cách lấy Customer Name qua Facade.
5. `ReceivableDebtController.java`: Xem cách thay thế Repo chéo bằng Facade map.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Hiện tại các Facade đang gọi trực tiếp Repository cục bộ và trả ra ngoài. Nếu tách thành Microservices thực sự, các Facade này sẽ trở thành HTTP/gRPC Clients.
- Việc xử lý Data Consistency (Tính nhất quán dữ liệu) giữa các module đang sử dụng Spring Events (`ApplicationEventPublisher`), nhưng chúng vẫn đang chạy trong cùng 1 Transaction Database. Khi lên Microservices, ta phải thiết kế Outbox Pattern cho các Events này.

## 6. Việc chưa làm / Out of scope
- Chia tách Schema DB: Dù code mức độ ứng dụng đã không gọi chéo Repository, toàn bộ ứng dụng vẫn đang dùng chung 1 database schema (do cấu hình Flyway hiện tại). Việc thiết kế đa schema (ví dụ `erp_crm`, `erp_order`) nằm ngoài phạm vi sprint này.

## 7. Cách chạy & Cách verify (Reproduce)
Lệnh build toàn bộ và chạy Test:
```bash
./mvnw clean test
```
*Kết quả mong đợi: `BUILD SUCCESS`, tất cả 133 integration/unit tests pass, báo hiệu Facade đã hoạt động chính xác và thay thế hoàn hảo cho Repositories.*
Kiểm tra code không còn cross-module Repository:
```bash
git grep "import com.storename.erp.*.infrastructure.*Repository" code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/
```
*Kết quả mong đợi: Chỉ các file của module A import `A.infrastructure.*Repository`. Không có module B import module A repository.*

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Mã nguồn hiện tại (Phase 8) đã sạch sẽ cả về mặt cấu trúc N+1 và Ranh giới Modules (Bounded Contexts).
- **Sprint 22** (Final E2E Regression) sẽ là lúc ta thực hiện các flow test cuối cùng trước khi đóng version để kết thúc chiến dịch tối ưu này.
