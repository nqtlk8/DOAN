import sys

def check_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    open_p = content.count('<DataState')
    close_p = content.count('</DataState>')
    print(f"{filepath}: open {open_p}, close {close_p}")

files_to_wrap = [
    'src/components/catalog/CustomerList.tsx',
    'src/components/crm/DebtList.tsx',
    'src/components/inventory/StockList.tsx'
]

for file in files_to_wrap:
    check_file(file)
