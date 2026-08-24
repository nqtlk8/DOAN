# Skill: Sử dụng CodeGraph (dành cho Gemini Pro / Gemini CLI)

## Tóm tắt
CodeGraph là một MCP server + CLI local, dựng sẵn một **knowledge graph** của toàn bộ codebase (symbol, call edge, import, kế thừa, route framework...) bằng tree-sitter, lưu trong SQLite (`.codegraph/codegraph.db`). Thay vì agent phải tự dò cấu trúc code bằng vòng lặp `grep → glob → read`, CodeGraph trả về **trong một lần gọi**: source code liên quan (đã trích sẵn, không cần đọc lại file), call path giữa các symbol (kể cả các hop dynamic-dispatch mà grep không theo được), và **blast radius** (phạm vi ảnh hưởng khi sửa một symbol). Kết quả: ít tool-call hơn, trả lời nhanh hơn, và ở codebase lớn còn tiết kiệm token/chi phí.

Gemini CLI nằm trong danh sách agent được CodeGraph hỗ trợ chính thức (`codegraph install` tự nhận diện và cấu hình Gemini CLI, ghi hướng dẫn vào `GEMINI.md`).

## Khi nào NÊN dùng CodeGraph

Ưu tiên CodeGraph thay vì grep/glob/đọc file thủ công khi câu hỏi của người dùng mang tính **cấu trúc / quan hệ code**, ví dụ:

- "Hàm/class X hoạt động như thế nào?" (hiểu logic một symbol)
- "Request đi từ đâu đến database?" / "X gọi tới Y bằng cách nào?" (trace luồng gọi)
- "Nếu tôi sửa hàm X thì những gì bị ảnh hưởng?" (blast radius / impact analysis)
- "Ai gọi hàm này?" / "Hàm này gọi những gì?" (callers/callees)
- Khảo sát một khu vực code chưa quen ("cho tôi biết module auth được tổ chức ra sao")
- Trước khi refactor, xóa, hoặc đổi signature một symbol — cần biết ai phụ thuộc vào nó
- Tìm test nào bị ảnh hưởng bởi các file vừa thay đổi (trước khi chạy CI/test)

## Khi nào KHÔNG cần / không nên dùng

- Câu hỏi không liên quan đến cấu trúc code (viết văn bản, giải thích khái niệm chung, câu hỏi một dòng không cần ngữ cảnh repo).
- Thư mục hiện tại **chưa có `.codegraph/`** (chưa `init`) — lúc này quay lại công cụ built-in (đọc/tìm file thủ công) và có thể gợi ý người dùng chạy `codegraph init` nếu việc này sẽ lặp lại nhiều lần.
- Đã có kết quả `codegraph_explore` cho đúng câu hỏi trong phiên hiện tại và code chưa đổi — đừng gọi lại, dùng lại kết quả cũ (đọc thẳng source đã trả về, không cần đọc file lần nữa).
- Sau khi CodeGraph trả kết quả, **đừng "double-check" bằng grep** — hãy tin kết quả trừ khi banner cảnh báo "stale" (dữ liệu cũ) xuất hiện.

## Cách dùng — 2 cơ chế song song

### 1) Qua MCP (cách mặc định khi Gemini CLI đã kết nối CodeGraph MCP server)
Chỉ có **một tool chính** được expose mặc định: `codegraph_explore`. Đây gần như là công cụ duy nhất bạn cần cho hầu hết câu hỏi cấu trúc:

- Gọi `codegraph_explore` với một câu hỏi tự nhiên hoặc tên symbol/file, ví dụ:
  - `codegraph_explore("how does a request reach the database")`
  - `codegraph_explore("UserService.login")`
- Nếu cần chỉ định đúng project (monorepo nhiều service, hoặc project thứ hai), truyền thêm `projectPath`.
- Kết quả trả về gồm: source verbatim của các symbol liên quan (nhóm theo file), call path giữa chúng, và tóm tắt blast radius. **Coi như đã "đọc" các file này** — không cần gọi thêm tool đọc file cho cùng nội dung.
- Nếu nêu tên một file/symbol cụ thể trong câu hỏi, kết quả sẽ trả về source có đánh số dòng của đúng đối tượng đó (tương đương tool Read).

