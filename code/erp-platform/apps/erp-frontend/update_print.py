import re

with open('src/components/common/PrintInvoice.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = "import { formatNumber } from '../../shared/utils/format';\n" + content
content = re.sub(
    r'const formatMoney = \(amount: number\) => {\s*return new Intl\.NumberFormat\(\'vi-VN\'\)\.format\(amount\);\s*};',
    r'const formatMoney = (amount: number) => {\n    return formatNumber(amount);\n  };',
    content
)

with open('src/components/common/PrintInvoice.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
