import re

with open('src/components/common/__tests__/SearchableCombobox.test.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Mock scrollIntoView
setup_code = """
beforeEach(() => {
  vi.clearAllMocks();
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
});
"""
content = re.sub(r'beforeEach\(\(\) => \{\s*vi.clearAllMocks\(\);\s*\}\);', setup_code.strip(), content)

# Fix findByText
content = content.replace("await screen.findByText('Nguy?n Van A')", "await screen.findByRole('option')")

with open('src/components/common/__tests__/SearchableCombobox.test.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
