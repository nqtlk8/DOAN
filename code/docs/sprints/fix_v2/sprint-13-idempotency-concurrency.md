---
date: 2026-09-17
---

# Sprint 13: Idempotency & Concurrency Control

## 1. Mục tiêu Sprint (Goal)
- Chống lỗi click đúp (double-click) trên giao diện khiến người dùng tạo ra nhiều đơn hàng/phiếu nhập giống nhau ngoài ý muốn.
- Xử lý race-condition tại Backend để đảm bảo an toàn tuyệt đối cho số liệu kế toán (Công nợ) và Kho hàng (Inventory), ngăn ngừa việc trừ/cộng dồn 2 lần (double deduction) cho cùng một thao tác xác nhận đơn.

## 2. Thành quả đạt được (Done)
- Cập nhật cơ chế sinh `Idempotency-Key` trên **Frontend** (`axiosInstance.ts`): Thay vì sinh ngẫu nhiên UUID cho mọi request POST/PUT (khiến mất tác dụng chống trùng lặp), nay áp dụng kỹ thuật caching (theo hàm băm `method:url:payload`) có thời hạn 5 giây. Nếu request y hệt được gửi lên trong 5 giây, sẽ dùng chung key cũ.
- Tái cấu trúc (Refactor) **Backend AOP** (`IdempotencyAspect.java`) theo mẫu "Acquire/Release Lock" an toàn cho xử lý song song (Concurrency-safe):
  - Dùng một transaction riêng để ghi (insert) trạng thái `IN_PROGRESS` của `Idempotency-Key`.
  - Tận dụng `DataIntegrityViolationException` (từ ràng buộc UNIQUE của Database) để phát hiện và chặn tức thời luồng thứ 2 chạy song song thay vì để 2 luồng cùng lúc lọt vào hàm Controller xử lý nghiệp vụ.
  - Trả về mã lỗi HTTP 409 (Conflict) cho các request bị chặn, cho phép Frontend tự bỏ qua hoặc báo cho người dùng "Đang xử lý".
  - Hoàn trả Cached Response nếu request đã từng thực thi thành công. Xoá lock (delete) nếu request thất bại do lỗi nghiệp vụ để cho phép retry.
- Phủ Annotation `@IdempotencyProtected` cho các API `createDraft` của `SalesInvoiceController`, `GoodsReturnController`, và `InboundReceiptController` (trước đó chỉ có ở hàm `confirm`).

## 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
Quyết định: Sử dụng PostgreSQL Row-level lock qua hàm `saveAndFlush` với ràng buộc UNIQUE để phát hiện race-condition, thay vì dùng Redis hay In-memory lock (ví dụ ReentrantLock).
Lý do: Hệ thống có thể mở rộng (scale-out) ra nhiều instance của Backend (phục vụ mô hình Branch). Khóa bằng CSDL (Database lock) sẽ luôn đúng đắn trên môi trường phân tán mà không đòi hỏi thêm hạ tầng phụ trợ phức tạp như Redis ở giai đoạn hiện tại.
Đã cân nhắc: Sử dụng Redis distributed lock — Bỏ vì muốn giữ kiến trúc ERP nguyên bản (lean) nhất có thể, tập trung vào Database RDBMS.

Quyết định: TTL 5 giây cache Idempotency Key tại Frontend.
Lý do: Ngăn người dùng click "Tạo đơn" liên tục nhiều lần (do mạng lag hoặc thao tác vội). Tuy nhiên sau 5 giây, nếu họ thực sự muốn tạo lại 1 đơn y chang, hệ thống sẽ sinh key mới và tiếp nhận bình thường.
Đã cân nhắc: Hash vĩnh viễn (hash trọn đời) ở Frontend — Bỏ vì người dùng hoàn toàn có thể muốn lên 2 hóa đơn y hệt nhau trong cùng ngày.

## 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `apps/erp-frontend/src/api/axiosInstance.ts`: Đọc hàm `interceptors.request` để thấy cách Frontend tự động chèn và tái sử dụng `Idempotency-Key`.
2. `services/erp-backend/src/main/java/com/storename/erp/common/aop/IdempotencyAspect.java`: Đọc kỹ cách xử lý transaction 3 bước (Acquire Lock -> Execute -> Update Cache).
3. `services/erp-backend/src/main/java/com/storename/erp/order/api/SalesInvoiceController.java`: Xem các Annotation `@IdempotencyProtected` được đánh dấu thêm.

## 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Các Transaction trong `IdempotencyAspect` dùng chung Datasource của ứng dụng chính. Dưới tải lớn (High load), việc mở liên tục các connection ngắn để set "Lock" có thể gây cạn kiệt connection pool nếu cấu hình pool size quá nhỏ. (Có thể khắc phục sau này bằng tăng pool size hoặc offload sang Redis/Memcached).

## 6. Việc chưa làm / Out of scope
- Tự động dọn dẹp (Cleanup) các bản ghi `idempotency_record` quá cũ. Bảng này theo thời gian có thể phình to. Cần có một cron-job tự động xóa các record có tuổi thọ trên 30 ngày (sẽ được làm ở các Sprint dọn dẹp cuối).

## 7. Cách chạy & Cách verify (Reproduce)
- **Backend:** Chạy lệnh `.\mvnw test` để đảm bảo không bị gãy (break) luồng AOP Idempotency trong các Controller test. (Tất cả test case hiện tại đều pass 100%).
- **Manual Test:** 
  1. Vào giao diện Tạo Đơn Bán Hàng (Sales Invoice).
  2. Bấm nút "Lưu" (Xác nhận) liên tục 5 lần thật nhanh.
  3. Mở Network tab ở DevTools: Request đầu tiên sẽ trả về 200/201, 4 request sau sẽ trả về 409 Conflict.
  4. Mở Database: Chỉ có đúng 1 hóa đơn được ghi, Công nợ và Tồn kho được cộng/trừ 1 lần duy nhất!

## 8. Điểm nối cho Sprint tiếp theo (Handoff)
Hệ thống đã đạt đến độ chín muồi về mặt đảm bảo an toàn giao dịch tài chính - kho bãi (Financial Consistency + Concurrency). Sprint 14 (Cleanup + Documentation) sẽ là Sprint tổng kết, rà soát lại toàn bộ mã nguồn, dọn dẹp các thư viện/imports không dùng và hoàn thiện bức tranh tài liệu cho dự án ERP.
