---
name: sprint-docs-rule
description: Quy tắc bắt buộc về viết tài liệu kết thúc Sprint
trigger: always_on
---

# Agent Rules — Debug Protocol & Sprint Documentation

Quy tắc bắt buộc cho AI Agent (Gemini, Claude Code, v.v.) khi làm việc trong repo này.
Đặt file này ở đầu context (system prompt / `GEMINI.md` / `CLAUDE.md`) — không chèn giữa các tài liệu khác.

---

## PHẦN 1: QUY TẮC BẮT BUỘC — DỪNG - CHẨN ĐOÁN - MỚI ĐƯỢC SỬA (Circuit Breaker)

Áp dụng cho mọi lỗi build/runtime/test gặp phải trong quá trình code.

### 1.1. Đếm số lần fix liên tiếp cho CÙNG một lỗi/triệu chứng

- Nếu đã sửa code và chạy lại từ **2 lần trở lên** mà lỗi VẪN xuất hiện
  (dù thông báo lỗi khác đi chút ít, ví dụ đổi dòng số, đổi tên biến trong stacktrace),
  phải **NGAY LẬP TỨC dừng việc sửa code**.
- KHÔNG được viết thêm bất kỳ dòng code sửa lỗi nào cho tới khi hoàn thành mục 1.2.

### 1.2. Bắt buộc Root Cause Analysis (RCA) trước khi sửa lần thứ 3 trở đi

Khi dừng lại, phải trả lời đủ 5 mục sau bằng văn bản, KHÔNG được bỏ qua mục nào:

1. **Nguyên văn lỗi mới nhất** (copy chính xác, không diễn giải lại).
2. **Giả thuyết đã thử là gì, và tại sao nó KHÔNG đúng** — liệt kê từng lần fix trước
   kèm lý do fix đó thất bại. Nếu không biết lý do thất bại, phải nói rõ:
   "tôi không biết vì sao lần trước thất bại".
3. **Lỗi này nằm ở lớp nào?** Chọn đúng 1:
   `syntax` / `logic` / `dependency-version` / `config-env` / `data-state` /
   `race-condition` / `unknown`.
4. **Bằng chứng cụ thể** chứng minh giả thuyết mới (không phải suy đoán):
   - đọc lại đúng đoạn code liên quan (dán ra),
   - hoặc log/print thêm biến để xác nhận giá trị thực tế trước khi sửa,
   - hoặc đọc doc/version của lib đang dùng nếu nghi ngờ do version.
5. **Kế hoạch sửa (chỉ 1 thay đổi duy nhất)** — không được sửa nhiều chỗ cùng lúc
   "cho chắc". Mỗi lần chỉ test đúng 1 giả thuyết.

### 1.3. Giới hạn cứng: tối đa 3 vòng fix cho 1 lỗi

