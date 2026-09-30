import re

with open('code/erp-platform/apps/erp-frontend/src/components/sales/Dashboard.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the actions block
actions_old = """      <button
        type="button"
        onClick={() => refetch()}
        aria-label="Làm mới"
        title="Làm mới dữ liệu"
        className="btn btn-secondary h-8 w-8 px-0"
      >
        <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
      </button>"""
# if that failed, let's try a regex
# <button type="button" onClick={() => refetch()} aria-label="Làm mới" title="Làm mới dữ liệu" className="btn btn-secondary h-8 w-8 px-0"><RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} /></button>
# Let's just find the area between custom filter and export button
# `</button>\n        </>\n      )}\n\n      <button`
actions_regex = re.compile(r'      <button[^>]+onClick=\{\(\) => refetch\(\)\}[^>]+>.*?<\/button>', re.DOTALL)

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
content = actions_regex.sub(actions_new, content)

# Check for "Cập nhật lúc"
if "Cập nhật lúc" not in content:
    print("Regex failed to find the refresh button!")
    
# also replace "Công nợ quá hạn"
content = content.replace('Công nợ quá hạn', 'Tổng công nợ phải thu')
content = content.replace('Tổng nợ phải thu quá hạn', 'Tổng nợ khách hàng theo chi nhánh')
content = content.replace('metrics.totalOverdueDebt', 'metrics.totalReceivableDebt')

with open('code/erp-platform/apps/erp-frontend/src/components/sales/Dashboard.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
print("Update applied")
