import re

with open('src/components/sales/SalesOrderForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("fetchData={ApiService.Catalog.searchCustomers}", "fetchData={ApiService.Catalog.searchCustomers as any}")
content = content.replace("fetchData={ApiService.Catalog.searchProducts}", "fetchData={ApiService.Catalog.searchProducts as any}")

with open('src/components/sales/SalesOrderForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
