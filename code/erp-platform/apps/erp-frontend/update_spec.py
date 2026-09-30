import re

with open('tests/ui/dashboard.spec.ts', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace totalOverdueDebt
content = content.replace('totalOverdueDebt:', 'totalReceivableDebt:')

# Add slowMovingProducts to dashboard mock
dashboard_mock_regex = re.compile(r'topSellingProducts:\s*\[(.*?)\]', re.DOTALL)
def add_slow(m):
    return m.group(0) + ",\n            slowMovingProducts: [{ productId: 201, productCode: 'SP201', productName: 'Sản phẩm chậm', currentStock: 50 }]"

content = dashboard_mock_regex.sub(add_slow, content)

# Add route mock for stock-alerts
# find // Mock analytics dashboard
alerts_mock = """    // Mock stock alerts
    await page.route('**/api/v1/analytics/stock-alerts*', async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          data: {
            negativeCount: 1,
            lowStockCount: 0,
            generatedAt: '2026-09-28T00:00:00Z',
            alerts: [
              {
                productId: 301, productCode: 'SP301', productName: 'Sản phẩm âm', branchId: 1, branchName: 'CN1', currentQuantity: -5, minQuantityThreshold: null, alertType: 'NEGATIVE_STOCK'
              }
            ]
          }
        }
      });
    });

    // Mock analytics dashboard"""
content = content.replace('    // Mock analytics dashboard', alerts_mock)

# Add assert panel hiển thị to E-DASH-01
edash01_regex = re.compile(r"test\('E-DASH-01[^)]+', async \(\{ page \}\) => \{.*?\}\);", re.DOTALL)
def add_assert(m):
    old_test = m.group(0)
    new_test = old_test[:-3] + "  await expect(page.getByText('1 sản phẩm tồn âm')).toBeVisible();\n  });"
    return new_test
content = edash01_regex.sub(add_assert, content)

with open('tests/ui/dashboard.spec.ts', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated dashboard.spec.ts")
