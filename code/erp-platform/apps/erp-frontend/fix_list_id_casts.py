import re

def update_file(filename, old_str, new_str):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old_str, new_str)
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

update_file('src/components/catalog/CustomerList.tsx', 'updateCustomer({ id: formData.id, payload }', 'updateCustomer({ id: String(formData.id), payload }')
update_file('src/components/catalog/ProductList.tsx', 'updateProduct({ id: formData.id, payload }', 'updateProduct({ id: Number(formData.id), payload }')
update_file('src/components/catalog/SupplierList.tsx', 'updateSupplier({ id: formData.id, payload }', 'updateSupplier({ id: String(formData.id), payload }')
update_file('src/components/catalog/SupplierList.tsx', 'onClick={() => setConfirmState({ isOpen: true, id: supplier.id! })}', 'onClick={() => setConfirmState({ isOpen: true, id: String(supplier.id) })}')

