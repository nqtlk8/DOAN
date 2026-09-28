# Kiểm duyệt UI/UX erp-frontend — Bảng câu hỏi

> **Cách trả lời:** điền vào dòng `Trả lời:` dưới mỗi câu. Có thể ghi chữ cái của phương án (vd `B`), ghi `Đề xuất` để lấy phương án được đánh dấu ✅, hoặc ghi ý của bạn. Câu nào bỏ trống, mình sẽ dùng phương án ✅.
>
> Ngày kiểm tra: 27/09/2026 — Phạm vi: `apps/erp-frontend/src`

---

## 0. Những gì mình tìm thấy khi kiểm tra code (đọc trước khi trả lời)

**Phát hiện quan trọng nhất — nguyên nhân gốc của phần lớn lỗi giao diện:**
`src/index.css` đang dùng cú pháp Tailwind v3 (`@tailwind base; @tailwind components; @tailwind utilities;`), trong khi dự án cài **Tailwind v4.3.3**. Ở v4, hai dòng `base` và `components` không còn tác dụng, nên **toàn bộ theme mặc định của Tailwind không được nạp**. Mình đã kiểm chứng bằng file build `dist/assets/index-*.css` (chỉ 18 KB) và build thử lại:

- Các class như `bg-white`, `p-4`, `text-sm`, `rounded-lg`, `bg-teal-600`, `text-slate-500`, `shadow-lg`, `max-w-7xl`, `gap-6`… **không sinh ra CSS**.
- Chỉ các token tự định nghĩa (`bg-erp-*`, `h-erp-input-height`…) và vài class không cần theme (`z-50`, `w-full`) là hoạt động.

Hệ quả trực tiếp:

| Triệu chứng bạn thấy | Nguyên nhân trong code |
|---|---|
| Dropdown tìm kiếm bị đè, không nhìn thấy để chọn | Khung gợi ý dùng `bg-white`, `shadow-lg`, `max-h-64` → **không có nền, không có bóng, không giới hạn chiều cao**, nên chữ gợi ý chồng lên nội dung phía sau. Ngoài ra khung gợi ý bị các vùng `overflow-auto` (bảng hàng hóa, header form, MDI, MainContent) **cắt mất**. |
| Màn hình login không đẹp | Toàn bộ `bg-slate-900`, `rounded-2xl`, `p-8`, `bg-teal-600`… không render; thêm 3 đốm màu teal/tím/hồng (trái với DESIGN_SYSTEM.md: tránh gradient tím/hồng). |
| Dashboard lộn xộn | Lưới `xl:grid-cols-4`, `gap-6`, card `rounded-2xl p-6`… không render → các khối dính nhau, không có khoảng cách. |
| Màu sắc không thống nhất | App đang có **3 ngôn ngữ thiết kế trộn nhau**: kiểu kế toán desktop (Tahoma 12px, nền `#E4EBF4`, dòng chọn `#316AC5`) ở form/ribbon; kiểu hiện đại slate/teal ở danh sách, combobox, login; kiểu indigo/emerald/cầu vồng ở dashboard. DESIGN_SYSTEM.md lại ghi màu chính teal `#0D9488` + font Inter — không khớp với code. |

**Các vấn đề khác đã ghi nhận:**

