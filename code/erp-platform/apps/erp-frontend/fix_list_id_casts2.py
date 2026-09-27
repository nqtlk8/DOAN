import re

def update_file(filename, old_str, new_str):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old_str, new_str)
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

update_file('src/components/catalog/ProductList.tsx', 'id: formData.id,', 'id: Number(formData.id),')
update_file('src/components/catalog/SupplierList.tsx', 'id: formData.id,', 'id: String(formData.id),')

