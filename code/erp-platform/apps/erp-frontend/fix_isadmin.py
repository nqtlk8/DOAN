import os
import re

def remove_isAdmin_block(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # The block looks like:
    # {isAdmin && (
    #   <button onClick={() => handleOpenModal()} className="btn-primary h-8 px-3 flex items-center">
    #     <Plus size={16} className="mr-1" />
    #     Thêm mới
    #   </button>
    # )}
    pattern = r'\{isAdmin && \([\s\S]*?<\/button>\s*\)\}'
    content = re.sub(pattern, '', content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

files_to_fix = [
    'src/components/crm/DebtList.tsx',
    'src/components/inventory/StockList.tsx'
]

for filepath in files_to_fix:
    remove_isAdmin_block(filepath)
