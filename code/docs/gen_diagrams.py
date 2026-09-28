import os
import zlib
import base64
import requests
import sys

diagrams = {
    "Hinh_2_1_KienTrucTongThe": """flowchart TD
    subgraph HQ["Trụ sở chính (Headquarter)"]
        HQ_Nginx["Nginx (Load Balancer/Reverse Proxy)"]
        HQ_App["Spring Boot (HQ Profile)"]
        HQ_DB[("PostgreSQL\\n(HQ DB)")]
        HQ_Redis[("Redis\\n(Cache/Session)")]
        HQ_Nginx --> HQ_App
        HQ_App --> HQ_DB
        HQ_App --> HQ_Redis
    end

    subgraph Branch1["Chi nhánh 1 (Branch)"]
        B1_Nginx["Nginx"]
        B1_App["Spring Boot (Branch Profile)"]
        B1_DB[("PostgreSQL\\n(Branch 1 DB)")]
        B1_Nginx --> B1_App
        B1_App --> B1_DB
    end

    subgraph BranchN["Chi nhánh N (Branch)"]
        BN_Nginx["Nginx"]
        BN_App["Spring Boot (Branch Profile)"]
        BN_DB[("PostgreSQL\\n(Branch N DB)")]
        BN_Nginx --> BN_App
        BN_App --> BN_DB
    end

    HQ_DB <-->|Logical Replication| B1_DB
    HQ_DB <-->|Logical Replication| BN_DB
""",
    "Hinh_2_2_KienTrucModule": """flowchart TD
    subgraph REST_API["REST API (Controllers)"]
        API_HQ[HQ Endpoints]
        API_Branch[Branch Endpoints]
    end

    subgraph Facade_Layer["Facade Layer (Cross-Module Communication)"]
        CRM_Facade[CRM Facade]
        Inv_Facade[Inventory Facade]
    end

    subgraph Business_Modules["Business Modules (Isolated)"]
        M_Id[Identity]
        M_Cat[Catalog]
        M_CRM[CRM]
        M_Sales[Sales]
        M_Return[Goods Return]
        M_Inbound[Inbound]
        M_Inv[Inventory]
        M_Analytics[Analytics]
    end

    subgraph Data_Access["Data Access Layer"]
        Repositories[Spring Data JPA Repositories]
    end

    REST_API --> Facade_Layer
    REST_API --> Business_Modules
    Facade_Layer -.-> Business_Modules
    Business_Modules --> Data_Access
""",
    "Hinh_2_4_LogicalReplication": """flowchart LR
    subgraph HQ["Trụ sở chính (HQ Database)"]
        HQ_PUB["Publisher: MASTER_TABLES"]
        HQ_SUB["Subscriber: TRANSACTION_TABLES"]
        T_Cat[("Catalog, Users, Price")]
        T_Trans_HQ[("Invoices, Receipts")]
        T_Cat --> HQ_PUB
        HQ_SUB --> T_Trans_HQ
    end

    subgraph Branch["Chi nhánh (Branch Database)"]
        B_SUB["Subscriber: MASTER_TABLES"]
        B_PUB["Publisher: TRANSACTION_TABLES"]
        T_Cat_B[("Catalog, Users, Price\\n(ReadOnly)")]
        T_Trans_B[("Invoices, Receipts")]
        B_SUB --> T_Cat_B
        T_Trans_B --> B_PUB
    end

    HQ_PUB == Push ==> B_SUB
    B_PUB == Push ==> HQ_SUB
""",
    "Hinh_2_5_KienTrucBaoMat": """flowchart TD
    subgraph HQ["HQ (Centralized Auth)"]
        AuthService["Auth Service\\n(Login, Generate JWT)"]
        HQ_Key["Private Key (RS256)"]
        AuthService --> HQ_Key
    end

    subgraph Branch["Branch (Offline Verification)"]
        SecFilter["Security Filter\\n(Validate JWT)"]
        Pub_Key["Public Key (RS256)"]
        SecFilter --> Pub_Key
    end

    Client["User / Staff"]
    Client -- "1. POST /login" --> AuthService
    AuthService -- "2. Trả về JWT Token" --> Client
    Client -- "3. Gọi API kèm JWT" --> SecFilter
    SecFilter -- "4. Xác thực ngoại tuyến (Offline)" --> Pub_Key
""",
    "Hinh_4_ActivityBanHang": """flowchart TD
    Start((Bắt đầu)) --> InputInvoice[Nhân viên nhập hóa đơn bán hàng]
    InputInvoice --> Validate[Hệ thống kiểm tra tính hợp lệ khách hàng]
    Validate -- Hợp lệ --> Compute[Tính tổng tiền hóa đơn]
    Validate -- Không hợp lệ --> EndError((Lỗi / Dừng))
    
    Compute --> StartLoop{Còn dòng\\nsản phẩm?}
    StartLoop -- Có --> FIFO[Xuất kho theo FIFO]
    FIFO --> RecordCost[Ghi giá vốn từng dòng]
    RecordCost --> StartLoop
    
    StartLoop -- Không --> IncreaseDebt[Ghi tăng công nợ khách hàng]
    IncreaseDebt --> Prepaid{Khách có\\ntrả trước?}
    Prepaid -- Có --> DecreaseDebt[Trừ công nợ tương ứng]
    Prepaid -- Không --> Confirm[Xác nhận hóa đơn]
    DecreaseDebt --> Confirm
    
    Confirm --> EndSuccess((Hoàn thành))
""",
    "Hinh_5_ActivityTraHang": """flowchart TD
    Start((Bắt đầu)) --> InputDraft[Lập phiếu trả hàng nháp]
    InputDraft --> ReqConfirm[Yêu cầu xác nhận]
    ReqConfirm --> LockInvoice[Khóa hóa đơn gốc\\n(Pessimistic Write Lock)]
    
    LockInvoice --> CheckQty{Số lượng trả <=\\nSố lượng mua?}
    CheckQty -- Không --> UnlockErr[Báo lỗi / Mở khóa] --> EndError((Lỗi))
    CheckQty -- Có --> MarkConfirm[Đánh dấu phiếu Xác nhận]
    
    MarkConfirm --> LoopStart{Còn dòng\\nhàng hoàn trả?}
    LoopStart -- Có --> ReturnStock[Hoàn hàng vào kho]
    ReturnStock --> RecordReturn[Ghi nhận lịch sử hoàn hàng]
    RecordReturn --> LoopStart
    
    LoopStart -- Không --> DecreaseDebt[Giảm công nợ khách hàng]
    DecreaseDebt --> Notify[Thông báo hoàn thành]
    Notify --> EndSuccess((Hoàn thành))
""",
    "Hinh_6_ActivityNhapKho": """flowchart TD
    Start((Bắt đầu)) --> InputDraft[Lập phiếu nhập kho nháp]
    InputDraft --> ReqConfirm[Yêu cầu xác nhận]
    ReqConfirm --> MarkConfirm[Đánh dấu phiếu Xác nhận]
    
    MarkConfirm --> LoopStart{Còn dòng\\nsản phẩm?}
    LoopStart -- Có --> IncreaseStock[Tăng tồn kho sản phẩm]
    IncreaseStock --> RecordMovement[Ghi nhận biến động nhập]
    RecordMovement --> CreateCostLayer[Tạo lớp giá vốn mới]
    CreateCostLayer --> LoopStart
    
    LoopStart -- Không --> Notify[Thông báo kết quả]
    Notify --> EndSuccess((Hoàn thành))
""",
    "Hinh_7_DFDCap0": """flowchart LR
    Admin([Quản trị viên / Admin])
    Staff([Nhân viên Chi nhánh / Staff])
    System((Hệ thống ERP\\nĐa chi nhánh))

    Admin -- "Cấu hình Master Data\\n(Người dùng, Sản phẩm, Bảng giá)" --> System
    System -- "Báo cáo tổng hợp, Doanh thu, Tồn kho" --> Admin
    
    Staff -- "Giao dịch bán lẻ\\n(Hóa đơn, Trả hàng, Nhập kho)" --> System
    System -- "Thông tin khách hàng, Tồn kho, Công nợ cục bộ" --> Staff
"""
}

