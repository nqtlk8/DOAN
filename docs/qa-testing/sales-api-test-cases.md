# Sales & Master Data API Test Cases (Sprint 13)

**Mô tả:** Tài liệu kịch bản kiểm thử (Black-box Testing) dành cho các API liên quan đến Master Data (Khách hàng, Sản phẩm), Báo giá (Quotations) và Đơn hàng (Sales Orders).

---

## 1. API: `GET /api/customers` và `GET /api/products`

### TC-MD-01: Lấy danh sách khách hàng không có bộ lọc
- **Mô tả:** Gọi API lấy danh sách khách hàng mặc định.
- **Input (Payload / Query):** `GET /api/customers`
- **Expected Output:**
  - Status Code: `200 OK`
  - Body trả về JSON với `success: true`.
  - Có mảng `items` chứa danh sách khách hàng (có id, code, name, email, phone).
  - Có các trường phân trang `total`, `page=1`, `size=20`.

### TC-MD-02: Lấy danh sách khách hàng có bộ lọc (Filter)
- **Mô tả:** Tìm kiếm khách hàng theo tên.
- **Input (Payload / Query):** `GET /api/customers?name=Acme`
- **Expected Output:**
  - Status Code: `200 OK`
  - Mảng `items` chỉ chứa các khách hàng có tên khớp hoặc chứa chuỗi "Acme".
  - Trường hợp không tìm thấy trả về mảng `items` rỗng `[]`, `total=0`.

### TC-MD-03: Lấy danh sách sản phẩm và kiểm tra số lượng tồn kho (stockQuantity)
- **Mô tả:** Gọi API lấy danh sách sản phẩm để xác minh trường `stockQuantity` và `basePrice` được trả về đúng.
- **Input (Payload / Query):** `GET /api/products`
- **Expected Output:**
  - Status Code: `200 OK`
  - JSON trả về `success: true`.
  - Mỗi object trong mảng `items` phải có trường `stockQuantity` (số nguyên, không âm) và `basePrice` (số thực).

### TC-MD-04: Lấy danh sách sản phẩm có bộ lọc (Filter)
- **Mô tả:** Tìm kiếm sản phẩm kết hợp theo mã và tên.
- **Input (Payload / Query):** `GET /api/products?code=PROD-1001&name=Chair`
- **Expected Output:**
  - Status Code: `200 OK`
  - Mảng `items` chỉ chứa sản phẩm thoả mãn điều kiện mã và tên tương ứng.

---

## 2. API: `POST /api/quotations`

### TC-QT-01: Tạo báo giá thành công (Dữ liệu hợp lệ)
- **Mô tả:** Gửi payload chuẩn để tạo mới một báo giá.
- **Input (Payload):**
  ```json
  {
    "customerId": "uuid-customer-123",
    "validUntil": "2026-12-31",
    "remarks": "Special discount applied",
    "items": [
      {
        "productId": "uuid-product-123",
        "quantity": 10,
        "unitPrice": 140.00,
        "discountPercent": 5.0
      }
    ]
  }
  ```
- **Expected Output:**
  - Status Code: `201 Created`
  - Body trả về: `success: true`, `data` chứa `quotationId` và `quotationCode`.
  - Hệ thống tạo record mới trong database (backend verify).

### TC-QT-02: Tạo báo giá thất bại (Sai cấu trúc - Thiếu trường bắt buộc)
- **Mô tả:** Gửi payload thiếu thông tin `customerId`.
- **Input (Payload):**
  ```json
  {
    "validUntil": "2026-12-31",
    "items": [
      {
        "productId": "uuid-product-123",
        "quantity": 1,
        "unitPrice": 100.00
      }
    ]
  }
  ```
- **Expected Output:**
  - Status Code: `400 Bad Request` (hoặc `422 Unprocessable Entity`).
  - Body trả về: `success: false`, thông báo lỗi (message) và mảng `errors` chỉ rõ thiếu `customerId`.

