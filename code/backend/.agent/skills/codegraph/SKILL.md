# SKILL: Sử dụng Codegraph bắt buộc

**Trigger:** Đọc và áp dụng skill này cho MỌI task viết/sửa code — không có ngoại
lệ, không được bỏ qua để "làm nhanh hơn".

---

## 1. Vì sao bắt buộc

Codegraph cho phép tra cứu class, method, dependency graph, caller/callee, symbol
reference trong toàn bộ codebase. Bỏ qua bước tra cứu này là nguyên nhân phổ biến
nhất dẫn tới: code trùng lặp (vi phạm SRP/DRY), breaking change không được phát
hiện, vi phạm ranh giới module.

## 2. Quy tắc BẮT BUỘC — trước khi viết code mới

Trước khi tạo entity/service/interface/DTO mới:

1. Tìm xem đã tồn tại class/interface/DTO nào cùng chức năng hoặc tên tương tự
   chưa (tránh trùng lặp).
2. Tìm các implementation hiện có của interface liên quan (VD: các Strategy đã
   có) để giữ đúng convention đặt tên và cấu trúc.
3. Tìm Entity liên quan và các Repository/Service đang dùng Entity đó, để hiểu rõ
   ranh giới module trước khi thêm field/quan hệ mới.

## 3. Quy tắc BẮT BUỘC — trước khi sửa code đã tồn tại

Trước khi sửa 1 method/class đã tồn tại:

1. Tra cứu **callers** (ai đang gọi hàm này) và **callees** (hàm này gọi gì) để
   đánh giá phạm vi ảnh hưởng (impact analysis).
2. Nếu method được gọi từ ≥ 2 module khác nhau → PHẢI báo cáo rõ trong tóm tắt
   thay đổi, không tự ý sửa breaking change mà không nêu rõ.
3. Kiểm tra method có đang được test (unit/black-box) tham chiếu tới không, để
   không làm vỡ test hiện có mà không cập nhật.

## 4. Quy tắc BẮT BUỘC — trước khi tạo module/package mới

1. Xem sơ đồ dependency giữa các module hiện có, đảm bảo module mới không tạo
   circular dependency.
2. Xác nhận module mới chỉ phụ thuộc vào `xxx.api` (Facade) của module khác,
   không import trực tiếp `domain`/`infrastructure` của module khác.

## 5. Quy tắc BẮT BUỘC — sau khi hoàn thành task

Trước khi báo cáo hoàn thành, dùng Codegraph tự kiểm tra:

1. Có Entity JPA nào bị leak ra Controller/API response không (tìm caller trả
   trực tiếp Entity thay vì DTO).
2. Có đoạn code trùng lặp logic (duplicate) với chỗ khác trong hệ thống không.
3. Có vi phạm quy tắc layer (Controller gọi thẳng Repository, bỏ qua Service)
   không.
4. Mọi Repository truy vấn bảng có `branch_id` có áp dụng `@BranchScoped` hay
   không (xem skill `08-branch-scope-multitenancy`).

## 6. Định dạng báo cáo bắt buộc sau mỗi task

```
1. File đã tạo/sửa: [danh sách]
2. Kết quả tra cứu Codegraph quan trọng: [phát hiện đáng chú ý, nếu có]
3. Test case đã viết + kết quả chạy: [...]
4. Document đã cập nhật: [README module / OpenAPI / ADR / CHANGELOG]
5. Rủi ro/giả định cần người review xác nhận: [...]
```
