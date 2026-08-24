---
name: strict-debugging-circuit-breaker
description: Quy tắc bắt buộc về quá trình debug khi code gặp lỗi (Dừng - Chẩn đoán - Mới được sửa)
trigger: always_on
---

## QUY TẮC BẮT BUỘC: DỪNG - CHẨN ĐOÁN - MỚI ĐƯỢC SỬA (Circuit Breaker)

### 1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng
- Nếu bạn đã sửa code và chạy lại từ 2 lần trở lên mà lỗi VẪN xuất hiện
  (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace),
  bạn phải NGAY LẬP TỨC dừng việc sửa code.
- KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành Bước 2.

### 2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi
Khi dừng lại, bạn PHẢI trả lời đủ 5 mục sau bằng văn bản, KHÔNG được bỏ qua mục nào:

1. **Nguyên văn lỗi mới nhất** (copy chính xác, không diễn giải lại).
2. **Giả thuyết đã thử là gì, và tại sao nó KHÔNG đúng** (liệt kê từng lần fix trước
   + lý do fix đó thất bại — nếu không biết lý do thất bại, phải nói rõ "tôi không biết vì sao lần trước thất bại").
3. **Lỗi này nằm ở lớp nào?** Chọn đúng 1: `syntax` / `logic` / `dependency-version`
   / `config-env` / `data-state` / `race-condition` / `unknown`.
4. **Bằng chứng cụ thể** chứng minh giả thuyết mới (không phải suy đoán):
   - đọc lại đúng đoạn code liên quan (dán ra),
   - hoặc log/print thêm biến để xác nhận giá trị thực tế trước khi sửa,
   - hoặc đọc doc/version của lib đang dùng nếu nghi ngờ do version.
5. **Kế hoạch sửa (chỉ 1 thay đổi duy nhất)** — không được sửa nhiều chỗ cùng lúc
   "cho chắc". Mỗi lần chỉ test đúng 1 giả thuyết.

### 3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi
- Nếu sau 3 lần sửa mà lỗi vẫn còn, KHÔNG được thử lần thứ 4.
- Phải dừng hẳn, tóm tắt lại toàn bộ những gì đã thử/loại trừ được,
  và hỏi lại người dùng, kèm đề xuất 2-3 hướng khả dĩ khác nhau
  (ví dụ: "có thể do conflict version giữa A và B, cần bạn xác nhận version thật đang chạy").

### 4. Cấm các hành vi sau (dấu hiệu "sửa mù")
- Cấm sửa code mà KHÔNG trích dẫn lại đúng đoạn log lỗi mới nhất trong câu trả lời.
- Cấm dùng cụm "thử cách khác xem sao" mà không nêu rõ giả thuyết đang kiểm chứng là gì.
- Cấm sửa nhiều file/nhiều đoạn code trong 1 lần chạy nếu chưa xác định rõ root cause
  (trừ khi nhiều chỗ đó rõ ràng cùng 1 nguyên nhân đã xác nhận).
- Cấm bỏ qua exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi"
  nếu chưa hiểu lý do lỗi.

### 5. Khi lỗi liên quan đến version/dependency/library
- Bắt buộc phải tra cứu (đọc changelog/doc thật, hoặc hỏi người dùng version đang cài)
  TRƯỚC khi đoán API — vì đây là loại lỗi agent hay bịa (hallucinate) API không tồn tại.
