import re

with open('code/erp-platform/apps/erp-frontend/src/components/sales/Dashboard.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

actions_regex = re.compile(r'      <button[^>]+onClick=\{refetchAll\}[^>]+>.*?<\/button>', re.DOTALL)

actions_new = """      {lastUpdated > 0 && (
        <span className="text-xs text-gray-500 mr-2 flex items-center">
          Cập nhật lúc {new Date(lastUpdated).toLocaleTimeString('vi-VN')}
        </span>
      )}
      <button
        type="button"
        onClick={refetchAll}
        aria-label="Làm mới"
        title="Làm mới dữ liệu"
        className="btn btn-secondary h-8 w-8 px-0"
      >
        <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
      </button>"""

if "Cập nhật lúc" not in content:
    content = actions_regex.sub(actions_new, content)
    print("Regex matched and updated")
else:
    print("Already updated")
    
with open('code/erp-platform/apps/erp-frontend/src/components/sales/Dashboard.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
