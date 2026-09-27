import re

with open('src/components/sales/SalesOrderForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("value: user?.branchName || 'CN Trung tâm'", "value: branch || 'CN Trung tâm'")
content = content.replace("user?.role === 'admin'", "user?.role === 'ADMIN'")
content = content.replace("renderCreateNew={(closeModal) => null /* TODO */}", "renderCreateNew={() => null /* TODO */}")

with open('src/components/sales/SalesOrderForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
