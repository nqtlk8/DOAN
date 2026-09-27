import re

with open('tests/sales-create.spec.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("await page.goto('/sales');", "await page.goto('/');")

with open('tests/sales-create.spec.ts', 'w', encoding='utf-8') as f:
    f.write(content)
