import os
import re

directories = ['src']
pattern = re.compile(r'(teal-|indigo-|emerald-|purple-|pink-|rose-|blue-|green-|orange-|red-|bg-white)')

found_files = []

for root, _, files in os.walk('src'):
    if '_to_delete' in root:
        continue
    for file in files:
        if file.endswith('.tsx') or file.endswith('.ts'):
            if file == 'PrintInvoice.tsx':
                continue
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                matches = pattern.findall(content)
                if matches:
                    found_files.append(path)

colors = {
    'bg-white': 'bg-surface',
    
    'text-red-500': 'text-danger',
    'text-red-600': 'text-danger',
    'text-red-700': 'text-danger',
    'bg-red-50': 'bg-danger-soft',
    'bg-red-100': 'bg-danger-soft',
    'border-red-200': 'border-line', # no border-danger-soft usually, just line
    'hover:bg-red-700': 'hover:bg-danger-dark',
    'hover:bg-red-600': 'hover:bg-danger',
    'text-red-800': 'text-danger',
    
    'text-blue-500': 'text-primary',
    'text-blue-600': 'text-primary',
    'text-blue-700': 'text-primary-dark',
    'bg-blue-50': 'bg-primary-soft',
    'bg-blue-100': 'bg-primary-soft',
    'border-blue-200': 'border-primary-soft',
    'hover:bg-blue-700': 'hover:bg-primary-dark',
    'hover:bg-blue-600': 'hover:bg-primary',
    
    'text-green-500': 'text-success',
    'text-green-600': 'text-success',
    'text-green-700': 'text-success',
    'bg-green-50': 'bg-success-soft',
    'bg-green-100': 'bg-success-soft',
    'border-green-200': 'border-success-soft',
    
    'bg-rose-50': 'bg-danger-soft',
    'text-rose-600': 'text-danger',
    'text-rose-700': 'text-danger',
    
    'bg-teal-50': 'bg-primary-soft',
    'bg-teal-100': 'bg-primary-soft',
    'bg-teal-600': 'bg-primary',
    'text-teal-600': 'text-primary',
}

for filepath in found_files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # We replace colors in exact string matching
    # Because replace can sometimes match substrings, we sort by length desc
    sorted_colors = sorted(colors.keys(), key=len, reverse=True)
    for k in sorted_colors:
        content = content.replace(k, colors[k])
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

