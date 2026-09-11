# Tài liệu Phân tích Nghiệp vụ — ERP Cửa hàng Vật liệu Xây dựng & Thiết bị Thông minh Nhà

**Phiên bản:** 1.2  
**Ngày:** 2026-09-08  
**Trạng thái:** Chính thức  
**Đối tượng:** Business Stakeholder · System Engineer · Architect

> **Changelog v1.2:**
> - Xác nhận FIFO là phương pháp tính giá vốn chính thức
> - Thêm nghiệp vụ **Stock Ledger (Sổ cái tồn kho)** — ghi nhận toàn bộ lịch sử biến động kho, truy vết giao dịch tạo nên tồn kho hiện tại
> - Mở rộng mô hình tồn kho từ 2 lớp lên 3 lớp: `stock_on_hand` + `stock_lot` + `stock_ledger`
>
> **Changelog v1.1:**
> - Bỏ nghiệp vụ chuyển kho — luân chuyển hàng giữa chi nhánh qua giao dịch mua/bán thông thường
> - Bỏ quy đổi đơn vị — nhập thủ công theo đúng đơn vị
> - Bỏ phân loại khách hàng (customer_type) — mọi khách hàng đều có thể mua chịu
> - Chuyển từ Weighted Average sang FIFO cho tính giá vốn & lợi nhuận
> - Nhà cung cấp quản lý tập trung tại HQ, hiển thị scoped theo chi nhánh
> - Bỏ xuất kho nội bộ & bảng chuyển kho

---

## Mục lục

