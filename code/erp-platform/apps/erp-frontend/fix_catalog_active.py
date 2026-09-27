import re

with open('src/types/catalog.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("isActive: boolean;", "isActive?: boolean;")
content = content.replace("active?: boolean;", "active?: boolean;\n    isActive?: boolean;")

with open('src/types/catalog.ts', 'w', encoding='utf-8') as f:
    f.write(content)
