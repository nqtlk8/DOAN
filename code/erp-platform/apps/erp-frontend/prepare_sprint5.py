import re
import os

def update_file(filename, replacements):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

update_file('src/components/inventory/GoodsReturnModule.tsx', [
    ('InboundReceipt', 'GoodsReturn'),
    ('phi?u nh?p', 'phi?u tr?'),
    ('inbound', 'return')
])

update_file('src/components/inventory/GoodsReturnList.tsx', [
    ('InboundReceipt', 'GoodsReturn'),
    ('inboundReceipts', 'goodsReturns'),
    ('receipts', 'returns'),
    ('receiptCode', 'returnCode'),
    ('supplierName', 'customerName'),
    ('Nhà cung c?p', 'Khách hàng'),
    ('inbound-list-row', 'return-list-row'),
    ('receipt', 'retItem')
])
