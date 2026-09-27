import re

with open('src/index.css', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('@apply badge bg-success-soft text-success;', '@apply inline-flex h-5 items-center rounded px-1.5 text-[11px] font-semibold bg-success-soft text-success;')
content = content.replace('@apply badge bg-warning-soft text-warning;', '@apply inline-flex h-5 items-center rounded px-1.5 text-[11px] font-semibold bg-warning-soft text-warning;')
content = content.replace('@apply badge bg-danger-soft text-danger;', '@apply inline-flex h-5 items-center rounded px-1.5 text-[11px] font-semibold bg-danger-soft text-danger;')
content = content.replace('@apply badge bg-slate-100 text-ink-muted;', '@apply inline-flex h-5 items-center rounded px-1.5 text-[11px] font-semibold bg-slate-100 text-ink-muted;')

with open('src/index.css', 'w', encoding='utf-8') as f:
    f.write(content)
