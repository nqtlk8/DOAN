# Sprint 37 - 2026-09-17

### 1. Mục tiêu Sprint (Goal)
- Sửa workflow tạo Đơn bán hàng (Sales Invoice): nút "Lưu" trên form trước đây chỉ tạo bản DRAFT (không trừ tồn kho, không cộng công nợ) và cần một bước "Xác nhận" riêng để áp dụng thay đổi. Theo yêu cầu người dùng: loại bỏ bước Draft trung gian, bấm "Lưu" phải trừ tồn kho + cộng công nợ ngay lập tức, atomic (không để lộ hoá đơn "mồ côi" ở trạng thái Draft nếu có lỗi giữa chừng).

### 2. Thành quả đạt được (Done)
- Backend: thêm `SalesInvoiceService.createAndConfirm(dto, branchId, userId)` — tạo hoá đơn và áp dụng toàn bộ hiệu ứng xác nhận (trừ tồn kho theo cost snapshot, cộng công nợ, trừ công nợ nếu có trả trước, set status = CONFIRMED) trong CÙNG MỘT giao dịch `@Transactional`. Không còn khoảng hở giữa "tạo" và "xác nhận" như thiết kế 2 bước cũ.
- Refactor: tách logic dùng chung giữa `confirmInvoice` (2 bước cũ, vẫn giữ để tương thích ngược) và `createAndConfirm` vào 2 hàm private `buildInvoice()` và `applyConfirmationEffects()` để tránh trùng lặp code.
- `SalesInvoiceController`: endpoint `POST /api/v1/sales-invoices` (nút Lưu gọi tới) nay gọi `createAndConfirm` thay vì `createDraft`. Endpoint `POST /api/v1/sales-invoices/{id}/confirm` vẫn giữ nguyên (dùng cho hoá đơn Draft cũ/legacy hoặc nút "Xác nhận" thủ công trên ribbon).
- Frontend (`SalesOrderForm.tsx`): bỏ đoạn code gọi 2 API tuần tự (create rồi confirm) đang tồn tại dở dang, chưa commit trong working tree — đoạn này bị lỗi vì bất kỳ trục trặc nào giữa 2 lần gọi (mất mạng, đóng tab, lỗi server tạm thời...) sẽ để lại hoá đơn Draft mồ côi, đúng triệu chứng người dùng report; đoạn code đó cũng phá vỡ hợp đồng mà bộ test Playwright `sales-create.spec.ts` đã viết sẵn từ trước (chỉ mock đúng 1 request `POST /sales-invoices`, không mock `/confirm`). Nay chỉ còn 1 lệnh gọi API duy nhất khi bấm Lưu.
- Thêm field `note` bị thiếu vào payload gửi lên khi Lưu (state `note` đã có sẵn trên form nhưng trước đây không được gửi trong request).
- Nút "Xác nhận" (ribbon, dùng cho hoá đơn Draft cũ) nay báo lỗi thân thiện thay vì gọi API và nhận lỗi chung chung, nếu hoá đơn hiện tại đã ở trạng thái CONFIRMED rồi.
- Thêm unit test `SalesInvoiceServiceTest#createAndConfirm_ShouldPersistAlreadyConfirmed_DeductInventoryAndIncreaseDebtInOneStep`: verify chỉ có 2 lần gọi `invoiceRepository.save()` (1 lần để lấy id, 1 lần lưu trạng thái CONFIRMED), không có bản DRAFT nào "lộ" ra ngoài transaction.

### 3. Quyết định kiến trúc & Lý do (ADR rút gọn)
- **Quyết định:** Gộp create + confirm thành 1 method `@Transactional` duy nhất ở backend, thay vì để Frontend gọi 2 API tuần tự.
- **Lý do:** Nếu tách 2 lời gọi HTTP riêng biệt (2 transaction riêng), luôn tồn tại 1 khoảng hở giữa 2 request nơi hoá đơn đã tồn tại ở DB dưới dạng DRAFT nhưng chưa Confirm — nếu request thứ 2 thất bại thì hoá đơn "mồ côi" này tồn tại vĩnh viễn ở trạng thái Draft. Gộp vào 1 giao dịch DB duy nhất loại bỏ hoàn toàn khoảng hở này: hoặc thành công toàn bộ (đã trừ kho + cộng nợ), hoặc rollback toàn bộ (không tạo ra bất kỳ dòng nào).
- **Đã cân nhắc:** Giữ nguyên 2 API riêng (create/confirm) và chỉ sửa Frontend để retry gọi confirm khi thất bại. Bỏ vì retry ở Frontend không giải quyết được trường hợp mất kết nối vĩnh viễn (đóng trình duyệt giữa chừng), và vẫn để lộ trạng thái Draft không mong muốn ra ngoài dù chỉ trong khoảnh khắc.
- **Quyết định:** KHÔNG xoá `createDraft()`/`confirmInvoice()` (luồng 2 bước cũ).
- **Lý do:** Các test integration khác (`DebtReconciliationIntegrationTest`, v.v.) vẫn dùng trực tiếp 2 method này làm test helper; nút "Xác nhận" trên ribbon UI (dùng khi sửa 1 hoá đơn Draft cũ có sẵn trong DB) cũng vẫn cần endpoint `/confirm`.

