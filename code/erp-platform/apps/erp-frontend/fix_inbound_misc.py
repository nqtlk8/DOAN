import re

with open('src/components/inventory/InboundReceiptList.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace("hasData={receipts.length > 0}", "")
with open('src/components/inventory/InboundReceiptList.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

with open('src/components/layout/MdiModuleLayout.tsx', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace("isLoading?: boolean;", "isLoading?: boolean;\n  hideConfirm?: boolean;")
with open('src/components/layout/MdiModuleLayout.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

