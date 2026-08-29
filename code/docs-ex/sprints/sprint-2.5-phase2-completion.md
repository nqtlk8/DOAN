# Sprint 2.5 - Phase 2 Completion & Core Stabilizations

**Ngày hoàn thành:** 2026-08-23
**Module:** Toàn hệ thống (Common, Order, Inventory, CRM)

---

## 1. Mục tiêu Sprint (Goal)
Hoàn thiện và chốt sổ Phase 2 (Core ERP). Sprint này tập trung vào việc khắc phục 5 vấn đề cốt lõi (P0/P1) về Concurrency, Idempotency, Security và Business Logic bị sót lại từ các Sprint trước, đảm bảo hệ thống an toàn tuyệt đối khi chạy đa luồng và đồng bộ dữ liệu.

## 2. Thành quả đạt được (Done)
- **Đảm bảo tính Atomic cho Idempotency:** Bọc toàn bộ quy trình kiểm tra và lưu `IdempotencyRecord` vào một `TransactionTemplate` (propagation `REQUIRED`) để đảm bảo Rollback đồng bộ với nghiệp vụ. File: `com.storename.erp.common.aop.IdempotencyAspect`.
- **Nâng cấp Idempotency Hash:** `requestHash` được sinh ra từ HTTP Method + URI + Payload (loại trừ các header biến động như JWT). Trả về 409 Conflict nếu trùng Key nhưng khác Hash. File: `com.storename.erp.common.aop.IdempotencyAspect`.
- **Chuẩn hoá Retry Cơ sở Dữ liệu:** Thay thế vòng lặp while tự chế bằng `@Retryable` của `spring-retry` bao bọc bên ngoài ranh giới `@Transactional` cho các Controller/Service nhạy cảm (SalesInvoice, GoodsReturn, OutboundReceipt).
- **Ràng buộc Hóa đơn Trả hàng (Goods Return Validation):** Ràng buộc chặt chẽ tính nguyên vẹn: Trả hàng phải đối chiếu với `SalesInvoice` hợp lệ (trạng thái `CONFIRMED`), không vượt quá số lượng đã bán. Bổ sung khóa ngoại thông qua migration `V7__goods_return_invoice_fk.sql`. Files: `GoodsReturnService`, `GoodsReturnController`.
- **Dọn dẹp nợ kỹ thuật Security:** Loại bỏ việc sinh ngẫu nhiên UUID cho `userId` tại Controller, thay bằng UUID deterministic dựa trên JWT Auth Name (`UUID.nameUUIDFromBytes`). Sửa lỗi NPE tại `SalesInvoiceController` do map sai `@AuthenticationPrincipal`.

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)

**Quyết định 1: Dùng `@Retryable` (spring-retry) ngoài `@Transactional` thay vì tự lặp `while` bên trong.**
- **Lý do:** Spring Boot đánh dấu Transaction là "Rollback-Only" ngay khi có `ObjectOptimisticLockingFailureException` văng ra từ repository, làm cho vòng lặp `while` bên trong transaction thất bại ở chặng commit cuối với lỗi `UnexpectedRollbackException`. Retry ở ngoài `@Transactional` đảm bảo mỗi lần thử lại đều là một transaction độc lập và "sạch sẽ".
- **Đã cân nhắc:** Tự lặp `while` bên trong method `Service` — bỏ vì chạm phải cơ chế ngầm định rollback-only của Spring.

**Quyết định 2: Request Hash cho Idempotency bỏ qua Header Authorization.**
- **Lý do:** Client (như App Mobile/Web) có thể retry request sau khi token JWT bị hết hạn và đã được làm mới (refresh token), dẫn tới việc nếu hash luôn cả token thì sẽ bị báo lỗi hash mismatch.
- **Đã cân nhắc:** Hash toàn bộ HTTP Headers — bỏ vì quá dễ thay đổi (các header tự sinh như User-Agent, Auth) gây false positive.

**Quyết định 3: Gộp Idempotency Record vào cùng Transaction nghiệp vụ.**
- **Lý do:** Đảm bảo tính nguyên tử (Atomicity). Nếu nghiệp vụ fail (ví dụ: kho không đủ hàng), Transaction bị rollback thì Idempotency Record cũng phải không được lưu, để client có thể sửa request và gọi lại.
- **Đã cân nhắc:** Lưu Idempotency Record trong một `REQUIRES_NEW` Transaction — bỏ vì sẽ dẫn đến việc lưu key thành công mặc dù nghiệp vụ thất bại, cản trở việc retry hợp lệ.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `IdempotencyAspect.java` (module `common/aop`): Quan sát cách dùng `TransactionTemplate` và cách tính MD5 Hash cho `serializableArgs`.
2. `SalesInvoiceService.java` & `GoodsReturnService.java` (module `order/application`): Chú ý Annotation `@Retryable` nằm ngoài cùng và các Exception được retry.
3. `GoodsReturnService.java`: Đọc logic validate `SalesInvoice` gốc (kiểm tra trạng thái `CONFIRMED` và tính tổng lượng hàng đã trả).
4. `SalesInvoiceController.java` (module `order/api`): Quan sát cách trích xuất `BranchId` và `UserId` an toàn từ thẻ `Authentication.getDetails()`.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- **Thiếu GlobalExceptionHandler:** Hiện tại hệ thống dựa vào BasicErrorController của Spring Boot. Cần xây dựng một `@RestControllerAdvice` tập trung ở Sprint tiếp theo để chuẩn hóa mọi Exception về `ApiResponse.error()` thay vì JSON 500 mặc định. Ảnh hưởng hiện tại: Lỗi trả về có dạng JSON rỗng/không chuẩn với format frontend kỳ vọng.
- **Cơ chế kho bán khống (Negative Stock):** Do module Inventory cho phép bán khống khi gọi từ `SALES_INVOICE`, nên việc hết hàng không tự động ném Exception. Nếu đây không phải là business requirement thực tế của toàn bộ doanh nghiệp, cần siết lại ở Phase tiếp theo.

## 6. Việc chưa làm / Out of scope
- **Lịch sử trạng thái đơn hàng (State Machine/History Log):** Chưa lưu trữ chi tiết ai đã đổi trạng thái hoá đơn vào thời điểm nào (chỉ cập nhật trường `updatedBy`). Không làm trong sprint này do ưu tiên giải quyết các bug P0/P1 trước tiên.

## 7. Cách chạy & Cách verify (Reproduce)
- **Chạy toàn bộ Unit/Integration Test:**
  ```bash
  .\mvnw test
  ```
- **Kết quả mong đợi:** 100% Tests Pass. Các bộ test Concurrent (ví dụ `OrderConcurrencyTest`) sẽ thành công và không bị Deadlock hay OptimisticLock crash ngang; `IdempotencyIntegrationTest` đảm bảo 409 Conflict cho hash sai và Rollback sạch khi gặp lỗi.

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Sprint 2.5 đã đóng gói hoàn thiện Phase 2. Sprint tiếp theo (Phase 3) nên bắt tay vào **Quy trình Tài chính cơ bản** (Cash/Bank, General Ledger, AP/AR). 
- **Lưu ý:** Khi xây dựng Controller mới trong Phase 3, hãy nhớ lấy thông tin Branch và User từ `SecurityContextHolder.getContext().getAuthentication()` theo cú pháp đã chuẩn hoá (như trong `GoodsReturnController`), KHÔNG parse ép kiểu qua `@AuthenticationPrincipal JwtAuthDetails`.