1. [Tổng quan Hệ thống](#1-tổng-quan-hệ-thống)
2. [Các Nghiệp vụ Chính](#2-các-nghiệp-vụ-chính)
   - 2.1 [Quản lý Danh mục & Sản phẩm](#21-quản-lý-danh-mục--sản-phẩm)
   - 2.2 [Quản lý Khách hàng (CRM)](#22-quản-lý-khách-hàng-crm)
   - 2.3 [Bán hàng (Sales)](#23-bán-hàng-sales)
   - 2.4 [Trả hàng (Goods Return)](#24-trả-hàng-goods-return)
   - 2.5 [Mua hàng / Nhập kho (Procurement & Inbound)](#25-mua-hàng--nhập-kho-procurement--inbound)
   - 2.6 [Quản lý Tồn kho (Inventory)](#26-quản-lý-tồn-kho-inventory)
   - 2.7 [Luân chuyển hàng giữa Chi nhánh](#27-luân-chuyển-hàng-giữa-chi-nhánh)
   - 2.8 [Phân quyền & Bảo mật (RBAC)](#28-phân-quyền--bảo-mật-rbac)
   - 2.9 [Báo cáo & Phân tích (Analytics)](#29-báo-cáo--phân-tích-analytics)
   - 2.10 [Trình chiếu sản phẩm (Visualizer)](#210-trình-chiếu-sản-phẩm-visualizer)
3. [Nghiệp vụ Giá — Cơ chế Linh hoạt](#3-nghiệp-vụ-giá--cơ-chế-linh-hoạt)
4. [Tính Lợi nhuận & Giá vốn — FIFO](#4-tính-lợi-nhuận--giá-vốn--fifo)
5. [Toàn vẹn Dữ liệu: ACID, Race-condition & Đồng bộ](#5-toàn-vẹn-dữ-liệu-acid-race-condition--đồng-bộ)
6. [Các Quy ước Chung Toàn Hệ thống](#6-các-quy-ước-chung-toàn-hệ-thống)
7. [Bảng Tóm tắt Quyết định Kỹ thuật](#7-bảng-tóm-tắt-quyết-định-kỹ-thuật)

---

## 1. Tổng quan Hệ thống

### 1.1. Doanh nghiệp là ai?

Hệ thống phục vụ một **chuỗi cửa hàng vật liệu xây dựng và thiết bị thông minh nhà (VLXD & TTNT)** vận hành theo mô hình **1 Trụ sở (HQ) + N Chi nhánh (Branch)**. Mỗi chi nhánh hoạt động bán hàng độc lập, có kho riêng, công nợ riêng, nhưng chia sẻ chung danh mục sản phẩm, thông tin khách hàng, nhà cung cấp và chính sách giá do HQ phát hành.

**Khách hàng:** Mọi khách hàng đều bình đẳng về điều kiện giao dịch — không phân loại. Bất kỳ khách hàng nào cũng có thể mua ngay hoặc mua chịu tùy thỏa thuận với nhân viên bán hàng.

### 1.2. Vấn đề kinh doanh cần giải quyết

| # | Vấn đề | Giải pháp hệ thống cung cấp |
|---|---|---|
| 1 | Quản lý nhiều chi nhánh thủ công, không đồng nhất | Nền tảng ERP thống nhất, dữ liệu master tập trung tại HQ |
| 2 | Giá bán linh hoạt theo khách hàng, khó theo dõi | Cơ chế giá đa tầng: giá niêm yết → giá gần nhất → giá thỏa thuận |
| 3 | Tồn kho không chính xác | Hệ thống trừ kho nguyên tử (atomic), cho phép âm kho có kiểm soát |
| 4 | Công nợ khách hàng khó đối soát | Snapshot công nợ trên từng hóa đơn, sổ nợ theo chi nhánh |
| 5 | Nhân viên nhiều chi nhánh truy cập lẫn nhau | RBAC theo scope chi nhánh, JWT RS256 |
| 6 | Lợi nhuận gộp không phản ánh đúng từng giao dịch | Tính lợi nhuận theo lô nhập (FIFO), snapshot giá vốn per line |

---

## 2. Các Nghiệp vụ Chính

### 2.1. Quản lý Danh mục & Sản phẩm

#### Mục tiêu nghiệp vụ
Duy trì danh sách sản phẩm thống nhất toàn hệ thống. HQ là người duy nhất được phép tạo/sửa/ẩn sản phẩm. Chi nhánh chỉ được đọc.

#### Quy trình

```
HQ Admin
  → Tạo/Cập nhật Danh mục (phân cấp cha/con, VD: Gạch → Gạch ốp lát → Gạch 60x60)
  → Tạo Sản phẩm (gắn danh mục, đơn vị tính, SKU duy nhất)
  → Khai báo Thuộc tính động (màu sắc, kích thước, xuất xứ, ...)
  → Hệ thống tự đồng bộ danh mục xuống toàn bộ chi nhánh (logical replication)
```

#### Đơn vị tính — nhập thủ công

Hệ thống **không tự động quy đổi đơn vị**. Mỗi sản phẩm có một đơn vị tính cơ bản (m², viên, thùng, bao, kg, ...). Khi nhập kho hay bán hàng, nhân viên nhập **đúng số lượng theo đơn vị đã khai báo** — không có cơ chế chuyển đổi tự động. Điều này đơn giản hóa hệ thống và phù hợp với thực tế vận hành đa dạng của ngành VLXD (tỷ lệ quy đổi phụ thuộc nhiều yếu tố phức tạp không thể chuẩn hóa).

#### Đặc điểm kỹ thuật nổi bật

- **EAV (Entity-Attribute-Value):** Thuộc tính sản phẩm (màu, kích thước, xuất xứ) không cố định schema — mỗi danh mục có bộ thuộc tính riêng, linh hoạt thêm mới mà không sửa schema DB.
- **JSONB Cache:** Mỗi sản phẩm có cột `attributes_cache` (JSONB) tổng hợp toàn bộ thuộc tính EAV để trang public đọc nhanh. Nguồn sự thật vẫn là bảng EAV — cache chỉ cập nhật khi có thay đổi.
- **Soft-delete bằng `is_active`:** Sản phẩm không bị xóa vật lý mà chỉ bị vô hiệu hóa.

---

### 2.2. Quản lý Khách hàng (CRM)

#### Mục tiêu nghiệp vụ
Duy trì hồ sơ khách hàng tập trung tại HQ, theo dõi công nợ phải thu theo từng chi nhánh.

#### Chính sách công nợ

Mọi khách hàng đều có thể mua chịu — không phân biệt loại khách hàng. Nhân viên bán hàng quyết định `amount_paid` khi lập hóa đơn:
- `amount_paid = total_amount` → Thanh toán ngay, không phát sinh công nợ
- `amount_paid < total_amount` → Phát sinh công nợ `remaining_debt = total_amount - amount_paid`
- `amount_paid = 0` → Mua chịu toàn bộ

#### Vòng đời công nợ (Receivable Debt)

```
Bán hàng → Hóa đơn CONFIRMED
  → remaining_debt > 0? → Tăng công nợ: receivable_debt.current_balance += remaining_debt

Khách hàng thanh toán / trả hàng
  → Giảm công nợ: receivable_debt.current_balance -= amount_paid / return_amount
```

> **Nguyên tắc thiết kế:** Mỗi chi nhánh giữ sổ nợ riêng cho cùng một khách hàng. Khách hàng A mua chịu ở Chi nhánh TP1 có công nợ tại TP1, **không chia sẻ** với số dư TP2 — quyết định có chủ đích tránh distributed transaction xuyên chi nhánh.

#### Thông tin khách hàng

Thông tin cơ bản (tên, SĐT, địa chỉ) do HQ quản lý. Khi có thay đổi, dữ liệu đồng bộ xuống chi nhánh qua logical replication. Chi nhánh **không được sửa** hồ sơ khách hàng.

---

### 2.3. Bán hàng (Sales)

#### Mục tiêu nghiệp vụ
Ghi nhận giao dịch bán hàng, trừ kho, cập nhật công nợ, lưu lịch sử giá và giá vốn (snapshot) theo từng dòng sản phẩm.

#### Vòng đời Hóa đơn Bán hàng

```
[DRAFT] ──────────────── Xác nhận ────────────→ [CONFIRMED]
   |                                                  |
   |  Sửa / Thêm dòng sản phẩm                        |
   |  Không ảnh hưởng kho & công nợ                   |  Bất biến — không sửa được
   |                                                  |
   └──────────────────────────────────────────        ↓
                                              Hủy → [CANCELLED]
                                              (hoặc tạo Phiếu Trả hàng)
```

#### Bước CONFIRMED — các hành động nguyên tử trong 1 transaction

1. **Trừ kho theo lô (FIFO)** — lấy hàng từ lô nhập cũ nhất còn tồn, ghi nhận cost của lô đó
2. **Snapshot giá vốn** — `unit_cost_snapshot` = cost của lô FIFO tại thời điểm bán (cố định vĩnh viễn)
3. **Snapshot công nợ** — Lưu `previous_debt`, `total_amount`, `remaining_debt` cứng vào hóa đơn
4. **Cập nhật công nợ** — `receivable_debt.current_balance += remaining_debt`
5. **Cập nhật giá gần nhất theo khách hàng** — `customer_product_price.last_price = unit_price`

#### Đơn đặt hàng Khách hàng (Customer Order)

Dành cho khách đặt hàng trước khi hàng về kho:
- Ghi nhận số lượng & giá thỏa thuận
- Khi hóa đơn được tạo từ đơn đặt hàng, `fulfilled_quantity` cộng dần
- Không được vượt `ordered_quantity` (CHECK constraint + optimistic locking)

---

### 2.4. Trả hàng (Goods Return)

#### Mục tiêu nghiệp vụ
Xử lý khi khách hàng trả lại hàng đã mua — hoàn kho, giảm công nợ.

#### Quy trình

```
Nhân viên tạo Phiếu Trả hàng (DRAFT)
  → Chọn hóa đơn gốc (tùy chọn, không bắt buộc)
  → Nhập sản phẩm & số lượng trả
  → Xác nhận (CONFIRMED)
      ├→ Hoàn kho: stock_on_hand.quantity += số lượng trả
      |    (Hàng trả được nhập lại kho như 1 lô mới với cost = return_price)
      ├→ Giảm công nợ: receivable_debt.current_balance -= total_return_amount
      └→ Ghi audit log
```

#### Nguyên tắc
- Giá hoàn trả (`return_price`) có thể khác giá bán gốc (thỏa thuận linh hoạt)
- Hàng trả về được nhập lại kho như **lô nhập mới** với giá vốn = giá hoàn trả

---

### 2.5. Mua hàng / Nhập kho (Procurement & Inbound)

#### Mục tiêu nghiệp vụ
Ghi nhận hàng nhập về kho từ nhà cung cấp, cập nhật tồn kho và tạo lô tồn (stock lot) để phục vụ tính giá vốn FIFO.

#### Vòng đời Phiếu Nhập kho

```
[DRAFT]  ─────── Xác nhận ──────→ [CONFIRMED]
   |                                    |
   |  Thêm/sửa sản phẩm, số lượng       |
   |  Chọn Nhà cung cấp                 |  Tăng stock_on_hand.quantity
   |  Gắn PO (tùy chọn)                 |  Tạo stock_lot mới (product + branch + cost + qty)
   |                                    |  Ghi payable_debt (nếu mua chịu)
   └────────────────────────────────────┘
```

#### Nhà cung cấp (Supplier) — HQ Master, Branch-Scoped

Nhà cung cấp được **tạo và quản lý tập trung tại HQ**. Tuy nhiên, mỗi NCC được gán phạm vi hoạt động theo chi nhánh — chi nhánh chỉ nhìn thấy và mua từ NCC của mình, **không thể thấy NCC của chi nhánh khác**. Dữ liệu NCC đồng bộ từ HQ xuống chi nhánh qua logical replication (read-only tại chi nhánh).

#### Đơn đặt hàng NCC (Purchase Order)

- Là công cụ theo dõi, **không ảnh hưởng tồn kho** khi tạo
- Chỉ khi phiếu nhập kho được xác nhận (CONFIRMED) thì kho mới tăng và lô mới được tạo

---



### 2.6. Quản lý Tồn kho (Inventory)

#### Triết lý thiết kế — 3 thành phần tách biệt rõ vai trò

```
Stock Movement  →  Nguồn sự thật về SỐ LƯỢNG tồn kho
Cost Layer      →  Nguồn phục vụ tính GIÁ VỐN FIFO
stock_on_hand   →  Cache tổng hợp — hot path (tốc độ đọc)
```

**Chứng từ nghiệp vụ** (inbound_receipt, sales_invoice, goods_return) là tác nhân tạo ra movement và cost layer entry — chứng từ không trực tiếp sửa stock_on_hand.

**Quy tắc nhất quán:** Tất cả 3 thành phần được cập nhật **trong cùng 1 database transaction** khi CONFIRM. Không có trường hợp thành phần này cập nhật mà thành phần kia không cập nhật.

#### Thành phần 1 — Stock Movement (Nguồn sự thật về số lượng)

Mỗi sự kiện làm thay đổi kho tạo ra **1 dòng trong stock_movement** — bất biến, append-only:

| Trường | Ý nghĩa |
|---|---|
| `product_id`, `branch_id` | Sản phẩm và chi nhánh |
| `movement_type` | `INBOUND` / `SALE` / `RETURN` / `ADJUSTMENT` |
| `quantity` | Số lượng (dương = vào, âm = ra) |
| `ref_type` | Loại chứng từ: `inbound_receipt` / `sales_invoice` / `goods_return` |
| `ref_id` | ID chứng từ gốc |
| `ref_line_id` | ID dòng chứng từ gốc |
| `performed_by` | Người thực hiện |
| `created_at` | Thời điểm |

> `SUM(quantity)` từ stock_movement = `stock_on_hand.quantity` — hai giá trị này luôn đồng nhất, có thể dùng để kiểm tra tính toàn vẹn dữ liệu định kỳ.

#### Thành phần 2 — Cost Layer (Nguồn tính giá vốn FIFO)

Mỗi lần **nhập kho** tạo 1 cost layer entry với giá vốn riêng. Khi **bán hàng**, FIFO tiêu thụ cost layer từ cũ nhất:

| Trường | Ý nghĩa |
|---|---|
| `product_id`, `branch_id` | Sản phẩm và chi nhánh |
| `unit_cost` | Giá vốn của lớp này |
| `initial_qty` | Số lượng ban đầu khi nhập |
| `remaining_qty` | Số lượng còn lại (giảm khi bán) |
| `inbound_movement_id` | FK tới stock_movement (INBOUND) tạo ra layer này |
| `created_at` | Thứ tự FIFO (cũ hơn → tiêu thụ trước) |

**Không có khái niệm "lô hàng" (lot)** — cost layer là một khái niệm thuần kế toán, không phải nghiệp vụ. Cùng sản phẩm nhập nhiều lần → nhiều cost layer entry với giá khác nhau.

#### Thành phần 3 — stock_on_hand (Cache hot path)

Running total đơn giản. Mục đích duy nhất: đọc nhanh số lượng hiện tại mà không cần `SUM` toàn bộ movement history.

```sql
stock_on_hand: product_id + branch_id → quantity (+ version for optimistic lock)
```

#### Ví dụ minh họa toàn luồng

```
Nhập PN-001: 100 viên × 10,000đ
  → stock_movement: INBOUND, qty=+100, ref=PN-001
  → cost_layer:     unit_cost=10,000, initial=100, remaining=100
  → stock_on_hand:  quantity: 0 → 100

Nhập PN-002: 50 viên × 12,000đ
  → stock_movement: INBOUND, qty=+50, ref=PN-002
  → cost_layer:     unit_cost=12,000, initial=50, remaining=50
  → stock_on_hand:  quantity: 100 → 150

Bán HĐ-001: 80 viên × 20,000đ
  FIFO: lấy 80 từ cost_layer[10,000] (còn 100 → còn 20)
  → stock_movement: SALE, qty=-80, ref=HĐ-001
  → cost_layer[10k]: remaining: 100 → 20
  → unit_cost_snapshot = 10,000đ (lưu vào sales_invoice_line)
  → stock_on_hand:  quantity: 150 → 70

Truy vết: SELECT * FROM stock_movement WHERE product_id=X AND branch_id=Y ORDER BY created_at
  → Thấy đủ lịch sử: nhập 100, nhập 50, bán 80 → 70 còn lại ✓
```

#### Các luồng tác động

| Sự kiện | stock_movement | cost_layer | stock_on_hand |
|---|---|---|---|
| Nhập hàng CONFIRMED | +1 dòng INBOUND | Tạo entry mới | + quantity |
| Bán hàng CONFIRMED | +1 dòng SALE | Tiêu thụ FIFO (giảm remaining) | − quantity |
| Trả hàng CONFIRMED | +1 dòng RETURN | Tạo entry mới (cost = return_price) | + quantity |

#### Chính sách Âm kho (Negative Stock)

> **Quyết định nghiệp vụ đã chốt:** Hệ thống **cho phép tồn kho xuống âm** — không có CHECK `quantity >= 0`.

**Lý do:** Nhà thầu thường xác nhận đơn hàng trước khi hàng về. Chặn bán khi âm kho gây gián đoạn nghiệp vụ. Kế toán đối soát định kỳ.

**Xử lý âm kho trong Cost Layer:** Khi bán mà không còn cost layer nào có `remaining_qty > 0`:
- Vẫn ghi stock_movement (SALE, quantity âm)
- `unit_cost_snapshot = 0`, đánh dấu `cost_basis = 'NO_LAYER'`
- Kế toán lọc các dòng `cost_basis = 'NO_LAYER'` để điều chỉnh thủ công

---





---

### 2.7. Luân chuyển hàng giữa Chi nhánh

#### Mục tiêu nghiệp vụ
Khi chi nhánh A dư hàng, chi nhánh B thiếu, hàng được luân chuyển giữa các chi nhánh.

#### Cơ chế: Mua bán thông thường

Không có nghiệp vụ "chuyển kho" riêng. Luân chuyển hàng thực hiện bằng cách:

```
Chi nhánh A (bên gửi):
  → Lập Sales Invoice bán cho "Chi nhánh B"
  → Giá có thể là giá vốn (không lợi nhuận) hoặc theo thỏa thuận nội bộ
  → Xác nhận → Trừ kho A bình thường

Chi nhánh B (bên nhận):
  → Lập Inbound Receipt nhập hàng từ "Chi nhánh A" (xem như nhà cung cấp)
  → Xác nhận → Tăng kho B, tạo lot mới với cost = giá mua từ A
```

#### Hệ quả

- Đơn giản hóa: không cần `outbound_receipt`, `stock_transfer` riêng
- Mỗi bên tự quản lý trong DB của mình — không có distributed transaction
- Luân chuyển tạo ra giao dịch thực tế, có thể báo cáo như doanh thu nội bộ
- Giá luân chuyển (thường = giá vốn) được ghi nhận minh bạch trong hóa đơn

---

### 2.8. Phân quyền & Bảo mật (RBAC)

#### Mô hình phân quyền

```
User ──has many──→ UserBranchRole
                       |
                       ├── role_id → Role (ADMIN / STAFF)
                       └── branch_id → Branch (NULL = toàn hệ thống)
```

| Role | Quyền | Phạm vi |
|---|---|---|
| `ADMIN` | Toàn quyền: quản lý master data, xem báo cáo HQ, quản lý user | Toàn hệ thống (branch_id = NULL) |
| `STAFF` | Nghiệp vụ hàng ngày: tạo hóa đơn, nhập kho, xem tồn kho | Scoped theo chi nhánh được gán |

#### JWT — Cơ chế xác thực

- **HQ** ký JWT bằng **RSA private key** (RS256)
- **Chi nhánh** chỉ giữ **public key** để xác minh — không thể tự cấp token
- Access Token: 30 phút | Refresh Token: 7 ngày

---

### 2.9. Báo cáo & Phân tích (Analytics)

#### Mục tiêu nghiệp vụ
Cung cấp báo cáo doanh thu, lợi nhuận gộp, biến động tồn kho theo chi nhánh và theo thời gian. Lợi nhuận gộp phản ánh đúng từng giao dịch — không bị "trung bình hóa" bởi weighted average cost.

#### Kiến trúc Analytics — Star Schema

```
dim_date ────┐
             ├──→ fact_sales (revenue, gross_profit theo ngày × sản phẩm × chi nhánh)
dim_product ─┤
dim_branch ──┘

dim_date ────┐
             └──→ fact_stock_movement (biến động kho: INBOUND, OUTBOUND, RETURN)
```

#### Lợi nhuận gộp — tính theo FIFO

```
Nhập lô 1: 100 viên × 10,000đ  (lot_1)
Nhập lô 2:  50 viên × 15,000đ  (lot_2)

Bán đơn 1: 80 viên × 20,000đ
  → FIFO lấy từ lot_1: cost = 10,000đ
  → gross_profit_đơn_1 = (20,000 - 10,000) × 80 = 800,000đ

Bán đơn 2: 40 viên × 22,000đ
  → FIFO: 20 viên còn lại từ lot_1 (cost=10,000) + 20 viên từ lot_2 (cost=15,000)
  → gross_profit_đơn_2 = (22,000-10,000)×20 + (22,000-15,000)×20 = 240,000 + 140,000 = 380,000đ

Tổng gross_profit = 800,000 + 380,000 = 1,180,000đ
```

Phương pháp này phản ánh đúng **quá trình kinh doanh thực tế**, không làm mờ đi sự khác biệt giá giữa các lô nhập.

---

### 2.10. Trình chiếu Sản phẩm (Visualizer)

#### Mục tiêu nghiệp vụ
Cho phép khách hàng trên website public xem trước sản phẩm (gạch, vật liệu) được ốp/lát lên không gian thực tế trước khi mua.

#### Cơ chế
1. HQ tải lên ảnh phòng mẫu (`visualizer_room`)
2. Khai báo vùng (zone) trên ảnh — sàn, tường — bằng tọa độ đa giác (`polygon_json`)
3. Khai báo tỷ lệ pixel/cm để tính chính xác kích thước gạch thật trên không gian ảo
4. Khách hàng chọn sản phẩm → hệ thống render ảnh kết quả → lưu session

---

## 3. Nghiệp vụ Giá — Cơ chế Linh hoạt

> **Đây là nghiệp vụ phức tạp và quan trọng nhất trong hệ thống.** Giá bán không phải là một con số cứng — nó biến động theo sản phẩm, chi nhánh, khách hàng, thời điểm, và thỏa thuận thương mại.

### 3.1. Ba tầng Giá

```
Tầng 1: Giá niêm yết (List Price)
         └── price_list (product × branch × effective_date)
                  "Giá chung cho tất cả khách"

Tầng 2: Giá gần nhất theo Khách hàng (Last Customer Price)
         └── customer_product_price (customer × product × branch)
                  "Lần trước khách này mua giá bao nhiêu?"

Tầng 3: Giá tùy chỉnh (Override Price)
         └── sales_invoice_line.unit_price (is_price_overridden = true)
                  "Nhân viên nhập tay giá thương lượng cho đơn này"
```

### 3.2. Logic gợi ý giá khi thêm sản phẩm vào hóa đơn

```
Khi nhân viên thêm sản phẩm P vào hóa đơn cho Khách hàng C tại Chi nhánh B:

1. Tra cứu customer_product_price(C, P, B)
   → Nếu có: GỢI Ý giá gần nhất (khách này đã từng mua giá này)

2. Nếu chưa có (khách mới hoặc sản phẩm mới):
   → Tra cứu price_list(P, B, TODAY): lấy effective_date gần nhất ≤ TODAY
   → Gợi ý giá niêm yết hiệu lực

3. Nhân viên CÓ THỂ sửa giá gợi ý:
   → is_price_overridden = TRUE
   → unit_price = giá nhân viên nhập
   → default_unit_price = giá gợi ý gốc (lưu lại để so sánh)

4. Khi hóa đơn CONFIRMED:
   → customer_product_price.last_price = unit_price
   → Lần bán sau, gợi ý sẽ là giá đã thỏa thuận lần này
```

### 3.3. Nguyên tắc "Giá cố định trong hóa đơn"

Một khi hóa đơn chuyển sang trạng thái `CONFIRMED`:
- `unit_price` trong `sales_invoice_line` **không bao giờ thay đổi** — đây là con số pháp lý
- Dù giá niêm yết hôm sau có thay đổi, hóa đơn cũ vẫn giữ nguyên giá tại thời điểm giao dịch

### 3.4. Giá niêm yết theo thời gian hiệu lực

```
price_list:
  product_id | branch_id | price  | effective_date
  ─────────────────────────────────────────────────
  Gạch A     | TP1       | 85,000 | 2026-01-01   ← Giá cũ
  Gạch A     | TP1       | 92,000 | 2026-06-01   ← Giá mới (hiệu lực từ 1/6)
  Gạch A     | TP2       | 90,000 | 2026-01-01   ← TP2 có giá khác TP1
```

- Mỗi chi nhánh có thể có **giá niêm yết riêng** cho cùng sản phẩm
- Lịch sử giá được **giữ nguyên** — không ghi đè — phục vụ đối soát và audit
- Khi tra giá, hệ thống lấy bản ghi có `effective_date` gần nhất ≤ ngày hiện tại

### 3.5. Bảng so sánh các Tình huống Giá

| Tình huống | Giá gợi ý | Ghi chú |
|---|---|---|
| Khách mới, sản phẩm phổ thông | Giá niêm yết hôm nay | Không có lịch sử mua |
| Khách đã từng mua | Giá gần nhất đã mua | Có thể khác giá niêm yết nếu lần trước được chiết khấu |
| Khách quen, mua sỉ | Giá gần nhất đã thỏa thuận | Thường thấp hơn giá niêm yết |
| Thỏa thuận đặc biệt | Nhân viên nhập tay | `is_price_overridden = true` |
| Sản phẩm mới, chưa có giá niêm yết | Hệ thống không gợi ý | Nhân viên bắt buộc nhập giá |

---

## 4. Tính Lợi nhuận & Giá vốn — FIFO

> **Quyết định đã xác nhận:** Hệ thống dùng **FIFO (First-In, First-Out)** làm phương pháp tính giá vốn hàng xuất kho — đúng chuẩn kế toán Việt Nam (Thông tư 200) và phản ánh trung thực quá trình kinh doanh.

### 4.1. Tại sao FIFO thay vì Weighted Average?

| Khía cạnh | Weighted Average (đã bỏ) | FIFO (hiện tại) |
|---|---|---|
| Phản ánh thực tế | Làm mờ sự khác biệt giữa các lô nhập | Rõ ràng: lô nào bán trước, cost bao nhiêu |
| Chuẩn kế toán VN | Được chấp nhận (TT 200) | Được chấp nhận (TT 200), phổ biến hơn với hàng VLXD |
| Báo cáo lợi nhuận | Lợi nhuận bị "bình quân hóa" | Lợi nhuận phản ánh đúng từng giao dịch, từng lô |
| Khả năng truy vết | Không biết hàng bán ra từ lô nào | Biết chính xác hàng bán từ lô nào, giá vốn bao nhiêu |

### 4.2. Luồng FIFO khi Nhập kho (CONFIRMED)

```
Tạo stock_lot mới:
  product_id   = P
  branch_id    = B
  unit_cost    = giá nhập lô này (từ inbound_receipt_line.unit_cost)
  initial_qty  = số lượng nhập
  remaining_qty = số lượng nhập  ← giảm dần khi bán
  received_at  = NOW()           ← dùng để sắp xếp FIFO

Ghi stock_ledger:
  movement_type = 'INBOUND'
  quantity_change = +initial_qty
  unit_cost = unit_cost của lot
  balance_qty = stock_on_hand.quantity sau khi tăng
  ref_type = 'inbound_receipt', ref_id = receipt.id
```

### 4.3. Luồng FIFO khi Bán hàng (CONFIRMED)

```
Cần bán: 150 viên Gạch A tại Chi nhánh TP1

Bước 1 — Xác định lot cần tiêu thụ (FOR UPDATE lock):
  SELECT * FROM stock_lot
  WHERE product_id=P AND branch_id=B AND remaining_qty > 0
  ORDER BY received_at ASC
  FOR UPDATE

  → Lot 1: received 2026-01-05, cost=10,000, remaining=100 → tiêu thụ 100 viên
  → Lot 2: received 2026-03-10, cost=12,000, remaining=200 → tiêu thụ 50 viên

Bước 2 — Cập nhật lot:
  UPDATE stock_lot SET remaining_qty = 0   WHERE id = lot_1
  UPDATE stock_lot SET remaining_qty = 150 WHERE id = lot_2

Bước 3 — Ghi unit_cost_snapshot vào sales_invoice_line:
  unit_cost_snapshot = weighted average của các lot được tiêu thụ lần này
                     = (100×10,000 + 50×12,000) / 150 = 10,667đ
  (Phương pháp này đủ chính xác và đơn giản cho UI — 1 dòng/sản phẩm)

Bước 4 — Ghi stock_ledger (2 dòng vì lấy từ 2 lot):
  Dòng 1: movement_type='SALE', quantity_change=-100, unit_cost=10,000, lot_id=lot_1
  Dòng 2: movement_type='SALE', quantity_change=-50,  unit_cost=12,000, lot_id=lot_2
  (Cả 2 dòng đều ref tới cùng 1 sales_invoice_line_id)

→ gross_profit của sales_invoice_line này:
  = (unit_price - 10,000)×100 + (unit_price - 12,000)×50
  = Tính từ stock_ledger, không phải từ 1 con số bình quân
```

### 4.4. Tính Lợi nhuận Gộp

**Nguồn dữ liệu:** `stock_ledger` (movement_type = 'SALE') JOIN `sales_invoice_line`

```
gross_profit_per_ledger_line = (sales_invoice_line.unit_price - stock_ledger.unit_cost)
                               × ABS(stock_ledger.quantity_change)

gross_profit_total = SUM(gross_profit_per_ledger_line)
                     cho tất cả dòng SALE trong kỳ báo cáo
```

**Ví dụ đầy đủ:**
```
Nhập lô 1: 100 viên × 10,000đ
Nhập lô 2:  50 viên × 12,000đ

Bán đơn 1: 80 viên × 20,000đ → lấy từ lot_1
  stock_ledger: qty=-80, cost=10,000
  gross_profit = (20,000-10,000)×80 = 800,000đ

Bán đơn 2: 40 viên × 22,000đ → 20 từ lot_1, 20 từ lot_2
  stock_ledger dòng 1: qty=-20, cost=10,000
  stock_ledger dòng 2: qty=-20, cost=12,000
  gross_profit = (22,000-10,000)×20 + (22,000-12,000)×20 = 240,000 + 200,000 = 440,000đ

Tổng gross_profit = 800,000 + 440,000 = 1,240,000đ
```

### 4.5. Tác động đến Schema (so với thiết kế cũ)

| Thay đổi | Chi tiết |
|---|---|
| **THÊM** bảng `cost_layer` | `product_id, branch_id, unit_cost, initial_qty, remaining_qty, received_at, inbound_movement_id` |
| **THÊM** bảng `stock_movement` | `product_id, branch_id, movement_type, quantity_change, unit_cost, balance_qty, balance_value, ref_type, ref_id, ref_line_id, performed_by, created_at` |
| **XÓA** cột `avg_cost` trên `stock_on_hand` | Không còn dùng weighted average — giá vốn tra từ `cost_layer` |
| **GIỮ** `cost_basis` trên `sales_invoice_line` | Snapshot weighted-of-lots tại thời điểm bán — cố định vĩnh viễn |
| **GIỮ** `stock_on_hand.quantity` | Running total cho hot path — không thay đổi vai trò |

### 4.6. Edge Cases

| Tình huống | Cách xử lý |
|---|---|
| Bán khi không còn lot (âm kho) | `unit_cost_snapshot = 0`, ghi ledger với `cost_basis = 'NO_LOT'` — kế toán điều chỉnh sau |
| Hủy hóa đơn đã CONFIRMED | Tạo ledger dòng `ADJUSTMENT` hoàn ngược, hoàn `remaining_qty` về lot gốc |
| Trả hàng về kho | Tạo lot mới `unit_cost = return_price`, ghi ledger dòng `RETURN` |



---

## 5. Toàn vẹn Dữ liệu: ACID, Race-condition & Đồng bộ

### 5.1. ACID tại mỗi Chi nhánh

Mỗi chi nhánh có **database PostgreSQL riêng**. Mọi giao dịch nghiệp vụ diễn ra hoàn toàn trong phạm vi một database — đảm bảo ACID đầy đủ mà **không cần distributed transaction**.


```
Trong 1 transaction xác nhận hóa đơn bán hàng (CONFIRMED):

  BEGIN;
    SELECT stock_lot WHERE remaining_qty > 0 ORDER BY received_at ASC FOR UPDATE
    UPDATE stock_lot SET remaining_qty = ...             (FIFO consumption)
    INSERT INTO stock_ledger (...) VALUES (...)          (ghi lịch sử biến động — 1 dòng/lot tiêu thụ)
    UPDATE stock_on_hand SET quantity = quantity - X     (cập nhật running total)
    UPDATE sales_invoice SET status = 'CONFIRMED' ...
    UPDATE sales_invoice_line SET unit_cost_snapshot = Y ...
    UPDATE receivable_debt SET current_balance = Z ...
    UPSERT customer_product_price SET last_price = P ...
  COMMIT;   ← ROLLBACK toàn bộ nếu bất kỳ bước nào thất bại
```


### 5.2. Xử lý Race Condition — Optimistic Locking

#### Vấn đề
Hai nhân viên cùng lúc bán cùng 1 sản phẩm, cùng đọc stock → ghi đè lẫn nhau.

#### Giải pháp: Optimistic Locking bằng cột `version`

```sql
-- Trong stock_on_hand:
version  BIGINT NOT NULL DEFAULT 0  -- JPA @Version
```

```
Nhân viên A đọc: stock(version=5, qty=100)
Nhân viên B đọc: stock(version=5, qty=100)

A COMMIT trước:
  UPDATE stock_on_hand SET qty=50, version=6 WHERE version=5 → 1 row OK

B thử COMMIT:
  UPDATE stock_on_hand SET qty=50, version=6 WHERE version=5 → 0 rows!
  → OptimisticLockingFailureException → ROLLBACK → retry
```

#### Locking cho stock_lot (FIFO)

Khi thực hiện FIFO consumption, cần lock các lot đang được tiêu thụ:

```sql
SELECT * FROM stock_lot
WHERE product_id = P AND branch_id = B AND remaining_qty > 0
ORDER BY received_at ASC
FOR UPDATE  -- Pessimistic lock vì cần đảm bảo FIFO chính xác tuyệt đối
```

> Dùng **pessimistic lock** (`SELECT FOR UPDATE`) cho stock_lot vì FIFO đòi hỏi thứ tự chính xác, không thể retry nếu lot bị thay đổi giữa chừng.

### 5.3. Idempotency — Tránh Gửi Lại Tạo Trùng

```
Client gửi:
  POST /api/v1/sales-invoices/{id}/confirm
  Idempotency-Key: <uuid, cố định cho 1 lần thao tác>

Server:
  1. Hash(request payload + key) → fingerprint
  2. Tra idempotency_record:
     → Đã có + hash khớp: trả response cũ (không chạy lại)
     → Đã có + hash khác: HTTP 409
     → Chưa có: chạy nghiệp vụ, lưu fingerprint
```

### 5.4. Đồng bộ Dữ liệu HQ ↔ Chi nhánh — Logical Replication

```
HQ DB → Chi nhánh (Master Data — Read Only tại chi nhánh):
  product, category, price_list, customer, supplier (scoped by branch_id)

Chi nhánh → HQ DB (Transaction Data — để Analytics):
  sales_invoice + lines, inbound_receipt + lines,
  customer_order, goods_return
```

**Tại sao Logical Replication thay vì REST API:**
- Real-time, không qua HTTP round-trip
- Chi nhánh offline vẫn hoạt động — catch up khi reconnect
- Analytics HQ đọc replicated data, không ảnh hưởng DB nghiệp vụ chi nhánh

### 5.5. Snapshot — Bất biến của Dữ liệu Lịch sử

| Cột Snapshot | Bảng | Lý do |
|---|---|---|
| `unit_cost_snapshot` | `sales_invoice_line` | Cost của lot FIFO tại thời điểm bán — cố định vĩnh viễn |
| `previous_debt` | `sales_invoice` | Công nợ trước giao dịch — in hóa đơn đúng |
| `total_amount` | `sales_invoice` | Tổng tiền — cố định pháp lý |
| `remaining_debt` | `sales_invoice` | Công nợ phát sinh — cố định pháp lý |

---

## 6. Các Quy ước Chung Toàn Hệ thống

### 6.1. Audit Trail

Mọi thao tác quan trọng đều được ghi vào `audit_log` (snapshot JSONB before/after). Bảng này chỉ dành cho **audit nghiệp vụ** — không dùng cho system log/debug.

### 6.2. Soft Delete

| Bảng | Cơ chế vô hiệu hóa |
|---|---|
| `product`, `category`, `branch`, `user_account` | `is_active = false` |
| Hóa đơn, phiếu nhập, phiếu trả | `status = 'CANCELLED'` |

### 6.3. Timestamp Chuẩn

Mọi bảng nghiệp vụ có `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`, và `confirmed_at TIMESTAMPTZ` (với bảng có workflow). Dùng `TIMESTAMPTZ` (UTC) để đảm bảo chính xác đa timezone.

### 6.4. Primary Key Convention

| Loại bảng | Kiểu PK | Lý do |
|---|---|---|
| Master data | `BIGSERIAL` | Hiệu năng JOIN, index B-Tree tối ưu |
| Transaction data | `UUID` | Tránh xung đột PK khi hợp nhất từ nhiều DB chi nhánh về HQ |
| Lookup / Reference | `SMALLSERIAL` | Bảng nhỏ, tiết kiệm bộ nhớ |

### 6.5. Chuẩn hóa Database — 3NF + Denormalize có chủ đích

| Điểm Denormalize | Lý do |
|---|---|
| `product.attributes_cache` (JSONB) | Tối ưu đọc trang public, tránh JOIN nhiều bảng EAV |
| `sales_invoice.previous_debt/total/remaining` | Snapshot pháp lý, bất biến |
| `sales_invoice_line.unit_cost_snapshot` | Báo cáo lợi nhuận ổn định theo FIFO |
| `customer_product_price.last_price` | Hot path: gợi ý giá nhanh |
| `stock_on_hand.quantity` | Running balance — không SUM toàn bộ lịch sử mỗi lần đọc |
| `stock_ledger.balance_qty / balance_value` | Running balance per movement — đối soát nhanh không cần replay toàn bộ ledger |
| `receivable_debt.current_balance` | Running balance công nợ |
| `audit_log.before_data/after_data` | Write-heavy log, không cần query field-level |

---

## 7. Bảng Tóm tắt Quyết định Kỹ thuật

| # | Yêu cầu Nghiệp vụ | Quyết định Kỹ thuật | Công nghệ / Pattern |
|---|---|---|---|
| 1 | Nhiều chi nhánh, kho và công nợ riêng | Mỗi chi nhánh có PostgreSQL riêng | Hub-and-Spoke DB, Logical Replication |
| 2 | Giao dịch bán hàng phải toàn vẹn | Tất cả thao tác CONFIRM trong 1 DB transaction | PostgreSQL ACID, Spring `@Transactional` |
| 3 | Race condition khi nhiều người bán cùng lúc | Optimistic lock `stock_on_hand.version` + Pessimistic lock `stock_lot` (FOR UPDATE) | JPA `@Version`, retry on `OptimisticLockException` |
| 4 | Nhấn nút 2 lần không tạo 2 hóa đơn | Idempotency key + request hash | AOP `IdempotencyAspect`, `idempotency_record` |
| 5 | Giá bán linh hoạt theo khách hàng | Gợi ý từ `customer_product_price`, cho phép override | `is_price_overridden`, `default_unit_price` vs `unit_price` |
| 6 | Lịch sử giá không bị mất | Append-only theo `effective_date` | `price_list` không UPDATE bản cũ |
| 7 | Hóa đơn cũ không thay đổi số liệu | Snapshot tại thời điểm CONFIRM | Cột `_snapshot`, bất biến sau CONFIRM |
| 8 | Tồn kho âm có kiểm soát | Không có CHECK `quantity >= 0` | Quyết định nghiệp vụ |
| 9 | Lợi nhuận phản ánh đúng từng lô nhập | FIFO: tiêu thụ lot cũ nhất trước, snapshot cost per lot | Bảng `stock_lot`, `FOR UPDATE`, `unit_cost_snapshot` |
| 10 | Biết giao dịch nào tạo nên tồn kho hiện tại | Stock Ledger: ghi mỗi biến động kho thành 1 dòng, có running balance | Bảng `stock_ledger`, append-only, ref về chứng từ gốc |
| 11 | Sản phẩm có thuộc tính động | EAV + JSONB cache | `product_attribute_value` + `product.attributes_cache` |
| 12 | Nhân viên chi nhánh không thấy data chi nhánh khác | RBAC scoped theo branch, JWT chứa branchId | `@BranchScopedAspect`, `UserBranchRole` |
| 13 | NCC tập trung HQ, hiển thị scoped theo chi nhánh | `supplier` là HQ master có `branch_id`, replicate xuống | Logical Replication, read-only tại chi nhánh |
| 14 | Luân chuyển hàng giữa chi nhánh | Bán hàng thông thường (giá vốn), không có stock_transfer riêng | `sales_invoice` + `inbound_receipt` bình thường |
| 15 | Báo cáo tổng hợp không ảnh hưởng OLTP | Tách biệt Analytics DB | Star Schema, ETL định kỳ, Read Replica |
| 16 | Audit trail đầy đủ cho nghiệp vụ | Snapshot JSONB before/after | AOP `AuditAspect`, bảng `audit_log` |
| 17 | Log hệ thống không làm chậm DB | Không ghi system log vào PostgreSQL | SLF4J → file JSON → Loki/ELK |

---

*Tài liệu này là nguồn tham chiếu chính thức cho toàn bộ quyết định thiết kế hệ thống.*  
*Mọi thay đổi yêu cầu nghiệp vụ phải được phản ánh vào tài liệu này TRƯỚC khi thực hiện thay đổi code hoặc schema.*

