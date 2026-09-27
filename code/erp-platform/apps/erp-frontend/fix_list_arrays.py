import re

def update_file(filename, old_str, new_str):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old_str, new_str)
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

update_file('src/components/catalog/ProductList.tsx', 'const products: Product[] = response || [];', 'const products: Product[] = (response as any) || [];')
update_file('src/components/catalog/SupplierList.tsx', 'const suppliers: Supplier[] = response || [];', 'const suppliers: Supplier[] = (response as any) || [];')
update_file('src/components/catalog/CustomerList.tsx', 'const customers: Customer[] = response || [];', 'const customers: Customer[] = (response as any) || [];')
