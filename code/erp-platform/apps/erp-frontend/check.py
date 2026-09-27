import re

with open('src/components/sales/SalesOrderForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

start_idx = content.find('<GenericDocumentForm')
end_idx = content.find('/>', start_idx)
if start_idx != -1 and end_idx != -1:
    old_form = content[start_idx:end_idx+2]
    print(old_form[:300])
    print("...")
    print(old_form[-300:])