### TC-QT-03: Tạo báo giá thất bại (Sai cấu trúc - Không có sản phẩm)
- **Mô tả:** Gửi payload với danh sách sản phẩm rỗng.
- **Input (Payload):**
  ```json
  {
    "customerId": "uuid-customer-123",
    "validUntil": "2026-12-31",
    "items": []
  }
  ```
- **Expected Output:**
  - Status Code: `400 Bad Request`.
  - Body trả về: `success: false`, `errors` thông báo danh sách sản phẩm (`items`) không được để trống.

### TC-QT-04: Tạo báo giá thất bại (Sai cấu trúc - Số lượng âm)
- **Mô tả:** Gửi payload với `quantity` là số âm.
- **Input (Payload):**
  ```json
  {
    "customerId": "uuid-customer-123",
    "validUntil": "2026-12-31",
    "items": [
      {
        "productId": "uuid-product-123",
        "quantity": -5,
        "unitPrice": 140.00
      }
    ]
  }
  ```
- **Expected Output:**
  - Status Code: `400 Bad Request`.
  - Body trả về: `success: false`, `errors` thông báo số lượng (`quantity`) không hợp lệ.

---

## 3. API: `POST /api/sales-orders`

### TC-SO-01: Tạo đơn hàng thành công và kiểm tra tồn kho (End-to-End Test)
- **Mô tả:** Tạo một đơn hàng hợp lệ. Sau đó kiểm tra API Products để xác nhận số lượng tồn kho (stockQuantity) đã bị trừ.
- **Các bước:**
  1. Giao dịch 1: `GET /api/products?code=PROD-1001` (Giả sử số lượng tồn là 50).
  2. Giao dịch 2: Tạo đơn hàng (Payload bên dưới).
  3. Giao dịch 3: `GET /api/products?code=PROD-1001`.
- **Input (Payload Giao dịch 2):**
  ```json
  {
    "customerId": "uuid-customer-123",
    "quotationId": "uuid-quotation-123",
    "remarks": "Reference to quotation",
    "expectedDeliveryDate": "2026-08-01",
    "items": [
      {
        "productId": "uuid-product-1001",
        "quantity": 10,
        "unitPrice": 133.00
      }
    ]
  }
  ```
- **Expected Output:**
  - Giao dịch 2: Trả về `201 Created`, `success: true`, và `salesOrderCode`.
  - Giao dịch 3: Tồn kho của sản phẩm `PROD-1001` phải giảm xuống thành 40 (50 - 10). Dữ liệu Inventory Transaction type `SALES_OUT` được tạo ngầm định.

### TC-SO-02: Tạo đơn hàng thất bại (Truyền thiếu sản phẩm)
- **Mô tả:** Gửi payload không có trường `items` (truyền thiếu sản phẩm).
- **Input (Payload):**
  ```json
  {
    "customerId": "uuid-customer-123",
    "expectedDeliveryDate": "2026-08-01"
  }
  ```
- **Expected Output:**
  - Status Code: `400 Bad Request`.
  - Body trả về: `success: false`, `errors` chỉ rõ trường `items` là bắt buộc.

### TC-SO-03: Tạo đơn hàng thất bại (Số lượng lớn hơn tồn kho hiện tại)
- **Mô tả:** Đặt mua số lượng hàng (ví dụ: 1000) lớn hơn lượng tồn kho thực tế của sản phẩm đó.
- **Input (Payload):**
  ```json
  {
    "customerId": "uuid-customer-123",
    "expectedDeliveryDate": "2026-08-01",
    "items": [
      {
        "productId": "uuid-product-1001",
        "quantity": 1000,
        "unitPrice": 133.00
      }
    ]
  }
  ```
- **Expected Output:**
  - Status Code: `400 Bad Request` hoặc `409 Conflict`.
  - Body trả về: `success: false`, `message` hiển thị lỗi "Không đủ số lượng tồn kho cho sản phẩm".
  - Kiểm tra tồn kho vẫn giữ nguyên như cũ, không bị trừ âm.
