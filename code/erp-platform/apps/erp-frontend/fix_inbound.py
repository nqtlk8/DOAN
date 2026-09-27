import re

with open('src/components/inventory/InboundReceiptModule.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("ApiService.Catalog.getSuppliers().then(setSuppliers)", "ApiService.Catalog.getSuppliers().then(setSuppliers as any)")
content = content.replace("ApiService.Catalog.getProducts().then(setProducts)", "ApiService.Catalog.getProducts().then(setProducts as any)")

with open('src/components/inventory/InboundReceiptModule.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
