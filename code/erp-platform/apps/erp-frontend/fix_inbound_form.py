import re

with open('src/components/inventory/InboundReceiptForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("error: !!fieldErrors.partner,", "")
content = content.replace("renderCombobox: () => (", "renderCombobox: (hasError) => (")
content = content.replace("<SearchableCombobox\n                data-testid=\"inbound-supplier-combo\"", "<SearchableCombobox\n                error={hasError}\n                data-testid=\"inbound-supplier-combo\"")

content = content.replace("bold: true, isPrimary: true", "strong: true, tone: 'primary'")
content = content.replace("renderCreateNew={() => null}", "")

with open('src/components/inventory/InboundReceiptForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
