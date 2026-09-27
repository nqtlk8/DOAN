# DESIGN SYSTEM (G6-G8)

## 1. Design Tokens (Màu sắc)

Hệ thống ERP sử dụng hệ thống màu thiết kế riêng, không sử dụng các màu mặc định của Tailwind như `teal-`, `indigo-`, `emerald-`... trong code giao diện nữa.

| Token | Giá trị | Dùng cho |
|---|---|---|
| `--color-primary` | `#1D4ED8` | Nút chính, tab active, focus, tổng tiền cần chú ý |
| `--color-primary-hover` | `#1E40AF` | Hover nút chính |
| `--color-primary-soft` | `#EFF6FF` | Nền dòng chọn, nền icon KPI, hover dropdown |
| `--color-primary-muted` | `#DBEAFE` | Viền nhạt, chip |
| `--color-app` | `#F5F7FA` | Nền toàn app |
| `--color-surface` | `#FFFFFF` | Card, ribbon, vùng form |
| `--color-line` | `#E2E8F0` | Viền bảng, đường chia |
| `--color-line-strong` | `#CBD5E1` | Viền ô nhập, viền nút phụ |
| `--color-ink` | `#0F172A` | Chữ chính, số tiền thường |
| `--color-ink-muted` | `#475569` | Nhãn, chữ phụ |
| `--color-ink-subtle` | `#64748B` | Placeholder, chú thích |
| `--color-danger` / `--color-danger-soft` | `#DC2626` / `#FEF2F2` | Lỗi, nợ, số âm, nút xóa |
| `--color-success` / `--color-success-soft` | `#16A34A` / `#F0FDF4` | Trạng thái "Đã xác nhận" |
| `--color-warning` / `--color-warning-soft` | `#D97706` / `#FFFBEB` | Trạng thái "Nháp" |

## 2. Kích thước & Mật độ (A6)

| Thành phần | Kích thước |
|---|---|
| Cỡ chữ nghiệp vụ | 13px (thân), 12px (nhãn, tiêu đề cột), 15px (tiêu đề phiếu) |
| Chiều cao ô nhập / bảng | 28px (`h-7`) |
| Chiều cao nút nghiệp vụ | 28px (`h-7`) |
| Bo góc nghiệp vụ | 4px (`rounded-[4px]`) |
| Bo góc Card / Popup | 8px (`rounded-lg`) |
| Bóng đổ Dropdown/Popup | `0 10px 30px -8px rgb(15 23 42 / 0.25)` |

## 3. Class dùng chung

- **Input:** `.erp-input` (cao 28px, bo 4px, focus chuẩn). Nếu lỗi dùng `.erp-input-error`.
- **Button:**
  - `.btn-primary`: Nút hành động chính (màu xanh dương).
  - `.btn-secondary`: Nút hành động phụ, hủy (nền trắng viền xám).
  - `.btn-danger`: Nút xóa (màu đỏ).
  - `.btn-ghost`: Nút icon không viền.
- **Table:**
  - `.erp-table`: Áp dụng cho bảng dữ liệu danh sách, header dính.
  - Cột số lượng/tiền: Gắn class `.num` (right-aligned, tabular-nums).
- **Badge:** `.badge`, `.badge-success`, `.badge-warning`, `.badge-danger`, `.badge-info`. Dùng cho trạng thái.

## 4. Định dạng dữ liệu (A4)

Tuyệt đối **KHÔNG** dùng `toLocaleString()` hay `toISOString().slice()` trực tiếp trong code UI. Thay vào đó, dùng `Intl.NumberFormat` với cấu hình tiếng Việt:
- Tiền tệ: `new Intl.NumberFormat('vi-VN').format(value)`
- Ngày tháng: Format theo múi giờ địa phương bằng helper hoặc trả về từ API.

Quy tắc số âm: Bắt buộc dùng `text-danger` để tô đỏ nếu giá trị < 0 (Không phải là cảnh báo).

## 5. Quy tắc TestID

Luôn giữ nguyên các `data-testid` để tương thích E2E.
Các prefix testid mới:
- `<prefix>-list-search`, `<prefix>-list-row`, `<prefix>-action-view`
- Mọi nút bấm trong dialog/form: `btn-add`, `btn-save`, `btn-cancel`.

## 6. Focus & Accessibility (G8)

- Mọi thành phần tương tác đều phải hiện rõ khi Tab qua (`:focus-visible`).
- Các nút nhấn luôn là `<button>` (không dùng `div` onClick) và có `aria-label` đối với icon button.
