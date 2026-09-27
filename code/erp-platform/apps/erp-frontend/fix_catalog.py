import re

with open('src/types/catalog.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("id: number;", "id: number | string;")
content = content.replace("id?: number;", "id?: number | string;")
content = content.replace("id: string;", "id: number | string;")

with open('src/types/catalog.ts', 'w', encoding='utf-8') as f:
    f.write(content)