1. `MainContent` bọc mọi tab bằng `max-w-7xl p-6` → form nghiệp vụ (min-width 1024px) bị bó hẹp và cuộn lồng nhau.
2. `GenericDocumentForm`: 3 khối tỷ lệ `1.2 : 2 : 1` với min-width 300/400/280px; header có `overflow-x-auto`; "Nhân viên" và "Người lập" hiển thị cùng một giá trị; cột "Chiết khấu" dòng luôn = 0 (chỉ đọc); cột "Ghi chú" dòng không lưu vào đâu; tất cả số tiền đều tô đỏ; ô số dùng `type="number"` nên không có dấu phân cách hàng nghìn khi nhập.
3. `SearchableCombobox`: gọi API ở **mỗi phím gõ** (không debounce); kiểu ô input (bo góc, `text-sm`, `py-1`) khác hẳn các ô ERP 24px nên lệch chiều cao dòng bảng 22px.
4. `TabBar` dùng class `text-primary` / `border-b-primary` nhưng chưa định nghĩa màu `primary` → tab đang mở không có màu nhấn.
5. Ribbon: nút "Xuất Trả Hàng Mua" không có hành động; "Đăng xuất" bị giấu trong tab "Hệ thống"; không hiển thị người đang đăng nhập / chi nhánh.
6. Dashboard: không có khối cảnh báo tồn kho; danh sách "Chi tiết sản phẩm" lặp lại dữ liệu của biểu đồ; biểu đồ 2 trục + 5 màu cầu vồng khó đọc.
7. **Backend đã có sẵn** bảng `inventory_alert_config` (ngưỡng tồn tối thiểu theo sản phẩm + chi nhánh) và job `LowStockAlertJob` chạy 15 phút/lần, **nhưng chưa có API nào** để frontend đọc ngưỡng hay danh sách cảnh báo. API tồn kho `/api/v1/inventory/stock` chỉ trả `quantity`, không có ngưỡng.
8. Rác trong thư mục frontend: `App.css` (CSS mẫu của Vite, không dùng), hai file `MdiModuleLayout.tsx` khác nhau (`src/layout/` và `src/components/layout/`, chỉ bản sau được dùng), nhiều script tạm `apply_*.js`, `fix_*.js`, `repair*.js`, `compare.js`, `test.js`… ở gốc.

---

## A. Định hướng chung

### A1. Phong cách tổng thể
- **A.** Kế toán desktop cổ điển cho toàn bộ app (giống MISA / Fast / KiotViet bản PC): chữ nhỏ, ô vuông, bảng dày đặc.
- **B.** Hiện đại hóa toàn bộ (kiểu web SaaS): bo góc, card, khoảng trắng rộng.
- **C.** ✅ Lai: màn hình nghiệp vụ (form phiếu, bảng, danh mục) giữ **mật độ cao kiểu desktop** nhưng tinh chỉnh màu/viền cho dịu mắt; Login và Dashboard làm hiện đại hơn nhưng cùng bảng màu.

Trả lời:

### A2. Màu chủ đạo (primary)
Màu này dùng cho nút chính, tab đang mở, viền focus, dòng được chọn, tiêu đề bảng.
- **A.** ✅ Xanh dương doanh nghiệp `#1D4ED8` (đậm, tin cậy, gần với tông ribbon hiện tại, hợp phần mềm kế toán).
- **B.** Teal `#0D9488` (theo DESIGN_SYSTEM.md, hiện dùng ở login/combobox).
- **C.** Xanh navy `#1E3A5F` (trầm, nghiêm túc).
- **D.** Xanh lá `#15803D`.
- **E.** Mã màu khác / màu logo cửa hàng: ______

Trả lời:

### A3. Màu nền và ribbon
- **A.** ✅ Nền app xám rất nhạt `#F5F7FA`, vùng nhập liệu trắng, ribbon trắng với viền dưới mảnh. Ô nhập trắng, viền xám `#CBD5E1`, focus viền màu primary.
- **B.** Giữ tông xanh nhạt hiện tại (ribbon `#E4EBF4`, ô nhập nền xanh nhạt `#E2E9F4`) nhưng làm dịu viền.
- **C.** Khác: ______

Trả lời:

### A4. Màu cho số tiền
Hiện tại *mọi* số tiền (Tiền hàng, Thành tiền, Số lượng, Còn của đơn, Nợ tổng) đều **đỏ đậm** → mắt khó phân biệt cái nào là cảnh báo.
- **A.** ✅ Số tiền thường màu đen đậm; **tổng cần chú ý** (Tổng cộng, Còn phải thu) màu primary đậm; **chỉ nợ / số âm / vượt tồn** mới đỏ.
- **B.** Giữ đỏ như hiện tại.
- **C.** Khác: ______

Trả lời:

