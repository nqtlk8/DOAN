# Purchasing Integration Test Cases

## Bối cảnh (Context)
Kiểm thử luồng tạo Đơn Nhập Hàng (Purchase Order) và cập nhật số lượng tồn kho (Inventory). Khi một đơn nhập hàng được tạo thành công, hệ thống phải sinh ra các bản ghi `inventory_transaction` với type là `PURCHASE_IN` để cộng thêm số lượng vào Inventory.

## Các kịch bản kiểm thử (Test Cases)

### TC-PO-01: Tạo Purchase Order thành công và cộng tồn kho
- **Mô tả:** Kiểm tra luồng End-to-End khi người dùng có quyền (ADMIN) gọi API tạo Đơn Nhập Hàng với dữ liệu hợp lệ.
- **Tiền điều kiện:** 
  - `Distributor` và `Product` đã tồn tại trong DB (Mocked).
  - Tồn kho ban đầu của Product được ghi nhận (ví dụ: 50).
- **Các bước thực hiện:**
  1. Gửi `GET /api/products?code={product_code}` để lấy tồn kho hiện tại.
  2. Gửi `POST /api/purchase-orders` với `distributorId`, `orderDate`, và danh sách `items` hợp lệ (số lượng nhập = 20).
  3. Kiểm tra HTTP Status trả về là `201 Created` và body `success: true`.
  4. Gửi lại `GET /api/products?code={product_code}` để kiểm tra tồn kho.
- **Kết quả mong đợi:** Tồn kho phải tăng thêm tương ứng với số lượng trong Đơn Nhập Hàng (ví dụ: 50 + 20 = 70). API tạo PO thành công trả về thông tin đơn hàng hợp lệ.

### TC-PO-02: Tạo Purchase Order thất bại do thiếu Items
- **Mô tả:** Đảm bảo hệ thống bắt lỗi (Validation) khi Request Body không chứa sản phẩm nào (`items` trống).
- **Các bước thực hiện:**
  1. Gửi `POST /api/purchase-orders` với `distributorId` hợp lệ nhưng `items = []`.
- **Kết quả mong đợi:** HTTP Status trả về `400 Bad Request`.

### TC-PO-03: Tạo Purchase Order thất bại do thiếu DistributorId
- **Mô tả:** Đảm bảo hệ thống bắt lỗi (Validation) khi thiếu thông tin `distributorId`.
- **Các bước thực hiện:**
  1. Gửi `POST /api/purchase-orders` với các `items` hợp lệ nhưng không truyền `distributorId`.
- **Kết quả mong đợi:** HTTP Status trả về `400 Bad Request`.

## Kỹ thuật tự động hóa (Automation)
Sử dụng **Spring Boot Test**, **MockMvc** và **Mockito** để giả lập các Dependencies (Repository) nhằm cô lập Unit/Integration test, đảm bảo tốc độ thực thi nhanh và không phụ thuộc vào dữ liệu DB vật lý trong môi trường test:
- `@MockBean` được áp dụng cho `DistributorRepository`, `ProductRepository`, `PurchaseOrderRepository`, `InventoryTransactionRepository`.
- Cấu hình Mock `InventoryTransactionRepository.save` để tự động cộng giá trị vào biến `AtomicInteger` mô phỏng Database khi nhận transaction type là `PURCHASE_IN`.
