import re

with open('src/api/ApiService.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("components['schemas']['BranchResponseDto']", "any")

with open('src/api/ApiService.ts', 'w', encoding='utf-8') as f:
    f.write(content)