def get_kroki_url(diagram_text, format="png"):
    data = diagram_text.encode('utf-8')
    compressed = zlib.compress(data, 9)
    encoded = base64.urlsafe_b64encode(compressed).decode('ascii')
    return f"https://kroki.io/mermaid/{format}/{encoded}"

out_dir = "diagrams"
os.makedirs(out_dir, exist_ok=True)

with open(f"{out_dir}/mermaid_codes.md", "w", encoding="utf-8") as md_file:
    for name, code in diagrams.items():
        print(f"Processing {name}...")
        sys.stdout.flush()
        
        md_file.write(f"### {name}\\n\\n```mermaid\\n{code}\\n```\\n\\n")
        md_file.flush()
        
        # We only generate SVG for maximum clarity (vector quality)
        svg_url = get_kroki_url(code, "svg")
        resp = requests.get(svg_url, timeout=10)
        if resp.status_code == 200:
            with open(f"{out_dir}/{name}.svg", "wb") as f:
                f.write(resp.content)
        
        # And let's generate PNG as well so user can use it everywhere
        png_url = get_kroki_url(code, "png")
        resp_png = requests.get(png_url, timeout=10)
        if resp_png.status_code == 200:
            with open(f"{out_dir}/{name}.png", "wb") as f:
                f.write(resp_png.content)
        
print("All diagrams generated.")
