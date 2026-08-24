# SKILL: AOP — Cross-Cutting Concerns

**Trigger:** Đọc skill này bất cứ khi nào code động tới: ghi log audit, transaction,
validate input, phân quyền theo chi nhánh, đo hiệu năng, hoặc retry.

---

## 1. Nguyên tắc chung

Mối quan tâm xuyên suốt (cross-cutting concern) BẮT BUỘC triển khai bằng **annotation
+ Spring AOP**, đặt Aspect tại `common/aop/`. **CẤM** viết tay logic này lặp lại
trong từng Service (VD: tự viết `log.info(...)` để audit, tự `try/catch` để retry).

## 2. Bảng annotation bắt buộc và khi nào dùng

| Annotation | Đặt ở đâu | Bắt buộc dùng khi nào |
|---|---|---|
| `@Transactional` | Method Service (`application` layer) | Mọi method có ghi dữ liệu (CREATE/UPDATE/DELETE), đặc biệt method nào động tới ≥ 2 bảng cùng lúc |
| `@Auditable` | Method Service | Mọi thao tác sửa giá (`price_list`), xác nhận hoá đơn (`SalesInvoice.confirm`), trả hàng (`GoodsReturn.confirm`), sửa công nợ, sửa phân quyền |
| `@Valid` (Bean Validation) | Tham số `@RequestBody` ở Controller | Mọi DTO request |
| `@BranchScoped` | Method Repository/Service truy vấn bảng có cột `branch_id` | Xem chi tiết đầy đủ ở skill `08-branch-scope-multitenancy` |
| `@LogExecutionTime` | Method Service nghiệp vụ chính | Method nào nằm trên hot path đã xác định (bán hàng, tìm kiếm sản phẩm, dashboard) |
| `@Retryable` (Spring Retry) | Method publish event/đồng bộ có thể lỗi tạm thời | Đồng bộ replication, gọi service ngoài (email, S3) |

## 3. Vị trí implementation của từng Aspect

```
common/aop/
 ├── AuditAspect.java          → @Around("@annotation(Auditable)"), ghi vào bảng audit_log
 ├── LogExecutionTimeAspect.java → @Around, log cảnh báo nếu method > ngưỡng cấu hình (application.yml)
 ├── BranchScopedAspect.java    → đọc branchId từ JWT claim (SecurityContext), tự thêm điều kiện lọc
 └── (Transaction, Validation, Retry dùng annotation chuẩn Spring, không cần Aspect tự viết)
```

## 4. Xử lý lỗi tập trung

- Toàn bộ exception xử lý qua `@ControllerAdvice` duy nhất tại `common/exception/`.
- Custom exception theo domain, kế thừa `BusinessException` chung (VD:
  `InsufficientStockException`, `ExceedOrderedQuantityException`,
  `InvalidInvoiceStateException`).
- Trả về client: mã lỗi chuẩn hoá + message, KHÔNG lộ stack trace.

## 5. Quy tắc riêng cho `@Transactional` khi động tới tài chính/kho

Bất kỳ domain method nào làm ≥ 2 việc sau đây cùng lúc PHẢI nằm trong **CÙNG 1
method có `@Transactional`**, không tách thành nhiều lời gọi transaction riêng:

- Trừ/cộng `stock_on_hand`
- Cập nhật `receivable_debt`/`payable_debt`
- Fulfill `customer_order_line`

Xem đầy đủ quy tắc và danh sách domain method cụ thể phải tuân thủ ở skill
`05-transaction-data-integrity`.

## 6. Checklist tự kiểm tra

- [ ] Method ghi dữ liệu có `@Transactional` chưa?
- [ ] Thao tác nhạy cảm (giá, hoá đơn, công nợ) có `@Auditable` chưa?
- [ ] DTO request có `@Valid` chưa?
- [ ] Repository/Service truy vấn bảng có `branch_id` có `@BranchScoped` chưa?
- [ ] Có đoạn code nào tự viết tay logic mà lẽ ra dùng annotation/AOP không? (dấu hiệu vi phạm skill này)