### A5. Font chữ
- **A.** ✅ `Segoe UI` (có sẵn trên Windows, không cần internet, hiển thị tiếng Việt tốt) với dự phòng `Inter, Roboto, Arial`.
- **B.** `Inter` từ Google Fonts (như DESIGN_SYSTEM.md) — cần internet hoặc tự đóng gói font vào dự án.
- **C.** Giữ `Tahoma`.

Trả lời:

Câu phụ: app có chạy trong mạng LAN không có internet không? (ảnh hưởng việc dùng Google Fonts) — Trả lời:

### A6. Mật độ / kích thước (áp dụng cho form và bảng)
| Mức | Cỡ chữ | Cao ô nhập | Cao dòng bảng |
|---|---|---|---|
| **A.** Chật (hiện tại) | 12px | 24px | 22px |
| **B.** ✅ Vừa | 13px | 28px | 28px |
| **C.** Thoáng | 14px | 32px | 34px |

Trả lời:

### A7. Độ phân giải màn hình mục tiêu
- **A.** ✅ Tối ưu cho 1366×768 và 1920×1080 (máy bàn / laptop văn phòng).
- **B.** Chỉ 1920×1080.
- **C.** Cần hỗ trợ cả tablet (≥ 768px).

Trả lời:

### A8. Chế độ tối (dark mode)
- **A.** ✅ Không làm (tập trung chất lượng giao diện sáng).
- **B.** Có, kèm nút chuyển.

Trả lời:

### A9. Sửa cấu hình Tailwind (nguyên nhân gốc ở mục 0)
Sửa `index.css` sang `@import "tailwindcss";` sẽ làm **hàng trăm class hiện đang "chết" bỗng hoạt động** → giao diện mọi màn hình thay đổi cùng lúc (có chỗ đẹp lên, có chỗ lộ ra khoảng cách/màu lạ cần chỉnh tiếp).
- **A.** ✅ Đồng ý sửa, sau đó rà lại từng màn hình.
- **B.** Không, chỉ dùng token `erp-*` tự định nghĩa và viết lại các class kia.

Trả lời:

### A10. Dọn dẹp file thừa
Xóa (hoặc chuyển vào `_to_delete/`) `App.css`, `src/layout/MdiModuleLayout.tsx` (bản không dùng), các script tạm `apply_*.js`, `fix_*.js`, `repair*.js`, `rebuild_final_form.js`, `build_ribbon.js`, `ui_ux_promax.js`, `compare.js`, `test.js`, `fix_tests.js`; cập nhật lại DESIGN_SYSTEM.md theo lựa chọn mới.
- **A.** ✅ Đồng ý (chuyển vào `_to_delete/` để bạn tự xóa).
- **B.** Không đụng tới.
- **C.** Chỉ cập nhật DESIGN_SYSTEM.md.

Trả lời:

---

## B. GenericDocumentForm (form phiếu bán hàng / nhập hàng)

### B1. Bố cục phần đầu phiếu
Hiện tại: 3 khối ngang *Thông tin phiếu | Khách hàng | Tài chính* tỷ lệ `1.2 : 2 : 1`.
- **A.** ✅ 3 khối tỷ lệ **`1 : 1.5 : 1`**, khối giữa (khách hàng) rộng nhất vì có địa chỉ, ghi chú dài; bỏ min-width cứng để co giãn theo màn hình.
- **B.** 3 khối bằng nhau `1 : 1 : 1`.
- **C.** 2 khối trên (*Thông tin phiếu* | *Khách hàng*), còn khối *Tài chính* chuyển xuống **thanh tổng kết phía dưới bảng** hàng hóa (giống hóa đơn giấy: tiền hàng → chiết khấu → VAT → trả trước → còn lại).
- **D.** Khác: ______

Trả lời:

### B2. Nhãn (label) của ô nhập
- **A.** ✅ Nhãn bên trái, căn phải, rộng cố định **96px**, màu xám đậm `#475569`; ô bắt buộc có dấu `*` đỏ.
- **B.** Nhãn nằm trên ô nhập (tốn chiều cao hơn, dễ đọc hơn).
- **C.** Giữ như cũ (80–90px).