### 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
1. `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/order/application/SalesInvoiceService.java` — đọc `buildInvoice()`, `applyConfirmationEffects()`, rồi `createAndConfirm()` để thấy toàn bộ luồng atomic mới.
2. `code/erp-platform/services/erp-backend/src/main/java/com/storename/erp/order/api/SalesInvoiceController.java` — endpoint `POST /` nay gọi `createAndConfirm`.
3. `code/erp-platform/apps/erp-frontend/src/components/sales/SalesOrderForm.tsx` — hàm `handleSubmit` (chỉ còn 1 lệnh gọi API) và `handleConfirm` (chặn gọi lại nếu đã CONFIRMED).
4. `code/erp-platform/services/erp-backend/src/test/java/com/storename/erp/order/application/SalesInvoiceServiceTest.java` — test mới `createAndConfirm_...`.

### 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Chưa build/test được trong sandbox của phiên làm việc này: máy thực thi lệnh ở đây chỉ có JDK 11 (project yêu cầu JDK 21) và không có mạng để tải Maven Wrapper; symlink npm workspace (`@erp/api-contract`) không đọc được khi truy cập từ môi trường Linux khác gốc Windows nên `tsc`/`vitest`/`playwright` cũng không chạy được từ đây (chỉ soát cú pháp bằng `esbuild` và kiểm tra dấu ngoặc cân bằng). **Cần tự chạy lại `mvnw test` và `npm run test` / `npx playwright test` trên máy thật (JDK 21, Node đã cài qua Windows) để xác nhận trước khi merge.**
- Hoá đơn Draft đã tồn tại từ trước sprint này (nếu có, do bug cũ) sẽ KHÔNG tự động được confirm — cần rà soát dữ liệu thật và xác nhận thủ công qua nút "Xác nhận", hoặc viết 1 script migrate riêng nếu cần dọn dẹp.
- Chưa thêm field `status` vào `SalesInvoiceCreateResponseDto`/OpenAPI contract để Frontend biết chắc trạng thái trả về thay vì giả định luôn là CONFIRMED sau khi Lưu — vì regenerate OpenAPI contract cần chạy backend thật. Rủi ro thấp ở hiện tại vì endpoint tạo mới luôn confirm ngay, nhưng nếu sau này backend đổi lại logic mà quên đồng bộ, Frontend sẽ set sai trạng thái.

### 6. Việc chưa làm / Out of scope
- Chưa hiển thị badge trạng thái (Draft/Đã xác nhận) trên UI form — `invoiceStatus` hiện chỉ dùng nội bộ để chặn double-confirm.
- Chưa xử lý trường hợp tồn kho âm (oversell): theo test có sẵn (`confirmInvoice_exceedsStock`), hệ thống cho phép bán vượt tồn kho (âm kho) — giữ nguyên hành vi cũ, không nằm trong phạm vi sprint này.
- Chưa migrate dữ liệu Draft cũ (nếu có) trong DB thật.

### 7. Cách chạy & Cách verify (Reproduce)
- Verify Backend (chạy trên máy có JDK 21):
  `cd code/erp-platform/services/erp-backend && .\mvnw test -Dtest="SalesInvoiceServiceTest,SalesInvoiceIntegrationTest,DebtReconciliationIntegrationTest"`
  Kỳ vọng: BUILD SUCCESS, bao gồm test mới `createAndConfirm_ShouldPersistAlreadyConfirmed_DeductInventoryAndIncreaseDebtInOneStep`.
- Verify Frontend:
  `cd code/erp-platform/apps/erp-frontend && npx playwright test tests/sales-create.spec.ts`
  Kỳ vọng: các test cũ (đặc biệt TC-SALE-38 & 39) vẫn pass, vì nay code chỉ gọi đúng 1 API giống hợp đồng test đã viết sẵn.
- Verify thủ công (khuyến nghị nhất): mở app, vào Bán hàng > Thêm mới, chọn khách hàng, thêm ít nhất 1 sản phẩm, bấm Lưu — kiểm tra ngay trong màn Tồn kho (đã trừ đúng số lượng) và Công nợ khách hàng (đã cộng đúng số tiền), không cần bấm thêm nút Xác nhận nào khác.

### 8. Điểm nối cho Sprint tiếp theo (Handoff)
- Nên bổ sung field `status` vào `SalesInvoiceCreateResponseDto` + regenerate OpenAPI contract để Frontend không phải giả định trạng thái CONFIRMED sau khi Lưu.
- Nên rà soát toàn bộ hoá đơn đang ở trạng thái DRAFT trong DB thật (nếu có) để quyết định confirm thủ công hoặc huỷ.
- Cân nhắc thêm badge trạng thái (Draft/Đã xác nhận) hiển thị trên form để người dùng luôn biết rõ trạng thái hoá đơn đang xem.
