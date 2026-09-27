with open('src/components/sales/SalesOrderForm.tsx', 'r', encoding='utf-8') as f:
    lines = f.readlines()
for i, line in enumerate(lines):
    if 'searchCustomers' in line or 'searchProducts' in line:
        print(f"{i+1}: {line.strip()}")