Trả lời:

### B3. Các ô chỉ đọc (Số phiếu, Lấy giá, Kho xuất, Nhân viên, Người lập)
Hiện là ô xám có viền, trông như bị vô hiệu hóa. "Nhân viên" và "Người lập" đang hiện **cùng một giá trị**.
- **A.** ✅ Hiển thị dạng chữ thường không viền (nền trong suốt), gộp "Nhân viên / Người lập" thành một dòng; "Số phiếu" hiển thị nổi bật ở góc tiêu đề.
- **B.** Giữ ô xám nhưng làm nhạt viền.
- **C.** Giữ nguyên.

Trả lời:

Câu phụ: "Nhân viên" (người bán) và "Người lập" (người nhập phiếu) có thể **khác nhau** không? Nếu có, "Nhân viên" có cần chọn được từ danh sách không? — Trả lời:

### B4. Các cột của bảng hàng hóa
Hiện tại: STT | Hàng hóa | Số lượng | Đơn giá | Chiết khấu | Thành tiền | Ghi chú | 🗑
- **A.** ✅ STT (40px) | **Mã hàng** (110px) | Tên hàng (còn lại, tự giãn) | **ĐVT** (70px) | **Tồn** (80px, chỉ đọc, đỏ nếu SL bán > tồn) | Số lượng (90px) | Đơn giá (120px) | Thành tiền (130px) | 🗑 (32px). Bỏ cột Chiết khấu và Ghi chú dòng (đang không hoạt động).
- **B.** Như A nhưng **giữ** cột Chiết khấu dòng và Ghi chú dòng, và làm cho chúng hoạt động thật (cần kiểm tra backend có nhận hai trường này không).
- **C.** Giữ nguyên cột, chỉ chỉnh độ rộng.
- **D.** Khác: ______

Trả lời:

### B5. Dòng đang được chọn trong bảng
Hiện tại: tô **xanh đậm `#316AC5`, chữ trắng** toàn dòng (kiểu Windows XP) — nặng mắt khi nhập liệu.
- **A.** ✅ Nền primary nhạt (~8%) + vạch màu primary 3px bên trái, chữ giữ màu thường.
- **B.** Giữ nền đậm chữ trắng.

Trả lời:

### B6. Nhập số tiền / số lượng
- **A.** ✅ Hiển thị có dấu phân cách hàng nghìn kiểu Việt Nam (`1.500.000`) cả khi đang nhập, căn phải; tự chọn toàn bộ số khi focus để gõ đè.
- **B.** Giữ ô `number` mặc định (không có dấu phân cách khi nhập).

Trả lời:

### B7. Luồng bàn phím khi nhập dòng hàng
- **A.** ✅ Chọn hàng xong → con trỏ tự nhảy sang ô **Số lượng**; Enter ở ô Số lượng → sang Đơn giá; Enter ở ô cuối → **tự thêm dòng mới** và focus ô Hàng hóa. Giữ phím tắt F2/F3/F4/F7/F8/F9/F12 như hiện tại.
- **B.** Chỉ dùng chuột + Tab như hiện tại.

Trả lời:

### B8. Phạm vi áp dụng
`GenericDocumentForm` hiện chỉ dùng cho **Phiếu bán hàng**. Phiếu nhập hàng (`InboundReceiptModule`) và Nhập lại hàng bán (`GoodsReturnModule`, mới có 10 dòng) đang làm riêng.
- **A.** ✅ Chỉ làm đẹp/sửa `GenericDocumentForm` + phiếu bán hàng trong đợt này; phiếu nhập áp dụng cùng bộ màu/kích thước nhưng không đổi cấu trúc.
- **B.** Chuyển luôn phiếu nhập hàng sang dùng `GenericDocumentForm`.
- **C.** Làm cả phiếu nhập và hoàn thiện Nhập lại hàng bán.

Trả lời:

---

## C. Dropdown tìm kiếm gợi ý (SearchableCombobox)

