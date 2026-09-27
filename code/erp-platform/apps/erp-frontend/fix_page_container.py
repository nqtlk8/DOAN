import re

with open('src/shared/components/Page/PageContainer.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("interface PageContainerProps {", "interface PageContainerProps extends React.HTMLAttributes<HTMLDivElement> {")
content = content.replace("({ children, className = '' }) => {", "({ children, className = '', ...rest }) => {")
content = content.replace("<div className={h-full overflow-auto }>", "<div className={h-full overflow-auto } {...rest}>")

with open('src/shared/components/Page/PageContainer.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