- Nếu sau 3 lần sửa mà lỗi vẫn còn, KHÔNG được thử lần thứ 4.
- Phải dừng hẳn, tóm tắt lại toàn bộ những gì đã thử/loại trừ được, và hỏi lại
  người dùng, kèm đề xuất 2-3 hướng khả dĩ khác nhau (ví dụ: "có thể do conflict
  version giữa A và B, cần bạn xác nhận version thật đang chạy").

### 1.4. Cấm các hành vi sau (dấu hiệu "sửa mù")

- Cấm sửa code mà KHÔNG trích dẫn lại đúng đoạn log lỗi mới nhất trong câu trả lời.
- Cấm dùng cụm "thử cách khác xem sao" mà không nêu rõ giả thuyết đang kiểm chứng.
- Cấm sửa nhiều file/nhiều đoạn code trong 1 lần chạy nếu chưa xác định rõ root
  cause (trừ khi nhiều chỗ đó rõ ràng cùng 1 nguyên nhân đã xác nhận).
- Cấm bỏ qua exception bằng try/catch rỗng hoặc comment code để "cho qua lỗi"
  nếu chưa hiểu lý do lỗi.

### 1.5. Khi lỗi liên quan đến version/dependency/library

- Bắt buộc phải tra cứu (đọc changelog/doc thật, hoặc hỏi người dùng version
  đang cài) TRƯỚC khi đoán API — đây là loại lỗi agent hay bịa (hallucinate)
  API không tồn tại.

---

## PHẦN 2: QUY TẮC BẮT BUỘC — DOCS KẾT THÚC SPRINT

Mỗi khi hoàn thành 1 sprint, PHẢI tạo file:

```
docs/sprints/sprint-<số>-<tên-ngắn>.md
```

Ví dụ: `docs/sprints/sprint-0.1-n-instance-rs256.md`

KHÔNG được coi sprint là "xong" nếu file này chưa tồn tại hoặc thiếu bất kỳ
mục bắt buộc nào bên dưới.

### 2.1. Nguyên tắc viết

- Viết cho **người đọc lần đầu**, không có context hội thoại đã dẫn tới quyết
  định đó. Không viết kiểu "như đã bàn ở trên" — phải tự giải thích lại từ đầu.
- Ưu tiên **sự thật kiểm chứng được** (đường dẫn file, tên class, lệnh chạy,
  kết quả test) hơn là mô tả cảm tính ("code sạch", "kiến trúc tốt").
- Cấm dùng tính từ mơ hồ để tự đánh giá chất lượng sprint ("xuất sắc",
  "hoàn hảo", "chuẩn"). Chỉ mô tả sự thật kiểm chứng được — docs là tài liệu
  kỹ thuật, không phải báo cáo PR-marketing.
- Không viết code chi tiết vào docs — chỉ trỏ đường dẫn file + giải thích
  **ý đồ (why)**, vì code sẽ tự thay đổi nhưng lý do thiết kế cần được giữ lại.

### 2.2. Cấu trúc bắt buộc (8 mục, không được bỏ mục nào)

#### 1. Mục tiêu Sprint (Goal)
- 1-3 câu: sprint này giải quyết bài toán/nhu cầu gì, tại sao làm bây giờ.
- Liên kết ngược: sprint này build trên sprint nào trước đó (nếu có).

#### 2. Thành quả đạt được (Done)
- Liệt kê dạng bullet, mỗi ý = 1 hạng mục kỹ thuật cụ thể đã hoàn thành.
- Với mỗi hạng mục: nêu rõ **nằm ở đâu** (file/class/module) để người đọc
  lần theo được.

#### 3. Quyết định kiến trúc & Lý do (Architecture Decisions — ADR rút gọn)
Mục **quan trọng nhất** — để sprint sau không "phá" quyết định cũ vì không
biết tại sao nó được chọn. Nếu không giải thích được LÝ DO BỎ phương án khác,
coi như mục này chưa đạt.

Với mỗi quyết định kỹ thuật quan trọng, nêu rõ:
- Phương án đã chọn
- 1-2 phương án đã cân nhắc nhưng bỏ, kèm lý do bỏ

Ví dụ format:
```
Quyết định: Dùng RS256 thay vì HS256 cho JWT.
Lý do: Branch cần xác thực offline mà không giữ khoá ký.
Đã cân nhắc: HS256 (đơn giản hơn) — bỏ vì khoá đối xứng phải nằm ở branch,
rủi ro leak.
```

#### 4. Hướng dẫn đọc code theo thứ tự (File Reading Guide)
- Liệt kê thứ tự file nên đọc để hiểu luồng, nhóm theo layer/phase.
- Với mỗi file: 1 câu nói rõ **nên chú ý điều gì** khi đọc (không phải tóm
  tắt cả file).

#### 5. Rủi ro / Nợ kỹ thuật đã biết (Known Risks & Tech Debt)
- Liệt kê mọi thứ đã cố tình làm tạm, làm rỗng (stub), hoặc biết là chưa
  tối ưu.
- Với mỗi mục: ảnh hưởng gì nếu không xử lý, và nên xử lý ở sprint nào.
- KHÔNG được giấu diếm để "sprint trông hoàn hảo" — mục này để sprint sau
  biết đường mà tránh bẫy, không phải để làm đẹp báo cáo.

#### 6. Việc chưa làm / Out of scope
- Nêu rõ ranh giới: cái gì được cân nhắc nhưng chủ động KHÔNG làm trong
  sprint này và vì sao (để tránh sprint sau hoặc người review hỏi "sao thiếu
  cái X").

#### 7. Cách chạy & Cách verify (Reproduce)
- Lệnh cụ thể để build/chạy/test lại toàn bộ thành quả sprint từ đầu
  (copy-paste chạy được, không diễn giải chung chung).
- Kết quả mong đợi khi chạy đúng (output mẫu, số lượng test pass, v.v.).

#### 8. Điểm nối cho Sprint tiếp theo (Handoff)
- 3-5 câu: sprint sau nên bắt đầu từ đâu, cần đọc gì trước, có phụ thuộc gì
  vào quyết định của sprint này cần nhớ.
- Nếu đã có kế hoạch sơ bộ cho sprint sau, ghi ngắn gọn ở đây (không cần
  chi tiết).

### 2.3. Quy tắc phụ

- **Timestamp**: đầu file ghi ngày hoàn thành + tên/mã sprint.
- Mỗi file docs sprint đứng **độc lập** — không được yêu cầu người đọc phải
  đọc docs sprint trước đó mới hiểu (được phép link tới, nhưng không được
  bắt buộc).
- Sau khi tạo xong, PHẢI thêm dòng liên kết vào `docs/sprints/README.md`
  (index), gồm: số sprint, tên, ngày, 1 dòng tóm tắt — để có cái nhìn tổng
  toàn dự án mà không cần mở từng file.
- Nếu sprint có thay đổi hành vi/API so với sprint trước (breaking change),
  PHẢI note rõ trong mục 3 hoặc mục 6, đánh dấu bằng `⚠️ BREAKING`.

### 2.4. Checklist tự kiểm trước khi báo "sprint xong"

Trước khi báo cáo hoàn thành sprint, agent phải tự liệt kê lại và đánh dấu
✅/❌ cho từng mục:

- [ ] 1. Mục tiêu Sprint
- [ ] 2. Thành quả đạt được (có trỏ đường dẫn file cụ thể)
- [ ] 3. Quyết định kiến trúc & Lý do (có nêu phương án đã bỏ + lý do)
- [ ] 4. Hướng dẫn đọc code theo thứ tự
- [ ] 5. Rủi ro / Nợ kỹ thuật đã biết
- [ ] 6. Việc chưa làm / Out of scope
- [ ] 7. Cách chạy & Cách verify
- [ ] 8. Điểm nối cho Sprint tiếp theo
- [ ] Đã thêm link vào `docs/sprints/README.md`
- [ ] Không có tính từ mơ hồ tự đánh giá chất lượng

Nếu bất kỳ mục nào ❌, chưa được coi là hoàn thành sprint.
