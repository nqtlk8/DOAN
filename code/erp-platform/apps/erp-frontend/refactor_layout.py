# -*- coding: utf-8 -*-
import os
import re

def refactor_ui(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if '<PageHeader' in content:
        return

    title_match = re.search(r'<h2[^>]*>([^<]+)</h2>', content)
    title = title_match.group(1).strip() if title_match else "Danh sách"
    
    table_match = re.search(r'<table[^>]*>', content)
    if not table_match:
        return
        
    map_match = re.search(r'([a-zA-Z0-9_]+)\.map\(', content[table_match.end():])
    array_name = map_match.group(1) if map_match else "[]"

    header_block = f'''<PageContainer>
      <PageHeader 
        title="{title}"
        actions={{
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search size={{16}} className="absolute left-2.5 top-2 text-ink-subtle" />
              <input
                type="text"
                placeholder="Tìm theo mã, tên..."
                value={{searchTerm}}
                onChange={{(e) => setSearchTerm(e.target.value)}}
                className="erp-input h-8 pl-8 w-[280px]"
              />
            </div>
            {{isAdmin && (
              <button onClick={{() => handleOpenModal()}} className="btn-primary h-8 px-3 flex items-center">
                <Plus size={{16}} className="mr-1" />
                Thêm mới
              </button>
            )}}
          </div>
        }}
      />

      <div className="card overflow-hidden">
        <DataState
          isLoading={{loading}}
          isError={{isError}}
          error={{error}}
          isEmpty={{{array_name}.length === 0}} 
          onRetry={{refetch}}
          loadingType="table"
        >
          <table className="erp-table">'''

    pattern = r'<PageContainer>.*?<table[^>]*>'
    content = re.sub(pattern, header_block, content, flags=re.DOTALL)
    
    # Check if we already have DataState inside table
    if '<DataState' in content:
        # Well, there was already a DataState inside? Let's remove old DataState wrappers if any.
        # Actually it's probably better to just replace </table> with </table>\n        </DataState>
        # We'll just carefully do it.
        content = re.sub(r'</table>\s*</div>\s*</PageContainer>', r'</table>\n        </DataState>\n      </div>\n    </PageContainer>', content, flags=re.DOTALL)
    
    # We also need to fix Table Header columns: text-xs font-medium uppercase tracking-wider text-ink-subtle
    # Replace <th className="... text-ink-subtle[^"]*"> with standard table headers? 
    # The requirement says: 	able.erp-table (header sticky, cột số dùng .num, ngày dùng formatDate, tiền dùng formatNumber). 
    # But erp-table already styles 	h mostly, maybe I don't need to change 	h classes immediately.
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

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

for file in files_to_wrap:
    refactor_ui(file)

