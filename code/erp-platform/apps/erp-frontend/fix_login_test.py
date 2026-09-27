import re

with open('src/components/auth/__tests__/Login.test.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

mock_env = '''
vi.mock('../../../config/env', () => ({
  ENV: { isDev: true }
}));
'''

content = content.replace("vi.mock('../../../context/AuthContext'", mock_env + "\nvi.mock('../../../context/AuthContext'")

with open('src/components/auth/__tests__/Login.test.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