Các tool con khác (`codegraph_node`, `codegraph_search`, `codegraph_callers`, `codegraph_callees`, `codegraph_impact`, `codegraph_files`, `codegraph_status`) vẫn hoạt động nhưng **ẩn mặc định** vì thông tin của chúng đã nằm trong `codegraph_explore`. Chỉ cần bật lại nếu thật sự cần một truy vấn hẹp (ví dụ chỉ muốn danh sách callers, không cần source đầy đủ) bằng biến môi trường `CODEGRAPH_MCP_TOOLS=explore,node,search,callers,...`.

### 2) Qua CLI (dùng khi không có MCP, viết script, hook CI, hoặc cần output máy đọc được)
```bash
codegraph status [path]              # kiểm tra project đã index chưa, thống kê nhanh
codegraph explore <query>            # giống hệt codegraph_explore, dùng trong terminal
codegraph node <symbol|file>         # source + callers của 1 symbol, hoặc đọc file có số dòng
codegraph query <search>             # tìm symbol theo tên (--kind, --limit, --json)
codegraph callers <symbol>           # ai gọi symbol này
codegraph callees <symbol>           # symbol này gọi gì
codegraph impact <symbol>            # phân tích ảnh hưởng khi đổi symbol (--depth, --json)
codegraph affected [files...]        # tìm test bị ảnh hưởng bởi các file đã đổi (--stdin để pipe từ git diff)
codegraph files [path]               # xem cấu trúc thư mục/file (--format, --filter, --json)
codegraph sync [path]                # đồng bộ tăng dần thủ công (thường không cần, auto-sync đã bật)
```

Ví dụ thực tế cho quy trình review trước khi commit:
```bash
git diff --name-only HEAD | codegraph affected --stdin --quiet
```

## Điều kiện tiên quyết
1. CodeGraph CLI đã cài (`codegraph` có trong PATH).
2. Đã chạy `codegraph install` một lần để nối CodeGraph vào Gemini CLI (ghi MCP server config + đoạn hướng dẫn vào `GEMINI.md`).
3. Với **mỗi project**, phải chạy `codegraph init` (hoặc ít nhất `codegraph index`) trong thư mục project để build graph lần đầu — nếu chưa có `.codegraph/`, các câu hỏi cấu trúc sẽ không có gì để trả lời.
4. Sau `init`, graph **tự động đồng bộ** khi file thay đổi (watcher debounce 2 giây) — không cần chạy lại thủ công trừ khi gặp lỗi.

## Quy tắc thực hành tốt
- **Luôn thử CodeGraph trước** grep/glob khi câu hỏi mang tính cấu trúc — đây là điểm mạnh cốt lõi (ít tool-call hơn, câu trả lời chính xác hơn vì có resolution qua nhiều file, kể cả dynamic dispatch).
- **Không lặp lại công việc**: nếu `codegraph_explore` đã trả về source của một file, không cần đọc lại file đó bằng tool khác.
- **Tin kết quả** trừ khi có banner báo dữ liệu cũ ("stale") — khi đó gọi `codegraph sync` (CLI) hoặc đợi vài giây rồi thử lại (MCP tự sync).
- Với monorepo, chỉ định đúng `projectPath`/thư mục con đã được `init` — một path chưa index sẽ trả lời rằng nên dùng công cụ built-in thay thế.
- Trước khi xóa/đổi signature một hàm quan trọng, luôn chạy một truy vấn kiểu impact/blast-radius trước.

## Xử lý sự cố nhanh
- **"CodeGraph not initialized"** → chạy `codegraph init` trong thư mục project.
- **Thiếu symbol mới thêm** → đợi vài giây (auto-sync) hoặc chạy `codegraph sync` thủ công; kiểm tra file có bị `.gitignore` hoặc nằm trong thư mục loại trừ mặc định (`node_modules`, `dist`...) không.
- **MCP không kết nối được** → kiểm tra `codegraph status`, sau đó chạy lại `codegraph install` để ghi lại cấu hình.
