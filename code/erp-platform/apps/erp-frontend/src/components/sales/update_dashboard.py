import re

with open('code/erp-platform/apps/erp-frontend/src/components/sales/Dashboard.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
content = content.replace("import { useQuery } from '@tanstack/react-query';", "import { useQuery } from '@tanstack/react-query';\nimport { useDashboardMetrics } from '../../hooks/useDashboardMetrics';\nimport { useStockAlerts } from '../../hooks/useStockAlerts';\nimport { StockAlertPanel } from './StockAlertPanel';")

# Replace useQuery for metrics with useDashboardMetrics
old_use_query = """  const {
    data: metrics,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useQuery<DashboardMetricsDto>({
    queryKey: ['dashboardMetrics', branchId, queryParams.start, queryParams.end],
    queryFn: () => ApiService.Analytics.getDashboardMetrics(branchId, queryParams.start, queryParams.end),
  });"""
new_use_query = """  const {
    data: metrics,
    isLoading,
    isError,
    error,
    refetch: refetchMetrics,
    isFetching,
    dataUpdatedAt: metricsUpdatedAt,
  } = useDashboardMetrics(branchId, queryParams.start, queryParams.end);

  const {
    data: alertsSummary,
    isLoading: isAlertsLoading,
    isError: isAlertsError,
    refetch: refetchAlerts,
    dataUpdatedAt: alertsUpdatedAt,
  } = useStockAlerts(branchId);

  const refetchAll = () => {
    refetchMetrics();
    refetchAlerts();
  };
  
  const lastUpdated = Math.max(metricsUpdatedAt || 0, alertsUpdatedAt || 0);
"""
content = content.replace(old_use_query, new_use_query)

# Fix refetch call in button
content = content.replace('onClick={() => refetch()}', 'onClick={refetchAll}')

# Fix Cập nhật lúc in refresh button area or PageHeader actions
old_refresh_btn = """      <button
        type="button"
        onClick={() => refetch()}
        aria-label="Làm mới"
        title="Làm mới dữ liệu"
        className="btn btn-secondary h-8 w-8 px-0"
      >
        <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
      </button>"""

# Using regex to find the button because encoding issue might change the labels (like Làm mới might be LAm m>i)
refresh_btn_regex = re.compile(r'      <button\s*type="button"\s*onClick=\{\(\) => refetch\(\)\}.*?<RefreshCw size=\{14\}.*?</button>', re.DOTALL)
new_refresh_btn = """      {lastUpdated > 0 && (
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
content = refresh_btn_regex.sub(new_refresh_btn, content)

content = content.replace('onClick={refetchAll}\n        aria-label="LAm m>i"', 'onClick={refetchAll}\n        aria-label="Làm mới"')

# Fix the total overdue debt to total receivable debt
content = content.replace('metrics.totalOverdueDebt', 'metrics.totalReceivableDebt')
content = content.replace('Công nợ quá hạn', 'Tổng công nợ phải thu')
content = content.replace('CA\'ng n quA hn', 'Tổng công nợ phải thu') # Fallback for corrupted characters if any
content = content.replace('Tổng nợ phải thu quá hạn', 'Tổng nợ khách hàng theo chi nhánh')
content = content.replace('T ng n phi thu quA hn', 'Tổng nợ khách hàng theo chi nhánh') 

# Update metrics onRetry={refetch} to onRetry={refetchAll}
content = content.replace('onRetry={refetch}', 'onRetry={refetchAll}')

# Insert StockAlertPanel under PageHeader
content = content.replace('<DataState', '<StockAlertPanel summary={alertsSummary} isLoading={isAlertsLoading} isError={isAlertsError} onRetry={refetchAlerts} />\n\n      <DataState')

# Find the end of the top products table and insert slow moving products
content_to_insert_slow_moving = """              </div>

              {/* Bảng Sản phẩm bán chậm */}
              <div className="lg:col-span-12 card overflow-hidden flex flex-col" data-testid="slow-products-table">
                <div className="px-4 h-12 flex items-center justify-between border-b border-line">
                  <h3 className="text-[15px] font-semibold text-ink">Sản phẩm bán chậm</h3>
                  {metrics.slowMovingProducts?.length > 0 && (
                    <span className="badge-neutral">{metrics.slowMovingProducts.length}</span>
                  )}
                </div>
                {(!metrics.slowMovingProducts || metrics.slowMovingProducts.length === 0) ? (
                  <div className="p-8 text-center text-ink-subtle flex flex-col items-center justify-center gap-2">
                    <PackageOpen size={32} className="text-line-dark" />
                    <p>Không có sản phẩm tồn kho mà không bán trong kỳ</p>
                  </div>
                ) : (
                  <div className="overflow-auto max-h-[300px]">
                    <table className="erp-table">
                      <thead className="sticky top-0 bg-white">
                        <tr>
                          <th className="w-10 text-center">#</th>
                          <th className="w-32">Mã SP</th>
                          <th>Tên SP</th>
                          <th className="num w-24">Tồn hiện tại</th>
                        </tr>
                      </thead>
                      <tbody>
                        {metrics.slowMovingProducts.map((p, i) => (
                          <tr key={p.productId ?? i} data-testid="slow-product-row">
                            <td className="text-center text-ink-subtle">{i + 1}</td>
                            <td>{p.productCode}</td>
                            <td className="truncate max-w-[220px]" title={p.productName}>
                              {p.productName}
                            </td>
                            <td className="num font-medium text-red-600">{formatNumber(p.currentStock)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>"""

content = content.replace('              </div>\n            </div>', content_to_insert_slow_moving)

# Finally write the updated file
with open('code/erp-platform/apps/erp-frontend/src/components/sales/Dashboard.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated Dashboard.tsx")
