import re

def fix_api_call(filename, method):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(f"queryFn: () => {method}()", f"queryFn: () => {method}() as any")
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

fix_api_call('src/components/catalog/CustomerList.tsx', 'ApiService.Catalog.getCustomers')
fix_api_call('src/components/catalog/ProductList.tsx', 'ApiService.Catalog.getProducts')
fix_api_call('src/components/catalog/SupplierList.tsx', 'ApiService.Catalog.getSuppliers')