### C1. Cách sửa lỗi bị đè / bị che
Đề xuất kỹ thuật: (1) sửa Tailwind để khung có nền trắng, bóng, giới hạn chiều cao; (2) **render khung gợi ý ra ngoài qua portal** (gắn vào `body`, vị trí `fixed`) để không bị bất kỳ vùng cuộn nào cắt; (3) **tự lật lên trên** khi sát đáy màn hình (dòng cuối của bảng); (4) tự đóng/định vị lại khi cuộn trang.
- **A.** ✅ Đồng ý toàn bộ.
- **B.** Chỉ làm (1), không dùng portal.

Trả lời:

### C2. Kích thước khung gợi ý
- **A.** ✅ Rộng tối thiểu **560px** (hoặc bằng ô nhập nếu ô rộng hơn), hiển thị tối đa **10 dòng** rồi cuộn; dòng cao 28px.
- **B.** Rộng bằng đúng ô nhập, tối đa 8 dòng.
- **C.** Khác: ______

Trả lời:

### C3. Các cột hiển thị khi tìm **sản phẩm**
Hiện tại: Mã | Tên | Giá
- **A.** ✅ Mã | Tên | ĐVT | Giá bán | **Tồn kho tại chi nhánh** (đỏ nếu ≤ 0).
- **B.** Giữ Mã | Tên | Giá.
- **C.** Khác: ______

Trả lời:

### C4. Các cột hiển thị khi tìm **khách hàng**
Hiện tại: Mã KH | Tên KH | Điện thoại
- **A.** ✅ Mã KH | Tên KH | Điện thoại | Địa chỉ (rút gọn).
- **B.** Thêm cột Công nợ hiện tại (mỗi lần tìm sẽ phải gọi thêm API công nợ → chậm hơn).
- **C.** Giữ nguyên.

Trả lời:

### C5. Hành vi tìm kiếm
- **A.** ✅ Mở danh sách ngay khi focus/click; chờ **250ms** sau khi ngừng gõ mới gọi API (debounce); **tô đậm phần chữ khớp**; ↑/↓ di chuyển, Enter hoặc Tab chọn, Esc đóng; tìm theo cả mã, tên, số điện thoại.
- **B.** Chỉ mở khi gõ từ 2 ký tự trở lên, còn lại như A.

Trả lời:

### C6. Khi không tìm thấy kết quả
- **A.** ✅ Hiện dòng "Không tìm thấy '…'" + nút **"+ Thêm khách hàng mới"** / **"+ Thêm sản phẩm mới"** mở popup tạo nhanh (chỉ với người có quyền).
- **B.** Chỉ hiện thông báo không tìm thấy.

Trả lời:

### C7. Popup tìm kiếm cũ (SearchModal)
Phiếu nhập hàng đang dùng popup tìm kiếm riêng (`SearchModal`), phiếu bán hàng dùng combobox.
- **A.** ✅ Thống nhất: dùng combobox mới cho cả phiếu nhập; giữ `SearchModal` làm tìm kiếm nâng cao (mở bằng nút 🔍 hoặc F5).
- **B.** Giữ nguyên hai kiểu như hiện tại, chỉ làm đẹp.

Trả lời:

---

## D. Màn hình đăng nhập

### D1. Bố cục
- **A.** ✅ **Chia đôi**: bên trái (≈ 55%) khối màu primary với tên hệ thống, khẩu hiệu ngắn và vài điểm nổi bật (Bán hàng · Tồn kho · Công nợ · Báo cáo); bên phải form đăng nhập trên nền trắng. Màn hình hẹp chỉ còn form.
- **B.** Một card trắng ở giữa, nền xám nhạt, logo phía trên.
- **C.** Card ở giữa trên nền tối/gradient màu primary (không dùng tím/hồng).

Trả lời:

### D2. Thương hiệu hiển thị
- Tên hiển thị (hiện là "ERP System"): ______
- Khẩu hiệu / mô tả ngắn (nếu có): ______
- Có logo không? Nếu có, bạn gửi file (SVG/PNG) hoặc để mình vẽ logo chữ đơn giản: ______

Trả lời:

