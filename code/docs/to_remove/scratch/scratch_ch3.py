import re

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def write_file(path, content):
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

ch3 = read_file('CH3_final.md')
d1 = read_file('out/D1_uc_activity.md')
d2 = read_file('out/D2_dfd.md')
d3 = read_file('out/D3_class.md')
d4 = read_file('out/D4_sequence.md')
d5 = read_file('out/D5_er.md')

# Helper to extract code blocks
def extract_block(text, header_keyword, next_header='##'):
    # This is a bit fragile, let's use regex based on specific titles if needed
    pass

# Alternatively, I can just use regex to find plantuml/mermaid blocks in D1-D5
import re

def get_diagrams(text, diag_type="plantuml"):
    return re.findall(rf'```{diag_type}[\s\S]*?```', text)

# D1
d1_puml = get_diagrams(d1, "plantuml")
# ch3 has mermaid blocks for UC and Activity
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+Admin\(\[Admin\]\)[\s\S]*?```', d1_puml[0], ch3)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+Staff\(\[Staff\]\)[\s\S]*?```', d1_puml[1], ch3)
ch3 = re.sub(r'```mermaid\nflowchart TD\n\s+S\(\(Bắt đầu\)\) --> A1[\s\S]*?```', d1_puml[2], ch3)
ch3 = re.sub(r'```mermaid\nflowchart TD\n\s+S\(\(Bắt đầu\)\) --> B1[\s\S]*?```', d1_puml[3], ch3)
ch3 = re.sub(r'```mermaid\nflowchart TD\n\s+S\(\(Bắt đầu\)\) --> C1[\s\S]*?```', d1_puml[4], ch3)

# Replace activity tables
d1_tables = re.findall(r'\| Hoạt động mới \|.*?(?=\n\n|\Z)', d1, re.DOTALL)
ch3 = re.sub(r'\| Hoạt động \(Hệ thống\) \| Nguồn / Căn cứ \(FACTS\) \|[\s\S]*?(?=\n\n#|\n\n\*)', d1_tables[0], ch3, count=1)
ch3 = re.sub(r'\| Hoạt động \(Hệ thống\) \| Nguồn / Căn cứ \(FACTS\) \|[\s\S]*?(?=\n\n#|\n\n\*)', d1_tables[1], ch3, count=1)
ch3 = re.sub(r'\| Hoạt động \(Hệ thống\) \| Nguồn / Căn cứ \(FACTS\) \|[\s\S]*?(?=\n\n\*|\n\n#)', d1_tables[2], ch3, count=1)

