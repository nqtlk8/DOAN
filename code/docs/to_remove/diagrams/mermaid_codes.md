### Hinh_2_1_KienTrucTongThe\n\n```mermaid\nflowchart TD
    subgraph HQ["Trụ sở chính (Headquarter)"]
        HQ_Nginx["Nginx (Load Balancer/Reverse Proxy)"]
        HQ_App["Spring Boot (HQ Profile)"]
        HQ_DB[("PostgreSQL\n(HQ DB)")]
        HQ_Redis[("Redis\n(Cache/Session)")]
        HQ_Nginx --> HQ_App
        HQ_App --> HQ_DB
        HQ_App --> HQ_Redis
    end

    subgraph Branch1["Chi nhánh 1 (Branch)"]
        B1_Nginx["Nginx"]
        B1_App["Spring Boot (Branch Profile)"]
        B1_DB[("PostgreSQL\n(Branch 1 DB)")]
        B1_Nginx --> B1_App
        B1_App --> B1_DB
    end

    subgraph BranchN["Chi nhánh N (Branch)"]
        BN_Nginx["Nginx"]
        BN_App["Spring Boot (Branch Profile)"]
        BN_DB[("PostgreSQL\n(Branch N DB)")]
        BN_Nginx --> BN_App
        BN_App --> BN_DB
    end

    HQ_DB <-->|Logical Replication| B1_DB
    HQ_DB <-->|Logical Replication| BN_DB
\n```\n\n### Hinh_2_2_KienTrucModule\n\n```mermaid\nflowchart TD
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
\n```\n\n### Hinh_2_4_LogicalReplication\n\n```mermaid\nflowchart LR
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
        T_Cat_B[("Catalog, Users, Price\n(ReadOnly)")]
        T_Trans_B[("Invoices, Receipts")]
        B_SUB --> T_Cat_B
        T_Trans_B --> B_PUB
    end

    HQ_PUB == Push ==> B_SUB
    B_PUB == Push ==> HQ_SUB
\n```\n\n### Hinh_2_5_KienTrucBaoMat\n\n```mermaid\nflowchart TD
    subgraph HQ["HQ (Centralized Auth)"]
        AuthService["Auth Service\n(Login, Generate JWT)"]
        HQ_Key["Private Key (RS256)"]
        AuthService --> HQ_Key
    end

    subgraph Branch["Branch (Offline Verification)"]
        SecFilter["Security Filter\n(Validate JWT)"]
        Pub_Key["Public Key (RS256)"]
        SecFilter --> Pub_Key
    end

    Client["User / Staff"]
    Client -- "1. POST /login" --> AuthService
    AuthService -- "2. Trả về JWT Token" --> Client
    Client -- "3. Gọi API kèm JWT" --> SecFilter
    SecFilter -- "4. Xác thực ngoại tuyến (Offline)" --> Pub_Key
\n```\n\n