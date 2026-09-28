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
- **Badge:** `.badge`, `.badge-success`, `.badge-warning`, `.badge-danger`, `.badge-neutral`. Dùng cho trạng thái.

## 4. Định dạng dữ liệu (A4)

Tuyệt đối **KHÔNG** dùng `toLocaleString()`, `new Intl.NumberFormat(...)` rời rạc hay `toISOString().slice()` trong component. Dùng helper trong `src/shared/utils/format.ts`:

| Helper | Ví dụ |
|---|---|
| `formatNumber(v, digits?)` | `1500000` → `1.500.000`; `formatNumber(1.5, 3)` → `1,5` |
| `formatCurrency(v)` | `250000` → `250.000 ₫` (chỉ dùng cho ô tổng, KPI; trong bảng dùng `formatNumber`) |
| `formatDate(v)` / `formatDateTime(v)` | `27/09/2026`, `27/09/2026 09:30` (chuỗi `yyyy-MM-dd` không bị lệch múi giờ) |
| `toDateKey(d)` / `toInputDate(d)` | `20260927` / `2026-09-27` theo giờ địa phương |
| `parseNumber(s)` | `'1.234,5'` → `1234.5` |
| `normalizeSearch(s)` | bỏ dấu tiếng Việt để tìm kiếm phía client |
| `escapeRegExp(s)` | bắt buộc khi đưa chuỗi người dùng gõ vào `RegExp` |
| `formatCompact(v)` | trục biểu đồ: `1,5 tr`, `850 N` |

Ô nhập số dùng `NumberInput` (hiển thị `1.500.000` khi đang gõ; `maxFractionDigits` cho số lượng thập phân; `allowNegative` cho số âm).

Quy tắc số âm: dùng `text-danger` nếu giá trị < 0 (không phải là cảnh báo).

## 5. Quy tắc TestID

Luôn giữ nguyên các `data-testid` đang có. Quy ước:
- Phiếu (`<prefix>` = `sales` | `inbound` | `return`): `<prefix>-line-row`, `-line-code`, `-line-quantity`, `-line-price`, `-line-total`, `-line-delete`, `-add-line`, `-grand-total`, `-total-qty`, `-partner-name`, `-partner-advanced`, `-error-msg`; `doc-code`, `doc-date`, `sum-*`.
- Thanh nút MDI: `btn-add`, `btn-save`, `btn-cancel`, `btn-confirm`, `btn-print`, `btn-exit` (chỉ render khi có handler).
- Ribbon: `ribbon-tab-<id>`, `ribbon-btn-<tabId>` (vd `ribbon-btn-products`, `ribbon-btn-stocks`).
- Danh mục: `<entity>-page`, `<entity>-row`, `list-no-match`; dropdown: `combobox-dropdown`, `combobox-create-new`; hộp thoại: `confirm-dialog-confirm|cancel`, `quick-create-save|cancel`, `search-modal-input|row`.

## 6. Focus & Accessibility (G8)

- Mọi thành phần tương tác đều phải hiện rõ khi Tab qua (`:focus-visible`).
- Các nút nhấn luôn là `<button>` (không dùng `div` onClick) và có `aria-label` đối với icon button.

## 7. Thành phần dùng chung

| Thành phần | Dùng khi |
|---|---|
| `GenericDocumentForm` + `documentLines.ts` | Mọi phiếu giao dịch. Dòng hàng quản lý bằng `useDocumentLines()` (luôn cập nhật dạng functional), lỗi dòng dùng `lineErrorKey(i, 'product' \| 'quantity')`, chọn sản phẩm dùng `productToLinePatch()` + `focusLineCell(id, 'quantity')`. |
| `MdiModuleLayout` | Khung module phiếu; form báo `onStateChange(mode, isLoading, status)` để layout ẩn nút Xác nhận khi phiếu không còn ở trạng thái Nháp. Form được giữ mounted khi chuyển sang "Danh sách phiếu". |
| `SearchableCombobox` | Dropdown gợi ý (portal, tự lật lên/xuống). Cột không có `width` sẽ tự giãn. |
| `ListTable` | Bảng danh mục chuẩn: header luôn hiện, trạng thái tải / lỗi / trống / không khớp nằm trong thân bảng. |
| `StatusBadge` | Chip trạng thái chứng từ (Nháp / Đã xác nhận / Đã hủy). |
| `ConfirmDialog`, `QuickCreate*`, `SearchModal` | Có `role="dialog"`; khi đang mở, phím tắt F2–F12/Esc của form bị bỏ qua. |

