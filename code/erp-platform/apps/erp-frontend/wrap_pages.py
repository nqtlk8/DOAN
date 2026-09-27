import os
import glob

def wrap_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if "PageContainer" in content:
        return
        
    # Replace <div className="p-6"> with <PageContainer>
    import_str = "import { PageContainer } from '../../shared/components/Page/PageContainer';\n"
    if "shared/components/Page" not in content:
        content = import_str + content
    
    content = content.replace('<div className="p-6">', '<PageContainer>')
    content = content.replace('<div className="p-6 min-h-screen bg-slate-50">', '<PageContainer>')
    content = content.replace('<div className="p-4 h-full flex flex-col">', '<PageContainer>')
    content = content.replace('<div className="p-4 h-full">', '<PageContainer>')
    
    # We have to replace the closing </div> of the main container with </PageContainer>.
    # Since it's hard to parse matching tags with regex, we can just find the last </div>\n    );\n  } or similar.
    content = content.replace('</div>\n    );\n  }\n);', '</PageContainer>\n    );\n  }\n);')
    content = content.replace('</div>\n    );\n};', '</PageContainer>\n    );\n};')
    content = content.replace('</div>\n  );\n};', '</PageContainer>\n  );\n};')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

files_to_wrap = [
    'src/components/catalog/ProductList.tsx',
    'src/components/catalog/CustomerList.tsx',
    'src/components/catalog/SupplierList.tsx',
    'src/components/inventory/StockList.tsx',
    'src/components/crm/DebtList.tsx',
    'src/components/admin/BranchList.tsx',
    'src/components/sales/SalesList.tsx',
    'src/components/inventory/StockMovementList.tsx'
]

for file in files_to_wrap:
    wrap_file(file)

