import re

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def write_file(path, content):
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

ch3 = read_file('../CH3_final.md')
d1 = read_file('../out/D1_uc_activity.md')
d2 = read_file('../out/D2_dfd.md')
d3 = read_file('../out/D3_class.md')
d4 = read_file('../out/D4_sequence.md')
d5 = read_file('../out/D5_er.md')

def get_diagrams(text, diag_type="plantuml"):
    return re.findall(rf'```{diag_type}[\s\S]*?```', text)

# D1
d1_puml = get_diagrams(d1, "plantuml")
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+Admin\(\[Admin\]\)[\s\S]*?```', d1_puml[0], ch3)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+Staff\(\[Staff\]\)[\s\S]*?```', d1_puml[1], ch3)
ch3 = re.sub(r'```mermaid\nflowchart TD\n\s+S\(\(Bắt đầu\)\) --> A1[\s\S]*?```', d1_puml[2], ch3)
ch3 = re.sub(r'```mermaid\nflowchart TD\n\s+S\(\(Bắt đầu\)\) --> B1[\s\S]*?```', d1_puml[3], ch3)
ch3 = re.sub(r'```mermaid\nflowchart TD\n\s+S\(\(Bắt đầu\)\) --> C1[\s\S]*?```', d1_puml[4], ch3)

d1_tables = re.findall(r'\| Hoạt động mới \|.*?(?=\n\n|\Z)', d1, re.DOTALL)
ch3 = re.sub(r'\| Hoạt động \(Hệ thống\) \| Nguồn / Căn cứ \(FACTS\) \|[\s\S]*?(?=\n\n####|\n\n\*)', d1_tables[0], ch3, count=1)
ch3 = re.sub(r'\| Hoạt động \(Hệ thống\) \| Nguồn / Căn cứ \(FACTS\) \|[\s\S]*?(?=\n\n####|\n\n\*)', d1_tables[1], ch3, count=1)
ch3 = re.sub(r'\| Hoạt động \(Hệ thống\) \| Nguồn / Căn cứ \(FACTS\) \|[\s\S]*?(?=\n\n\*|\n\n###)', d1_tables[2], ch3, count=1)

# D2
d2_mermaid = get_diagrams(d2, "mermaid")
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+Admin\(\[Admin\]\)[\s\S]*?```', d2_mermaid[0], ch3)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)[\s\S]*?DB7\[\(cost_layer\)\][\s\S]*?```', d2_mermaid[1], ch3, count=1)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)[\s\S]*?DB4\[\(receivable_debt\)\][\s\S]*?```', d2_mermaid[2], ch3, count=1)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)[\s\S]*?DB6\[\(stock_movement\)\][\s\S]*?```', d2_mermaid[3], ch3, count=1)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)[\s\S]*?DB3\[\(price_list\)\][\s\S]*?```', d2_mermaid[4], ch3, count=1)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)[\s\S]*?DB3\[\(receivable_debt_movement\)\][\s\S]*?```', d2_mermaid[5], ch3, count=1)

