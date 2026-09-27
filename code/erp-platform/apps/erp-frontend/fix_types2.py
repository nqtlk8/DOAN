with open('src/components/sales/SalesOrderForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("fetchData={(query) => ApiService.Catalog.searchCustomers(query)}", "fetchData={(query) => ApiService.Catalog.searchCustomers(query) as any}")
content = content.replace("fetchData={(query) => ApiService.Catalog.searchProducts(query)}", "fetchData={(query) => ApiService.Catalog.searchProducts(query) as any}")

with open('src/components/sales/SalesOrderForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
