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

    # We need to find the <DataState ... loadingType="table"> and close it right after </table>
    # In my previous script, I replaced:
    # content = re.sub(r'</table>\s*</div>\s*</PageContainer>', ...)
    # which didn't match.
    # What if I find <table className="erp-table"> and its closing </table> ?
    # It's easy: just replace the LAST </table> before </div> or something?
    # Let's count <DataState and </DataState>. If <DataState > </DataState>, we need one more.
    open_count = content.count('<DataState')
    close_count = content.count('</DataState>')
    
    if open_count > close_count:
        # We find the table that is right after <DataState ... loadingType="table">
        # and append \n</DataState> after its </table>.
        # Since these are simple files mostly, we can just replace the first </table> that comes after loadingType="table"
        idx = content.find('loadingType="table"')
        if idx != -1:
            table_close_idx = content.find('</table>', idx)
            if table_close_idx != -1:
                # insert </DataState> after </table>
                insert_pos = table_close_idx + len('</table>')
                content = content[:insert_pos] + '\n        </DataState>' + content[insert_pos:]
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
