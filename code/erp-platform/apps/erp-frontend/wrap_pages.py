import os
import glob
import re

files = glob.glob('src/components/**/*List.tsx', recursive=True) + ['src/components/sales/Dashboard.tsx']
for f in files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    
    if 'PageContainer' in content:
        continue
        
    # Add import
    import_stmt = "import { PageContainer } from '../../shared/components/Page/PageContainer';\n"
    if 'src/components/' in f and f.count('/') == 3:
         import_stmt = "import { PageContainer } from '../../shared/components/Page/PageContainer';\n"
         
    content = import_stmt + content
    
    # Replace the return statement's outer div.
    # Usually it's like <div className="p-6"> or <div className="min-h-screen p-6">
    content = re.sub(r'return\s*\(\s*<div\b([^>]*)className="([^"]*?)(p-6|min-h-screen)([^"]*)"', 
                     lambda m: f'return (\n    <PageContainer>\n      <div className="{m.group(2).replace("p-6", "").replace("min-h-screen", "").strip()}"', 
                     content, count=1)
    
    if '<PageContainer>' in content:
        # Also need to close it at the end.
        content = re.sub(r'</div>\s*\);\s*};', r'</div>\n    </PageContainer>\n  );\n};', content)
        
    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)
