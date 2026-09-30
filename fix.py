import re

file_path = r'D:\Docs\CodeProject\DOAN\code\docs\CH3_final.md'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: RolePermission PK in ERD and Table
content = re.sub(r'RolePermission \{\s*Long id PK', r'RolePermission {\n        RolePermissionId id PK', content)
content = content.replace('| RolePermission | id | Long | Khóa chính', '| RolePermission | id | RolePermissionId | Khóa chính phức hợp')

# Fix 2: Hình 8 DFD UC10
content = content.replace('DB5[(receivable_debt)]', 'DB5[(receivable_debt)]\n    DB6[(stock_movement)]\n    DB7[(cost_layer)]')
content = content.replace('P -- D4 --> DB5', 'P -- D4 --> DB5\n    P -- D4 --> DB6\n    DB7 -- D3 --> P\n    P -- D4 --> DB7')
content = content.replace('tồn kho cập nhật (stock_on_hand [E21]), và biến động công nợ (eceivable_debt [E24])', 'tồn kho cập nhật (stock_on_hand [E21]), biến động công nợ (eceivable_debt [E24]), biến động kho (stock_movement [E22]), và cập nhật giá vốn (cost_layer [E23])')

# Fix 3: Hình 10 DFD UC12
content = content.replace('DB5[(cost_layer)]', 'DB5[(cost_layer)]\n    DB6[(stock_movement)]')
content = content.replace('P -- D4 --> DB5', 'P -- D4 --> DB5\n    P -- D4 --> DB6')
content = content.replace('và tạo lớp giá nhập mới (cost_layer [E23])', 'tạo lớp giá nhập mới (cost_layer [E23]), và tạo biến động kho (stock_movement [E22])')

# Fix 4: Hình 11 DFD UC09
content = content.replace('DB2[(customer_product_price)]', 'DB2[(customer_product_price)]\n    DB3[(price_list)]')
content = content.replace('DB2 -- D3 --> P', 'DB2 -- D3 --> P\n    DB3 -- D3 --> P')
content = content.replace('(customer_product_price [E18]).', '(customer_product_price [E18]) và bảng giá chung (price_list [E05]).')

# Fix 5: Hình 20 Sequence Inbound
# We need to find the sequence diagram for Hình 20.
content = content.replace('actor Admin as A01', 'actor Staff as A02')
content = content.replace('participant Admin', 'participant Staff')
content = content.replace('Admin->>', 'Staff->>')
content = content.replace('-->>Admin', '-->>Staff')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done")