### D3. Dòng "Tài khoản test: admin/password, staff_tp1/password"
- **A.** ✅ Chỉ hiện khi chạy môi trường dev (`npm run dev`), ẩn khi build production.
- **B.** Luôn hiện (phục vụ demo / chấm đồ án).
- **C.** Bỏ hẳn.

Trả lời:

### D4. Tính năng bổ sung cho form đăng nhập (chọn nhiều)
- [ ] ✅ Nút hiện/ẩn mật khẩu
- [ ] ✅ Cảnh báo khi đang bật Caps Lock
- [ ] ✅ Tự focus ô tên đăng nhập, Enter để đăng nhập, spinner khi đang xử lý
- [ ] "Ghi nhớ tên đăng nhập" (lưu tên đăng nhập, không lưu mật khẩu)
- [ ] Hiển thị chi nhánh đang kết nối (TP1, TP2… — lấy từ cấu hình build)
- [ ] Link "Quên mật khẩu?" (backend chưa có chức năng này → chỉ hiện hướng dẫn liên hệ admin)

Trả lời (ghi các mục chọn):

---

## E. Dashboard (Tổng quan)

### E1. Bố cục tổng thể
Đề xuất ✅ (từ trên xuống):
1. **Thanh tiêu đề**: tên trang + bộ lọc chi nhánh + bộ lọc thời gian + nút Làm mới + Xuất Excel — nằm gọn một hàng.
2. **4 thẻ KPI** một hàng.
3. **Biểu đồ Top sản phẩm** (≈ 2/3 chiều rộng) + **Khối cảnh báo tồn kho** (≈ 1/3) cạnh nhau.
4. **Bảng Top sản phẩm chi tiết** (thay cho danh sách thẻ tròn hiện tại).

- **A.** ✅ Theo đề xuất trên.
- **B.** Đưa **khối cảnh báo tồn kho lên đầu** (ngay dưới thanh tiêu đề, toàn chiều rộng), sau đó mới tới KPI và biểu đồ.
- **C.** Khác (mô tả): ______

Trả lời:

### E2. Các thẻ KPI
Hiện có: Tổng doanh thu · Lợi nhuận gộp · Vòng quay tồn kho · Công nợ quá hạn.
- **A.** ✅ Giữ 4 thẻ này, làm gọn (bỏ icon mờ khổng lồ ở nền), thêm **số sản phẩm cần chú ý** (âm/thấp) vào thẻ Vòng quay tồn kho dưới dạng dòng phụ.
- **B.** Thay "Vòng quay tồn kho" bằng "Số SP tồn thấp / âm".
- **C.** Thêm thẻ "Số đơn hàng" và "Giá trị đơn trung bình" (cần backend trả thêm dữ liệu).

Trả lời:

### E3. Biểu đồ Top sản phẩm
Hiện là cột đứng, 2 trục (doanh thu + số lượng), 5 màu cầu vồng, tên sản phẩm dài bị chồng chữ ở trục X.
- **A.** ✅ **Cột ngang** (tên sản phẩm đọc được đầy đủ), một màu primary, chỉ hiển thị doanh thu; số lượng đưa vào tooltip và bảng chi tiết.
- **B.** Giữ cột đứng 2 trục, chỉ đổi sang 2 màu thống nhất.

Trả lời:

Câu phụ: bạn có muốn thêm **biểu đồ doanh thu theo ngày/tháng** không? (API dashboard hiện **không** trả dữ liệu theo thời gian → cần sửa backend) — Trả lời:

### E4. Bộ lọc thời gian
Hiện có: Toàn thời gian · Tháng này · Năm nay.
- **A.** ✅ Hôm nay · 7 ngày qua · Tháng này · Quý này · Năm nay · Toàn thời gian · Tùy chọn (từ ngày – đến ngày). Mặc định: **Tháng này**.
- **B.** Giữ như cũ, mặc định Toàn thời gian.

Trả lời:

