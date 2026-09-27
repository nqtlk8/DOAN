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

    # Add PageHeader import if PageContainer is imported
    if 'import { PageContainer }' in content and 'PageHeader' not in content:
        content = content.replace("import { PageContainer } from '../../shared/components/Page/PageContainer';", "import { PageContainer } from '../../shared/components/Page/PageContainer';\nimport { PageHeader } from '../../shared/components/Page/PageHeader';")

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