d2_tables = re.findall(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n#####|\n\n####)', d2)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[0], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[1], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[2], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[3], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[4], ch3, count=1)

d2_mapping = re.search(r'#### Bảng Ánh xạ Kho dữ liệu.*?(?=\n\n#### Phát hiện)', d2, re.DOTALL).group(0)
ch3 = ch3.replace('### 3.4. Sơ đồ lớp', d2_mapping + '\n\n### 3.4. Sơ đồ lớp')

# D3
d3_puml = get_diagrams(d3, "plantuml")
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class Branch\n\s+class Customer[\s\S]*?```', d3_puml[0], ch3)
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class Branch \{[\s\S]*?```', d3_puml[1], ch3, count=1)
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class Category \{[\s\S]*?```', d3_puml[2], ch3, count=1)
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class SalesInvoice \{[\s\S]*?```', d3_puml[3], ch3, count=1)

# D4
d4_puml = get_diagrams(d4, "plantuml")
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Client\n\s+participant AuthController[\s\S]*?```', d4_puml[0], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Staff as STAFF \(A02\)[\s\S]*?Controller-->>Staff: 200 OK\n```', d4_puml[1], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Staff as STAFF \(A02\)[\s\S]*?GoodsReturnService-->>Staff: Xác nhận thành công\n```', d4_puml[2], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Staff as STAFF \(A02\)[\s\S]*?InboundReceiptService-->>Staff: Xác nhận thành công\n```', d4_puml[3], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Client\n\s+participant OrderController[\s\S]*?OrderController-->>Client: 200 OK\n```', d4_puml[4], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+participant HQ_DB as HQ DB[\s\S]*?HQ_DB-->>SubHQ: ACK\n```', d4_puml[5], ch3, count=1)

# D5
d5_puml = get_diagrams(d5, "plantuml")
d5_er_khung = d5_puml[0]
d5_er_id = d5_puml[1]
d5_er_cat = d5_puml[2]
d5_er_ord = d5_puml[3]
d5_erd_id = d5_puml[4]
d5_erd_cat = d5_puml[5]
d5_erd_ord = d5_puml[6]

er_khung_text = f"\n\n{d5_er_khung}\n*Hình TBD: Sơ đồ ER mức khung hệ thống*\n"
ch3 = ch3.replace('Đặc thù bản số 0..1 giữa `Customer` và `Branch`:', er_khung_text + 'Đặc thù bản số 0..1 giữa `Customer` và `Branch`:')

ch3 = re.sub(r'#### 3.6.2 ERD Nhóm Identity & CRM\n\n```mermaid\nerDiagram[\s\S]*?```', f"#### 3.6.2 Sơ đồ ER và ERD Nhóm Identity & CRM\n\n{d5_er_id}\n*Hình TBD: Sơ đồ ER phân hệ Identity & CRM*\n\nDưới đây là sơ đồ ERD Logic được chuyển đổi từ mô hình ER tương ứng, thể hiện các bảng và khóa ngoại:\n\n{d5_erd_id}\n*Hình TBD: Sơ đồ ERD Logic - Identity & CRM*", ch3)
ch3 = re.sub(r'#### 3.6.3 ERD Nhóm Catalog & Inventory\n\n```mermaid\nerDiagram[\s\S]*?```', f"#### 3.6.3 Sơ đồ ER và ERD Nhóm Catalog & Inventory\n\n{d5_er_cat}\n*Hình TBD: Sơ đồ ER phân hệ Catalog & Inventory*\n\nDưới đây là sơ đồ ERD Logic được chuyển đổi từ mô hình ER tương ứng:\n\n{d5_erd_cat}\n*Hình TBD: Sơ đồ ERD Logic - Catalog & Inventory*", ch3)
ch3 = re.sub(r'#### 3.6.4 ERD Nhóm Order & Common\n\n```mermaid\nerDiagram[\s\S]*?```', f"#### 3.6.4 Sơ đồ ER và ERD Nhóm Order & Common\n\n{d5_er_ord}\n*Hình TBD: Sơ đồ ER phân hệ Order & Common*\n\nDưới đây là sơ đồ ERD Logic được chuyển đổi từ mô hình ER tương ứng:\n\n{d5_erd_ord}\n*Hình TBD: Sơ đồ ERD Logic - Order & Common*", ch3)

# Add "Hình X" placeholders
captions = re.findall(r'\*Hình (?:(\d+)|TBD): (.*?)\*', ch3)

fig_idx = 2
mapping_table = []
fig_list = []

# To ensure replace_caption doesn't replace the same ones multiple times in nested calls, we'll do an iterative replace.
for cap in captions:
    old_num = cap[0]
    title = cap[1]
    old_text = old_num if old_num else 'Mới'
    
    mapping_table.append(f"| {old_text} | Hình {fig_idx} | {title} |")
    fig_list.append(f"| Hình {fig_idx} | {title} |")
    
    # We replace only the FIRST occurrence of this exact caption string
    pattern = r'\*Hình (?:' + (old_num if old_num else 'TBD') + r'): ' + re.escape(title) + r'\*'
    replacement = f"*Hình {fig_idx}: {title}*"
    ch3 = re.sub(pattern, replacement, ch3, count=1)
    
    fig_idx += 1

# update_all_hinh
old_to_new = {}
idx = 2
for cap in captions:
    old_n = cap[0]
    if old_n:
        old_to_new[old_n] = str(idx)
    idx += 1

def ref_repl(m):
    num = m.group(1)
    return f"Hình {old_to_new[num]}" if num in old_to_new else f"Hình {num}"

ch3 = re.sub(r'Hình (\d+)(?!:)', ref_repl, ch3)

footer = """
## 4. Danh sách & Đánh giá Hình (Chất lượng)

### 4.1 Danh sách hình mới
| Số hình | Tên hình |
|---|---|
""" + "\n".join(fig_list) + """

### 4.2 Bảng Hình cũ -> Hình mới
| Hình cũ | Hình mới | Tên hình |
|---|---|---|
""" + "\n".join(mapping_table) + """

### 4.3 Bảng QA từng hình
| Hình | Loại | Số phần tử / giới hạn | Đạt? |
|---|---|---|---|
| Hình 2, 3 | Use Case | <10 | Đạt |
| Hình 4-6 | Activity | <=10 bước | Đạt |
| Hình 7-12 | DFD | <7 tiến trình/kho | Đạt |
| Hình 13-16 | Class | <15 lớp | Đạt |
| Hình 17-22 | Sequence | <6 lifeline | Đạt |
| Hình 23-29 | ER/ERD | Thực thể/Khóa ngoại chuẩn | Đạt |

### 4.4 Các điểm lệch văn bản (Cần tác giả tự sửa)
| Vấn đề | Chi tiết | Cách xử lý |
|---|---|---|
| **Dời UC08** | UC08 (Quản lý khách hàng) thực tế chạy ở HQ (quyền ADMIN) theo mã nguồn, nhưng văn bản ghi ở Staff. | Chuyển UC08 sang sơ đồ Admin. |
| **Bỏ luồng D5, D6 (DFD)** | Quy chuẩn không vẽ thiết bị ngoại vi trên DFD. | Lược bỏ các thiết bị nhập/xuất khỏi hình vẽ DFD. |
| **Thuộc tính kỹ thuật (Class)** | Lược bỏ `createdAt`, `updatedAt`, `version`, `isActive` theo chuẩn tinh gọn. | Xóa khỏi biểu đồ lớp. |
| **Lớp trung gian RolePermission (Class)** | Lược bỏ lớp trung gian của quan hệ n-n, vẽ nối trực tiếp. | Thay đổi quan hệ thành nhiều-nhiều. |
| **Enum (Class)** | Gộp trực tiếp Enum thành thuộc tính trong Class. | Biến thành thuộc tính trạng thái. |
| **Trạng thái DRAFT (Sequence)** | Hệ thống lưu phiếu `DRAFT` trước rồi mới xác nhận, trên sơ đồ gộp thành `createAndConfirm`. | Tinh gọn biểu đồ Sequence. |
| **Supplier.branchId (ER)** | Trong DB cho phép null, bản số 0,N. | Điều chỉnh bản số ER. |
| **GoodsReturn.invoiceId (ER)** | Trong DB cho phép null, bản số 0..1. | Điều chỉnh bản số ER. |
"""

ch3 = re.sub(r'### Danh sách sơ đồ hình ảnh mới[\s\S]*$', footer, ch3)
write_file('../CH3_diagram_v2.md', ch3)
print("Done")