### E5. Ai được xem Dashboard?
Hiện chỉ ADMIN. STAFF đăng nhập vào thẳng màn hình bán hàng.
- **A.** ✅ Giữ như cũ, nhưng STAFF vẫn thấy **cảnh báo tồn kho của chi nhánh mình** (xem mục F5).
- **B.** STAFF cũng có Dashboard rút gọn cho chi nhánh mình.

Trả lời:

---

## F. Khối cảnh báo tồn kho thấp / âm (tính năng mới)

### F1. Cách xác định "tồn thấp"
- **A.** Một **ngưỡng chung** cho mọi sản phẩm (vd ≤ 10), cấu hình trong frontend. Làm nhanh, không cần sửa backend.
- **B.** **Ngưỡng riêng** từng sản phẩm/chi nhánh, lấy từ bảng `inventory_alert_config` đã có trong DB. Cần thêm API backend + màn hình để admin nhập ngưỡng.
- **C.** ✅ Kết hợp: dùng ngưỡng riêng nếu sản phẩm có cấu hình, nếu không thì dùng **ngưỡng mặc định**.

Trả lời:

Ngưỡng mặc định là bao nhiêu? (đề xuất: **10**) — Trả lời:

### F2. Được phép sửa backend (erp-backend) không?
Để có dữ liệu chính xác, đề xuất thêm 1 API: `GET /api/v1/inventory/stock/alerts?branchId=` trả về danh sách sản phẩm có tồn ≤ ngưỡng hoặc < 0, kèm ngưỡng áp dụng. (Và nếu chọn F1 = B/C: thêm API đọc/ghi ngưỡng + cột "Ngưỡng tối thiểu" trong màn hình Tồn kho.)
- **A.** ✅ Được sửa backend.
- **B.** Không, chỉ làm ở frontend (lọc từ API tồn kho hiện có với ngưỡng chung — khi đó F1 bắt buộc là A).

Trả lời:

### F3. Phạm vi chi nhánh
API tồn kho hiện đi qua **server chi nhánh** (cổng 8081) và lọc theo chi nhánh của người đăng nhập; còn Dashboard đi qua **server HQ** (cổng 8080).
- **A.** ✅ ADMIN: xem tất cả chi nhánh, có cột Chi nhánh, và **đi theo bộ lọc chi nhánh** của Dashboard. STAFF: chỉ chi nhánh của mình.
- **B.** Chỉ hiển thị chi nhánh đang đăng nhập cho mọi người.

Trả lời:

Câu phụ: ở HQ có dữ liệu tồn kho của **tất cả** chi nhánh không (qua replication)? Nếu bạn không chắc, mình sẽ tự kiểm tra trong code backend. — Trả lời:

### F4. Nội dung và cách hiển thị khối cảnh báo
Đề xuất ✅:
- Tiêu đề "Cảnh báo tồn kho" + 3 chip đếm: **Âm** (đỏ) · **Hết hàng** (= 0, cam) · **Sắp hết** (≤ ngưỡng, vàng). Bấm chip để lọc.
- Bảng: Mã SP | Tên SP | Chi nhánh | Tồn hiện tại | Ngưỡng | Trạng thái.
- Sắp xếp: Âm trước → Hết hàng → Sắp hết; trong mỗi nhóm, tồn thấp nhất lên đầu.
- Hiển thị tối đa **8 dòng**, cuối khối có nút **"Xem tất cả (n)"** mở tab Tồn kho đã lọc sẵn.
- Không có cảnh báo → hiện trạng thái xanh "Tồn kho ổn định".

- **A.** ✅ Theo đề xuất.
- **B.** Chỉ cần 2 mức: **Âm** và **Thấp** (gộp hết hàng vào thấp).
- **C.** Khác: ______

Trả lời:

### F5. Hiển thị cảnh báo ở chỗ khác (chọn nhiều)
- [ ] ✅ Huy hiệu số (badge đỏ) trên nút "Tồn kho" của ribbon
- [ ] ✅ Màn hình Tồn kho: tô màu dòng âm/thấp + bộ lọc "Chỉ hiện cần chú ý"
- [ ] ✅ Form bán hàng: cảnh báo ngay tại dòng khi số lượng bán > tồn hiện có
- [ ] Toast thông báo một lần sau khi đăng nhập ("Có n sản phẩm tồn thấp")
- [ ] Không cần, chỉ Dashboard