# D2
d2_mermaid = get_diagrams(d2, "mermaid")
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+Admin\(\[Admin\]\)\n\s+Staff\(\[Staff\]\)[\s\S]*?```', d2_mermaid[0], ch3)
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)\n\s+IN\[Thiết bị nhập\][\s\S]*?```', d2_mermaid[1], ch3, count=1) # DFD 10
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)\n\s+IN\[Thiết bị nhập\][\s\S]*?```', d2_mermaid[2], ch3, count=1) # DFD 11
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)\n\s+IN\[Thiết bị nhập\][\s\S]*?```', d2_mermaid[3], ch3, count=1) # DFD 12
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)\n\s+IN\[Thiết bị nhập\][\s\S]*?```', d2_mermaid[4], ch3, count=1) # DFD 09
ch3 = re.sub(r'```mermaid\nflowchart LR\n\s+U\(\[Staff\]\)\n\s+IN\[Thiết bị nhập\][\s\S]*?```', d2_mermaid[5], ch3, count=1) # DFD 14

d2_tables = re.findall(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[0], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[1], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[2], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[3], ch3, count=1)
ch3 = re.sub(r'\| Dòng \| Ý nghĩa \|[\s\S]*?(?=\n\n\*\*Thuật toán)', d2_tables[4], ch3, count=1)

# Add Bảng ánh xạ kho dữ liệu to DFD
d2_mapping = re.search(r'#### Bảng Ánh xạ Kho dữ liệu.*?(?=\n#### Phát hiện)', d2, re.DOTALL).group(0)
ch3 = ch3.replace('3.4. Sơ đồ lớp', d2_mapping + '\n\n### 3.4. Sơ đồ lớp')

# D3
d3_puml = get_diagrams(d3, "plantuml")
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class Branch\n\s+class Customer[\s\S]*?```', d3_puml[0], ch3)
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class Branch \{[\s\S]*?```', d3_puml[1], ch3, count=1)
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class Category \{[\s\S]*?```', d3_puml[2], ch3, count=1)
ch3 = re.sub(r'```mermaid\nclassDiagram\n\s+class SalesInvoice \{[\s\S]*?```', d3_puml[3], ch3, count=1)

# D4
d4_puml = get_diagrams(d4, "plantuml")
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Client\n\s+participant AuthController[\s\S]*?```', d4_puml[0], ch3)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Staff as STAFF \(A02\)[\s\S]*?SalesInvoiceService-->>Controller: Hóa đơn đã xác nhận[\s\S]*?```', d4_puml[1], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Staff as STAFF \(A02\)[\s\S]*?GoodsReturnService-->>Staff: Xác nhận thành công\n```', d4_puml[2], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Staff as STAFF \(A02\)[\s\S]*?InboundReceiptService-->>Staff: Xác nhận thành công\n```', d4_puml[3], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+actor Client\n\s+participant OrderController[\s\S]*?```', d4_puml[4], ch3, count=1)
ch3 = re.sub(r'```mermaid\nsequenceDiagram\n\s+autonumber\n\s+participant HQ_DB as HQ DB[\s\S]*?```', d4_puml[5], ch3, count=1)

# D5
d5_puml = get_diagrams(d5, "plantuml") # index 0,1,2,3 for ER Chen, 4,5,6 for ERD Logic
d5_er_khung = d5_puml[0]
d5_er_id = d5_puml[1]
d5_er_cat = d5_puml[2]
d5_er_ord = d5_puml[3]
d5_erd_id = d5_puml[4]
d5_erd_cat = d5_puml[5]
d5_erd_ord = d5_puml[6]

# Section 3.6.1 Insert ER khung
er_khung_text = f"\n\n{d5_er_khung}\n*Hình 23: Sơ đồ ER mức khung hệ thống*\n"
ch3 = ch3.replace('Đặc thù bản số 0..1 giữa `Customer` và `Branch`:', er_khung_text + 'Đặc thù bản số 0..1 giữa `Customer` và `Branch`:')

# 3.6.2 ERD Nhóm Identity & CRM
ch3 = re.sub(r'#### 3.6.2 ERD Nhóm Identity & CRM\n\n```mermaid\nerDiagram[\s\S]*?```', f"#### 3.6.2 Sơ đồ ER và ERD Nhóm Identity & CRM\n\n{d5_er_id}\n*Hình 24: Sơ đồ ER phân hệ Identity & CRM*\n\nDưới đây là sơ đồ ERD Logic được chuyển đổi từ mô hình ER tương ứng, thể hiện các bảng và khóa ngoại:\n\n{d5_erd_id}", ch3)

# 3.6.3
ch3 = re.sub(r'#### 3.6.3 ERD Nhóm Catalog & Inventory\n\n```mermaid\nerDiagram[\s\S]*?```', f"#### 3.6.3 Sơ đồ ER và ERD Nhóm Catalog & Inventory\n\n{d5_er_cat}\n*Hình 26: Sơ đồ ER phân hệ Catalog & Inventory*\n\nDưới đây là sơ đồ ERD Logic được chuyển đổi từ mô hình ER tương ứng:\n\n{d5_erd_cat}", ch3)

# 3.6.4
ch3 = re.sub(r'#### 3.6.4 ERD Nhóm Order & Common\n\n```mermaid\nerDiagram[\s\S]*?```', f"#### 3.6.4 Sơ đồ ER và ERD Nhóm Order & Common\n\n{d5_er_ord}\n*Hình 28: Sơ đồ ER phân hệ Order & Common*\n\nDưới đây là sơ đồ ERD Logic được chuyển đổi từ mô hình ER tương ứng:\n\n{d5_erd_ord}", ch3)

# Note: Renumbering will be done dynamically
# Create figure map
def renumber_figures(text):
    # Find all '*Hình X:' or 'Hình X'
    fig_idx = 2
    
    # We will find all '*Hình \d+:' and replace with current fig_idx
    # Since we need to update references too, it's better to manually map or use a function.
    
    # Wait, the prompt says update everywhere. So I'll do a 2-pass regex.
    # Pass 1: find all caption lines `*Hình \d+:` to build the mapping.
    # Actually, we added some new figures, so we just enumerate them in order of appearance.
    
    captions = re.findall(r'\*Hình \d+:.*?\*', text)
    mapping = {}
    for cap in captions:
        old_num_match = re.search(r'\*Hình (\d+):', cap)
        if old_num_match:
            old_num = int(old_num_match.group(1))
            # Wait, there are multiple new figures that might have wrong old_nums (e.g., from my f-strings).
            # It's better to just replace them sequentially.
    
    new_text = text
    def repl(m):
        nonlocal fig_idx
        res = f"*Hình {fig_idx}: {m.group(1)}*"
        fig_idx += 1
        return res
    
    # But references in text also need to be updated... This is complex because we need the EXACT old->new mapping.
    pass

write_file('scratch_ch3.py', ch3)
