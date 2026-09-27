import re

def update_file(filename, old_str, new_str):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old_str, new_str)
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

update_file('src/components/catalog/CustomerList.tsx', 'setSelectedCustomerId(id)', 'setSelectedCustomerId(String(id))')
update_file('src/components/catalog/ProductList.tsx', 'setSelectedProductId(id)', 'setSelectedProductId(Number(id))')
update_file('src/components/catalog/SupplierList.tsx', 'setSelectedSupplierId(id)', 'setSelectedSupplierId(String(id))')