Trả lời (ghi các mục chọn):

### F6. Khi bán vượt tồn (dẫn tới tồn âm)
- **A.** ✅ Chỉ **cảnh báo** (viền đỏ + tooltip), vẫn cho lưu phiếu.
- **B.** **Chặn** không cho lưu.
- **C.** Hỏi xác nhận trước khi lưu.

Trả lời:

### F7. Tần suất cập nhật khối cảnh báo
- **A.** ✅ Khi mở Dashboard + tự làm mới mỗi **5 phút** + nút Làm mới.
- **B.** Chỉ khi mở Dashboard / bấm Làm mới.

Trả lời:

---

## G. Các chỉnh sửa nhỏ khác — xác nhận có làm không

Đánh `x` vào ô muốn làm (mặc định mình làm hết các mục ✅):

- [ ] ✅ G1. Bỏ giới hạn `max-w-7xl` + `p-6` của khung nội dung cho các **form nghiệp vụ** (chiếm toàn màn hình); Dashboard và danh mục giữ lề hợp lý.
- [ ] ✅ G2. Sửa thanh tab (TabBar): tab đang mở có màu primary rõ ràng; nút đóng luôn hiện ở tab đang mở; nhấp chuột giữa để đóng tab.
- [ ] ✅ G3. Góc phải ribbon: hiển thị **tên người dùng · vai trò · chi nhánh** + nút **Đăng xuất** (đưa ra khỏi tab Hệ thống).
- [ ] ✅ G4. Nút "Xuất Trả Hàng Mua" chưa có chức năng → ẩn đi (hoặc làm mờ + ghi "Sắp có").
- [ ] ✅ G5. Thống nhất giao diện 5 màn hình danh sách (Sản phẩm, Khách hàng, Nhà phân phối, Tồn kho, Công nợ) theo cùng bộ màu và mật độ đã chọn ở mục A.
- [ ] ✅ G6. Định dạng số và tiền theo chuẩn Việt Nam ở mọi nơi (`1.500.000 ₫`, ngày `dd/MM/yyyy`).
- [ ] ✅ G7. Trạng thái trống / đang tải / lỗi đồng nhất (dùng lại `DataState`).
- [ ] ✅ G8. Hiệu ứng focus rõ ràng cho bàn phím, `cursor-pointer` cho mọi nút.
- [ ] G9. Khác (ghi thêm): ______

---

## H. Cách làm việc

### H1. Có cần xem thiết kế trước khi sửa code không?
- **A.** Mình làm **bản mockup** (Login, Dashboard có khối cảnh báo, form phiếu bán hàng) để bạn duyệt trước, rồi mới sửa code.
- **B.** ✅ Sửa thẳng vào code theo câu trả lời, bạn chạy `npm run dev` để xem và góp ý vòng 2.

Trả lời:

### H2. Thứ tự ưu tiên
Đề xuất ✅: (1) Sửa Tailwind + bộ màu/token → (2) Dropdown tìm kiếm → (3) GenericDocumentForm → (4) Dashboard + cảnh báo tồn kho → (5) Login → (6) Các mục G.

Trả lời (giữ nguyên hoặc sắp lại):

### H3. Kiểm thử
Các test Playwright trong `tests/` dựa vào `data-testid` (vd `login-username`, `sales-line-row`, `sales-customer-combo`…).
- **A.** ✅ Giữ nguyên toàn bộ `data-testid`; sau khi sửa chạy `npm run build` + `npm run lint` + `vitest`; cập nhật test nếu cấu trúc thay đổi. Test Playwright bạn tự chạy (cần backend đang chạy).
- **B.** Không cần chạy test.

Trả lời:

### H4. Ghi chú thêm / yêu cầu khác
(ví dụ: màn hình tham khảo bạn thích, ảnh chụp lỗi dropdown, hạn nộp đồ án…)

Trả lời:
