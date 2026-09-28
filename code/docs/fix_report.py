import re

def main():
    file_path = r'D:\Docs\CodeProject\DOAN\code\docs\BAOCAO_HOANCHINH_V2.md'
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Update 1.4.1
    old_141 = """- Identity: Quản lý xác thực và phân quyền người dùng.
- Branch: Quản lý thông tin chi nhánh.
- Catalog: Quản lý sản phẩm, danh mục và bảng giá.
- CRM: Quản lý thông tin khách hàng và công nợ phải thu.
- Sales (Order): Xử lý hóa đơn bán hàng.
- Goods Return: Xử lý phiếu trả hàng.
- Inbound: Quản lý phiếu nhập kho.
- Inventory: Xử lý tồn kho, ghi nhận biến động kho và tính toán giá vốn FIFO.
- Analytics: Phân tích số liệu, dashboard và cảnh báo.
- Replication & Security: Đồng bộ dữ liệu và bảo mật hệ thống."""
    new_141 = """- Identity: Quản lý xác thực và phân quyền người dùng.
- Branch: Quản lý thông tin chi nhánh.
- Catalog: Quản lý sản phẩm, danh mục, nhà cung cấp và bảng giá.
- CRM: Quản lý thông tin khách hàng và công nợ phải thu.
- Order: Xử lý hóa đơn bán hàng (Sales) và phiếu trả hàng (Goods Return).
- Inventory: Quản lý tồn kho, tính giá vốn FIFO và nhập kho (Inbound).
- Analytics: Phân tích số liệu, dashboard và cảnh báo.
- Common: Xử lý các tiện ích chung và đảm bảo tính Idempotency của giao dịch.
- Infrastructure & System: Cơ chế hạ tầng về đồng bộ dữ liệu (Logical Replication) và bảo mật."""
    if old_141 in content:
        content = content.replace(old_141, new_141)
    else:
        print("Warning: Could not find exact text for 1.4.1")

    # 2. Update 2.3.1
    old_231 = """- Identity: Đăng nhập, làm mới và thu hồi token.
- Catalog & CRM: Quản lý danh mục, giá bán và hồ sơ, công nợ khách hàng.
- Sales & Return: Lập hóa đơn bán hàng và phiếu trả hàng.
- Inventory & Inbound: Xử lý nhập kho, tồn kho, tính giá vốn FIFO.
- Analytics: Báo cáo, cảnh báo, dashboard tổng hợp."""
    new_231 = """- Identity & Branch: Đăng nhập, thu hồi token, quản lý thông tin chi nhánh.
- Catalog: Quản lý danh mục, sản phẩm, bảng giá và nhà cung cấp.
- CRM: Quản lý hồ sơ khách hàng và theo dõi công nợ.
- Order (Sales & Goods Return): Lập hóa đơn bán hàng và xử lý phiếu trả hàng.
- Inventory (kèm Inbound): Xử lý nhập kho, quản lý tồn kho và tính giá vốn FIFO.
- Analytics: Báo cáo, cảnh báo, dashboard tổng hợp.
- Common & Infrastructure: Xử lý đồng bộ dữ liệu, bảo mật và lưu vết giao dịch."""
    if old_231 in content:
        content = content.replace(old_231, new_231)
    else:
        print("Warning: Could not find exact text for 2.3.1")

    # 3. Update 2.5.2
    old_252 = "Ứng dụng Backend được thiết kế với 9 module. Các module giao tiếp qua Facade để hạn chế sự phụ thuộc."
    new_252 = """Ứng dụng Backend được thiết kế theo kiến trúc Modular Monolith với 9 module (package) chính. Các module giao tiếp qua Facade để hạn chế sự phụ thuộc. Các nghiệp vụ như Bán hàng (Sales) và Trả hàng (Goods Return) được quy tụ về module **Order**; Nhập hàng (Inbound) được quy tụ về module **Inventory**. Các cơ chế Replication & Security được xếp vào **Infrastructure & System**.

Dưới đây là bảng chuẩn hóa danh sách các module bám sát mã nguồn thực tế:

| STT | Tên Module | Package | Chức năng chính | Entity chính | Nhóm Sơ đồ (Class/ERD) |
|---|---|---|---|---|---|
| 1 | Identity | `identity` | Quản lý người dùng, phân quyền, cấp token | `UserAccount`, `Role`, `Permission` | Identity & CRM |
| 2 | Branch | `branch` | Quản lý thông tin và cấu hình chi nhánh | `Branch` | Identity & CRM |
| 3 | CRM | `crm` | Quản lý khách hàng, công nợ phải thu | `Customer`, `ReceivableDebt`, `ReceivableDebtMovement` | Identity & CRM |
| 4 | Catalog | `catalog` | Quản lý sản phẩm, danh mục, bảng giá, nhà cung cấp | `Product`, `Category`, `Supplier`, `PriceList` | Catalog & Inventory |
| 5 | Inventory | `inventory` | Nhập kho (Inbound), quản lý tồn kho, tính giá vốn FIFO | `InboundReceipt`, `StockOnHand`, `StockMovement`, `CostLayer` | Catalog & Inventory |
| 6 | Order | `order` | Bán hàng (Sales), Trả hàng (Goods Return), giá riêng | `SalesInvoice`, `GoodsReturn`, `CustomerProductPrice` | Order & Common |
| 7 | Analytics | `analytics` | Báo cáo, thống kê, Dashboard | `FactSales`, `DimBranch` (Dữ liệu OLAP) | Không vẽ (OLAP) |
| 8 | Common | `common` | Tiện ích chung, lưu vết giao dịch (Idempotency) | `IdempotencyRecord`, `BaseEntity` | Order & Common |
| 9 | Infrastructure & System | `infrastructure`, `system`| Cơ chế hạ tầng: Redis, Logical Replication, Security | Không có Entity nghiệp vụ | Không vẽ |"""
    if old_252 in content:
        content = content.replace(old_252, new_252)
    else:
        print("Warning: Could not find exact text for 2.5.2")

    # 4. Update 3.4.1 and 3.4.5
    content = content.replace(
        "Do hệ thống bao gồm 24 lớp thực thể cốt lõi, sơ đồ được chia thành 1 sơ đồ tổng quát và 3 sơ đồ chi tiết theo các cụm chức năng: Identity & CRM, Catalog & Inventory, Order.",
        "Do hệ thống bao gồm 24 lớp thực thể cốt lõi, sơ đồ được chia thành 1 sơ đồ tổng quát và 3 sơ đồ chi tiết theo các cụm chức năng: Identity & CRM, Catalog & Inventory, Order & Common."
    )
    
    content = content.replace(
        "#### 3.4.5 Sơ đồ chi tiết cụm Order",
        "#### 3.4.5 Sơ đồ chi tiết cụm Order & Common"
    )
    
    # 5. Fix "Order" -> "Order & Common" in lists of figures
    content = content.replace(
        "| Hình 12 | Sơ đồ lớp chi tiết cụm Order |",
        "| Hình 12 | Sơ đồ lớp chi tiết cụm Order & Common |"
    )
    content = content.replace(
        "| 16 | Hình 12 | Sơ đồ lớp chi tiết cụm Order |",
        "| 16 | Hình 12 | Sơ đồ lớp chi tiết cụm Order & Common |"
    )

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

    print("Successfully updated the markdown file.")

if __name__ == "__main__":
    main()
