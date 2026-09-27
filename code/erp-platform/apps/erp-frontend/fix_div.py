import os

files_to_wrap = [
    'src/components/catalog/ProductList.tsx',
    'src/components/catalog/CustomerList.tsx',
    'src/components/catalog/SupplierList.tsx',
    'src/components/inventory/StockList.tsx',
    'src/components/crm/DebtList.tsx',
    'src/components/crm/DebtStatement.tsx',
    'src/components/sales/SalesList.tsx',
    'src/components/inventory/StockMovementList.tsx'
]

for filepath in files_to_wrap:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Remove the extra </div> before </PageContainer>
    content = content.replace('    </div>\n    </PageContainer>', '    </PageContainer>')
    content = content.replace('    </div>\n  </PageContainer>', '  </PageContainer>')
    content = content.replace('  </div>\n</PageContainer>', '</PageContainer>')
    
    # CustomerList has an extra </DataState> we added by accident
    content = content.replace('</DataState>\n        </DataState>\n      </div>', '</DataState>\n      </div>')

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
