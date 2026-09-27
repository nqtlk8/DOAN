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
                    found_files.append((path, set(matches)))

for path, matches in found_files:
    print(f"{path}: {', '.join(matches)}")
