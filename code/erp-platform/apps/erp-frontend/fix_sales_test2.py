import re

with open('tests/sales-create.spec.ts', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("localStorage.setItem('access_token', data.accessToken);\n    }, mockStaffResponse.data);", "localStorage.setItem('access_token', data.accessToken);\n    }, mockStaffResponse.data);\n    await page.reload();")

with open('tests/sales-create.spec.ts', 'w', encoding='utf-8') as f:
    f.write(content)
