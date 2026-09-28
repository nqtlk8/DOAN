# Sprint 20: N+1 & Read Model Cleanup
Date: 2026-09-17

## 1. Mục tiêu Sprint (Goal)
- Loại bỏ triệt để các truy vấn N+1 làm giảm hiệu suất hệ thống khi lấy danh sách dữ liệu có liên kết (Customer, Branch, Product, Category, v.v.).
- Làm sạch Controller: Ngăn chặn việc trả về trực tiếp Domain Entity ra ngoài API, chuyển sang dùng Response DTO thuần tuý.
- Sprint này tiếp nối giai đoạn tinh chỉnh (Phase 8), giúp ứng dụng sẵn sàng cho tải thực tế.

## 2. Thành quả đạt được (Done)
- **N+1 trên Controller ReceivableDebtController**: Fix lỗi fetch Branch, SalesInvoice, GoodsReturn lặp lại bằng cách gom các ID và truy vấn theo lô (`branchRepository.findAllById`, `invoiceRepository.findAllById`). Sửa đổi tại `ReceivableDebtController.java`.
- **N+1 trên ReceivableDebtRepository**: Cấu hình `@EntityGraph(attributePaths = {"customer"})` để tải eager Customer trong các phương thức `findAll`, `findByBranchId`, tránh N+1 khi duyệt danh sách công nợ. 
- **N+1 trên ProductReader**: Cấu hình `@EntityGraph` cho `ProductRepository.findAll` để nạp eager Category. Đồng thời sửa phương thức `getAllProductsWithBranchPrice` trong `ProductReader.java` dùng `PriceListRepository.findByBranchIdAndProductIdIn` (fetch theo lô tất cả giá chi nhánh), xoá sổ vòng lặp N+1 `priceListRepository`.
- **N+1 trên SalesInvoiceService**: Thay thế vòng lặp `.findById()` của CustomerRepository trong stream bằng cách map `customerIds` rồi batch fetch. Sửa tại `SalesInvoiceService.java`.
- **N+1 trên GoodsReturnService**: Thay thế cách lookup Customer Name chậm chạp sang batch fetch.
- **DTO cho GoodsReturn**: Ngăn chặn `GoodsReturnController` trả về `com.storename.erp.order.domain.GoodsReturn` (entity có quan hệ Lazy -> tiềm ẩn lỗi hoặc lộ cấu trúc DB). Tạo mới `GoodsReturnResponseDto` và sửa lại `GoodsReturnService.java` để map và trả về DTO.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions)
- **Quyết định:** Sử dụng Batch Fetching trong Service/Controller (kết hợp `collect(Collectors.toSet())` và `findAllById`) đối với các Aggregate Roots khác Module (ví dụ Order lấy thông tin Customer).
  - **Lý do:** Spring Data `@EntityGraph` chỉ hoạt động cho quan hệ nội bộ trong cùng Database Context/Aggregate. Với tham chiếu xuyên module (Customer được lưu là UUID trong Order), ta bắt buộc phải dùng batch fetching thủ công qua Service facade hoặc Repository liên kết để đảm bảo hiệu suất.
  - **Đã cân nhắc:** Bỏ qua không hiển thị tên Customer trong danh sách Order (bỏ qua vì không đáp ứng yêu cầu nghiệp vụ); Viết native query JOIN (bỏ qua vì vi phạm ranh giới Module Domain Driven Design).
- **Quyết định:** Entity `GoodsReturn` phải được bọc trong `GoodsReturnResponseDto` trước khi trả về.
  - **Lý do:** Việc trả Entity có trường `@ManyToOne(fetch = FetchType.LAZY)` sẽ khiến Jackson serializer bắn `LazyInitializationException` khi nó ra khỏi giao dịch (ngoài layer Service), hoặc sinh ra N+1 nếu Open-In-View bật. DTO là lớp khiên an toàn duy nhất.

## 4. Hướng dẫn đọc code theo thứ tự
1. `ReceivableDebtController.java`: Chú ý phần cleanup N+1 sử dụng `findAllById` map.
2. `ProductRepository.java` & `PriceListRepository.java`: Chú ý `@EntityGraph` và phương thức `findByBranchIdAndProductIdIn`.
3. `ProductReader.java`: Chú ý cách `finalPriceMap` được tạo ra qua 1 lượt truy vấn DB và gán cho các Product DTO.
4. `SalesInvoiceService.java` & `GoodsReturnService.java`: Cách map `CustomerName` batch.
5. `GoodsReturnResponseDto.java`: Cấu trúc DTO mới thay cho Entity trả về.

## 5. Rủi ro / Nợ kỹ thuật đã biết
- Mặc dù đã dùng Batch Fetching (`findAllById`) cho danh sách lớn, nếu số lượng phần tử trả về vượt qua 1000 records, một số CSDL (như Oracle hoặc cấu hình của Postgres) có thể bị quá tải IN clause limit. Về lâu dài cần tính phương án phân trang (Pagination) bắt buộc trên mọi endpoint GET List.
- Tên khách hàng (Customer Name) được sao chép sang DTO của Invoice và Return ngay lúc query (Read time). Nếu tên khách hàng thay đổi, Invoice cũ vẫn hiển thị tên mới. Nghiệp vụ có thể yêu cầu lưu snapshot tên khách hàng ngay trong `SalesInvoice` lúc xuất hoá đơn. Cần review lại trong Sprint sau.

## 6. Việc chưa làm / Out of scope
- Phân trang (Pagination) cho `ReceivableDebtController` hay `ProductReader` không nằm trong scope xử lý N+1 sprint này. Sprint này chỉ tối ưu cấu trúc query hiện tại để không sụp đổ vì N+1 vòng lặp.

## 7. Cách chạy & Cách verify (Reproduce)
Chạy maven test để đảm bảo tất cả DTO mới và thay đổi không phá vỡ Logic hiện hành:
```bash
./mvnw clean test
```
*Kết quả mong đợi: `BUILD SUCCESS` không còn lỗi biên dịch từ DTO mapper, 133 tests PASS toàn bộ.*

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Mã nguồn về cơ bản đã xử lý xong N+1 cho đọc danh sách.
- Sprint 21 (Module Boundary Cleanup) sẽ tiếp tục giải quyết các liên kết chéo module để đảm bảo code sẵn sàng cho microservices nếu cần tách sau này. Cần cẩn trọng khi tách `CustomerRepository` ra khỏi `SalesInvoiceService`.
