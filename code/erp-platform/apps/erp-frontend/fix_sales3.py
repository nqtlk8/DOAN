import re

with open('src/components/sales/SalesOrderForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("renderCreateNew={() => null /* TODO */}", "")
content = content.replace("renderCreateNew={() => null}", "")

with open('src/components/sales/SalesOrderForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
