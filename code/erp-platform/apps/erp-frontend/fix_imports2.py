import os
import re

files_to_wrap = [
    'src/components/catalog/ProductList.tsx',
    'src/components/catalog/CustomerList.tsx',
    'src/components/catalog/SupplierList.tsx',
    'src/components/inventory/StockList.tsx',
    'src/components/crm/DebtList.tsx',
    'src/components/crm/DebtStatement.tsx',
    'src/components/admin/BranchList.tsx',
    'src/components/sales/SalesList.tsx',
    'src/components/inventory/StockMovementList.tsx'
]

for filepath in files_to_wrap:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if 'PageHeader' not in content[:1000]: # check imports
        # insert at the top
        content = "import { PageHeader } from '../../shared/components/Page/PageHeader';\n" + content
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
