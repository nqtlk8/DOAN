import { test, expect } from '@playwright/test';

const mockAdminResponse = {
  success: true,
  data: {
    role: "ADMIN",
    user: {
      id: "admin-123",
      username: "admin",
      role: "ADMIN",
      fullName: "Admin User",
      email: "admin@erp.com"
    },
    accessToken: "mock-access-token",
    refreshToken: "mock-refresh-token"
  }
};

const mockDashboardMetrics = {
  success: true,
  data: {
    totalRevenue: 50000000,
    grossProfit: 15000000,
    inventoryTurnoverRatio: 1.25,
    totalOverdueDebt: 500000,
    topSellingProducts: [
      { productId: 1, productName: 'Product A', quantitySold: 100, revenue: 10000000 }
    ],
    slowMovingProducts: [
      { productId: 2, productName: 'Product B', quantitySold: 5, revenue: 500000 }
    ]
  }
};

test.describe('Dashboard flow', () => {

  test.beforeEach(async ({ page }) => {
    // Mock get dashboard metrics
    await page.route('**/api/v1/analytics/dashboard*', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockDashboardMetrics)
        });
      } else {
        await route.fallback();
      }
    });

    // Fallback mock for any other API to prevent hanging
    await page.route('**/api/v1/**', async route => {
      if (!route.request().url().includes('/analytics/dashboard')) {
        await route.fulfill({ status: 200, body: JSON.stringify({ success: true, data: [] }) });
      }
    });

    // Mock login session
    await page.goto('/');
    await page.evaluate((data) => {
      localStorage.setItem('user', JSON.stringify(data.user));
      localStorage.setItem('access_token', data.accessToken);
    }, mockAdminResponse.data);
  });

  test('TC-DASHBOARD-01: Xem Dashboard - hin th d liu metrics', async ({ page }) => {
    await page.goto('/');
    
    // Đảm bảo truy cập được Dashboard
    await page.getByRole('button', { name: 'Dashboard' }).click();
    
    const pageWrapper = page.getByTestId('dashboard-page');
    await expect(pageWrapper).toBeVisible();
    
    // Kiểm tra các metrics được hiển thị
    await expect(page.getByTestId('metric-revenue')).toContainText('50,000,000');
    await expect(page.getByTestId('metric-profit')).toContainText('15,000,000');
    await expect(page.getByTestId('metric-turnover')).toContainText('1.25');
    await expect(page.getByTestId('metric-debt')).toContainText('500,000');
  });

  test('TC-DASHBOARD-02: Export Excel', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Dashboard' }).click();
    
    // Mock API Excel export
    await page.route('**/api/v1/analytics/export/excel*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        body: 'dummy-excel-data'
      });
    });

    const exportBtn = page.getByTestId('dashboard-export-btn');
    await expect(exportBtn).toBeVisible();
    
    // Capture download
    const downloadPromise = page.waitForEvent('download');
    await exportBtn.click();
    const download = await downloadPromise;
    
    // Verify file name
    expect(download.suggestedFilename()).toBe('dashboard_report.xlsx');
  });

  test('TC-DASHBOARD-03: Lọc dữ liệu theo thời gian', async ({ page }) => {
    let capturedUrl: any = null;
    
    await page.route('**/api/v1/analytics/dashboard*', async route => {
      if (route.request().method() === 'GET') {
        capturedUrl = new URL(route.request().url());
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockDashboardMetrics)
        });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Dashboard' }).click();

    // Fill date range
    await page.getByTestId('dashboard-date-start').fill('2024-01-01');
    await page.getByTestId('dashboard-date-end').fill('2024-01-31');
    await page.getByTestId('dashboard-filter-btn').click();

    // Check if URL contains query params
    await page.waitForTimeout(500); // Give it time to route
    expect(capturedUrl).not.toBeNull();
    expect(capturedUrl?.searchParams.get('startDateKey')).toBe('20240101');
    expect(capturedUrl?.searchParams.get('endDateKey')).toBe('20240131');
  });
});
