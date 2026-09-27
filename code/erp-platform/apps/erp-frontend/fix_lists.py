import re

def update_file(filename, old_str, new_str):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old_str, new_str)
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

update_file('src/components/catalog/CustomerList.tsx', 'const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null);', 'const [selectedCustomerId, setSelectedCustomerId] = useState<string | number | null>(null);')

update_file('src/components/catalog/ProductList.tsx', 'const [selectedProductId, setSelectedProductId] = useState<number | null>(null);', 'const [selectedProductId, setSelectedProductId] = useState<string | number | null>(null);')

update_file('src/components/catalog/SupplierList.tsx', 'const [selectedSupplierId, setSelectedSupplierId] = useState<string | null>(null);', 'const [selectedSupplierId, setSelectedSupplierId] = useState<string | number | null>(null);')
update_file('src/components/catalog/SupplierList.tsx', 'handleDelete(selectedSupplierId)', 'handleDelete(String(selectedSupplierId))')

