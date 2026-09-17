# API Contract - v5 (Post-Refactoring)

## 1. Chuẩn response chung
Tất cả các REST API đều được bọc trong `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "message": "...",
  "errors": null
}
```

**Đặc biệt lưu ý**: Controller tuyệt đối không trả về JPA Domain Entity (`SalesInvoice`, `GoodsReturn`, `Product`, ...). Mọi payload trong `data` đều đã được mapping sang class `*ResponseDto` tương ứng (ví dụ: `SalesInvoiceResponseDto`, `ReceivableDebtResponseDto`).

## 2. Header yêu cầu
- `Authorization`: `Bearer <token>` (cho các endpoint bị protect).
- `Idempotency-Key`: Chuỗi UUID định danh request. **Bắt buộc** đối với tất cả các API thay đổi dữ liệu (POST, PUT, DELETE) như `confirmInvoice`, `confirmReceipt`, `decreaseDebt`.

## 3. Quản lý Quyền & Xác thực
- **Xác thực**: JWT được tạo từ HQ (RS256 Private Key) và được verify tại các Branch (RS256 Public Key).
- **Phân quyền Role**: Dùng `@PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")`. Không có tiền tố `ROLE_`.
- **Phân quyền Branch**: Backend không còn dùng `@BranchScoped`. Thay vào đó, lấy thông tin nhánh tự động từ Token bằng hàm `AuthUtils.getBranchIdOrNull()`. Nhân viên chi nhánh chỉ xem/thêm/sửa dữ liệu của chi nhánh mình.

## 4. Danh sách các API chính

### 4.1. Authentication (HQ Only)
| Method | Path | Controller | Instance |
|---|---|---|---|
| POST | `/api/v1/auth/login` | `AuthController` | HQ |
| POST | `/api/v1/auth/refresh` | `AuthController` | HQ |
| POST | `/api/v1/auth/revoke` | `AuthController` | HQ |

### 4.2. Master Data (Catalog, CRM, Branch) - Tự động đồng bộ HQ -> Branch
| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/branches` | HQ-only controller |
| GET/POST/PUT | `/api/v1/catalog/products` | POST/PUT bị giới hạn `HQ + ADMIN` |
| GET/POST | `/api/v1/suppliers` | Response bọc `ApiResponse<SupplierResponseDto>` |
| GET/POST/PUT | `/api/v1/customers` | HQ-only, ADMIN cho thao tác POST/PUT |

### 4.3. Inventory (Tồn Kho & Phiếu Nhập)
| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/inventory/stock` | Trả về `StockOnHand` theo chi nhánh |
| GET | `/api/v1/stock-movements` | Lịch sử sổ kho |
| POST | `/api/v1/inventory/inbound` | Draft phiếu nhập |
| POST | `/api/v1/inventory/inbound/{id}/confirm` | Cập nhật kho, tính CostLayer. Yêu cầu `Idempotency-Key` |

### 4.4. Sales (Bán Hàng) & Goods Return (Trả Hàng)
| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/v1/sales-invoices` | Draft Invoice |
| POST | `/api/v1/sales-invoices/{id}/confirm` | Xác nhận bán, xuất kho FIFO, tăng Nợ (`ReceivableDebtMovementType.SALE`). Yêu cầu `Idempotency-Key` |
| POST | `/api/v1/goods-returns` | Draft Return |
| POST | `/api/v1/goods-returns/{id}/confirm` | Xác nhận khách trả, nhập lại kho, giảm Nợ (`ReceivableDebtMovementType.RETURN`). Yêu cầu `Idempotency-Key` |

### 4.5. Receivable Debt (Công Nợ Khách Hàng)
| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/receivable-debts` | DTO bao gồm `customerId`, tổng nợ. |
| POST | `/api/v1/receivable-debts/payments` | API Khách trả tiền. Gọi Service giảm nợ (`ReceivableDebtMovementType.PAYMENT`). Yêu cầu `Idempotency-Key` |

## 5. Xử lý lỗi (Exception Handling)
Tất cả các exception được `GlobalExceptionHandler` chặn lại và trả về:
```json
{
  "success": false,
  "data": null,
  "message": "Nội dung lỗi chi tiết",
  "errors": ["Error 1", "Error 2"] 
}
```
Mã HTTP tuỳ thuộc vào loại exception (ví dụ: `400 Bad Request` cho Validation, `409 Conflict` cho Idempotency, `404 Not Found` cho sai ID).
