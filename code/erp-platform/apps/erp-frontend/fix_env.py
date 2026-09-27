import re

with open('src/config/env.ts', 'r', encoding='utf-8') as f:
    content = f.read()

if "export const ENV = {" not in content:
    content = "export const ENV = {\n  isDev: import.meta.env.DEV,\n};\n\n" + content

with open('src/config/env.ts', 'w', encoding='utf-8') as f:
    f.write(content)
