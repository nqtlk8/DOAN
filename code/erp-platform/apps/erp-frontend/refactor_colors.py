import os

def refactor_list_component(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    colors = {
        'bg-teal-600': 'bg-primary',
        'hover:bg-teal-700': 'hover:bg-primary-dark',
        'text-teal-600': 'text-primary',
        'text-teal-700': 'text-primary-dark',
        'border-teal-200': 'border-primary-soft',
        'border-teal-500': 'border-primary',
        'bg-teal-50': 'bg-primary-soft',
        'ring-teal-500': 'ring-primary',
        
        'bg-emerald-100': 'bg-green-50',
        'text-emerald-600': 'text-success',
        'text-emerald-700': 'text-success',
        'bg-emerald-50': 'bg-success-soft',
        'border-emerald-200': 'border-success-soft',
        'bg-emerald-500': 'bg-success',
        
        'bg-indigo-600': 'bg-primary',
        'hover:bg-indigo-700': 'hover:bg-primary-dark',
        'bg-indigo-100': 'bg-primary-soft',
        'text-indigo-600': 'text-primary',
        'text-indigo-700': 'text-primary-dark',
        'bg-indigo-50': 'bg-primary-soft',
        
        'text-slate-900': 'text-ink',
        'text-slate-800': 'text-ink',
        'text-slate-700': 'text-ink',
        'text-slate-600': 'text-ink-muted',
        'text-slate-500': 'text-ink-subtle',
        'text-slate-400': 'text-ink-lighter',
        
        'bg-slate-50': 'bg-app',
        'bg-slate-100': 'bg-slate-100',
        'border-slate-200': 'border-line',
        'border-slate-100': 'border-line',
        'border-slate-300': 'border-line-strong',
        
        'text-red-600': 'text-danger',
        'text-red-700': 'text-danger',
        'text-red-500': 'text-danger',
        'bg-red-50': 'bg-danger-soft',
        'bg-red-100': 'bg-danger-soft',
        'bg-red-600': 'bg-danger',
        'hover:bg-red-700': 'hover:bg-danger-dark',
        
        'bg-rose-50': 'bg-danger-soft',
        'text-rose-600': 'text-danger',
        'text-rose-700': 'text-danger',
        
        'bg-blue-50': 'bg-primary-soft',
        'bg-blue-100': 'bg-primary-soft',
        'bg-blue-600': 'bg-primary',
        'hover:bg-blue-700': 'hover:bg-primary-dark',
        'text-blue-600': 'text-primary',
        'text-blue-700': 'text-primary',
        
        'bg-orange-100': 'bg-warning-soft',
        'text-orange-600': 'text-warning',
        'bg-orange-50': 'bg-warning-soft',
        'text-orange-700': 'text-warning',
    }
    
    for k, v in colors.items():
        content = content.replace(k, v)

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
    refactor_list_component(file)

