import { test, expect } from '@playwright/test';

test.describe('Dashboard E2E', () => {
  test.beforeEach(async ({ page }) => {
    // Mock user
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({ status: 200, json: { success: true, data: { accessToken: 'fake', role: 'ADMIN' } } });
    });
    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({ status: 200, json: { success: true, data: { id: 1, username: 'admin', role: 'ADMIN' } } });
    });

    // Mock branches
    await page.route('**/api/v1/branches*', async (route) => {
      await route.fulfill({ status: 200, json: { data: [{ id: 1, name: 'Chi nhánh TP1', branchCode: 'CN1' }] } });
    });

    // Mock analytics dashboard
    await page.route('**/api/v1/analytics/dashboard*', async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          data: {
            totalRevenue: 50000000,
            grossProfit: 15000000,
            inventoryTurnoverRatio: 1.25,
            totalOverdueDebt: 500000,
            topSellingProducts: [
              { productId: 101, productName: 'Sản phẩm 1', quantitySold: 120, revenue: 30000000 },
              { productId: 102, productName: 'Sản phẩm 2', quantitySold: 80, revenue: 20000000 }
            ]
          }
        }
      });
    });

    // Log in
    await page.goto('/');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('password');
    await page.keyboard.press('Enter');
    
    // Admin goes to dashboard by default
    await expect(page.getByTestId('dashboard-page')).toBeVisible();
  });

  test('E-DASH-01: ADMIN vào / thấy dashboard-page với các số liệu', async ({ page }) => {
    await expect(page.getByTestId('metric-revenue')).toContainText('50.000.000');
    await expect(page.getByTestId('metric-debt')).toContainText('500.000');
    await expect(page.getByTestId('metric-debt')).toContainText('Cần thu hồi');
  });

  test('E-DASH-02: top-product-row có 2 dòng', async ({ page }) => {
    const rows = page.getByTestId('top-product-row');
    await expect(rows).toHaveCount(2);
    await expect(rows.nth(0)).toContainText('Sản phẩm 1');
  });

  test('E-DASH-03: Request dashboard đầu tiên có startDateKey đúng', async ({ page }) => {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const startKey = `${y}${m}01`;
    
    // We can check if the select period is "month" by default which means startKey is first day of month
    const requestPromise = page.waitForRequest(req => 
      req.url().includes('analytics/dashboard') && req.url().includes(`startDateKey=${startKey}`)
    );
    
    // Trigger a refetch or navigate again if needed, but we already loaded it in beforeEach.
    // Let's just click "Tất cả thời gian" then back to "Tháng này"
    await page.getByTestId('dashboard-period').selectOption('all');
    await page.getByTestId('dashboard-period').selectOption('month');
    
    const request = await requestPromise;
    expect(request.url()).toContain(`startDateKey=${startKey}`);
  });

  test('E-DASH-04: Chọn kỳ Tùy chọn', async ({ page }) => {
    await page.getByTestId('dashboard-period').selectOption('custom');
    
    await expect(page.getByTestId('dashboard-date-start')).toBeVisible();
    await expect(page.getByTestId('dashboard-date-end')).toBeVisible();
    await expect(page.getByTestId('dashboard-filter-btn')).toBeVisible();

    await page.getByTestId('dashboard-date-start').fill('2026-01-01');
    await page.getByTestId('dashboard-date-end').fill('2026-01-31');

    const requestPromise = page.waitForRequest(req => 
      req.url().includes('analytics/dashboard') && 
      req.url().includes('startDateKey=20260101') && 
      req.url().includes('endDateKey=20260131')
    );

    await page.getByTestId('dashboard-filter-btn').click();
    const request = await requestPromise;
    expect(request.url()).toContain('startDateKey=20260101');
  });

  test('E-DASH-05: Nút Xuất Excel', async ({ page }) => {
    await page.route('**/api/v1/analytics/export/excel*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        body: 'dummy-excel-data'
      });
    });

    const downloadPromise = page.waitForEvent('download');
    await page.getByTestId('dashboard-export-btn').click();
    
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('bao-cao-kinh-doanh');
  });
});
