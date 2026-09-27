import { test, expect } from '@playwright/test';

test.describe('Sprint 8 - Lists UI and Behaviors', () => {
  test.beforeEach(async ({ page }) => {
    // Mock user
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({ status: 200, json: { success: true, data: { accessToken: 'fake', role: 'ADMIN' } } });
    });
    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({ status: 200, json: { success: true, data: { id: 1, username: 'admin', role: 'ADMIN' } } });
    });

    // Mock products API
    await page.route('**/api/v1/catalog/products*', async route => {
      await route.fulfill({
        status: 200,
        json: {
          data: [
            { id: 1, productCode: 'SP01', code: 'SP01', name: 'Sản phẩm 1', unit: 'Cái', status: 'ACTIVE', categoryId: 1, categoryName: 'Danh mục 1' },
            { id: 2, productCode: 'SP02', code: 'SP02', name: 'Sản phẩm 2', unit: 'Chiếc', status: 'ACTIVE', categoryId: 1, categoryName: 'Danh mục 1' }
          ]
        }
      });
    });

    // Mock stock API
    await page.route('**/api/v1/inventory/stock*', async route => {
      await route.fulfill({
        status: 200,
        json: {
          data: [
            { productId: 1, productCode: 'SP01', productName: 'Sản phẩm 1', quantity: -3, branchId: 1, branchName: 'Chi nhánh TT' }
          ]
        }
      });
    });
    
    // Mock branches
    await page.route('**/api/v1/branches*', async (route) => {
      await route.fulfill({ status: 200, json: { data: [{ id: 1, name: 'Chi nhánh TT', branchCode: 'CN1' }] } });
    });
    
    // Mock dashboard metrics to prevent errors if it loads
    await page.route('**/api/v1/analytics/dashboard*', async (route) => {
      await route.fulfill({ status: 200, json: { data: { topSellingProducts: [] } } });
    });

    // Log in
    await page.goto('/');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('password');
    await page.getByTestId('login-submit').click();
    
    // Admin goes to dashboard by default
    await expect(page.getByTestId('dashboard-page')).toBeVisible({ timeout: 10000 });
  });

  test('E-LIST-01 & E-LIST-02: Product List Search', async ({ page }) => {
    // E-LIST-01
    await page.click('[data-testid="ribbon-tab-DanhMuc"]');
    await page.click('text=Sản Phẩm');
    
    await expect(page.getByText('Danh Mục Sản Phẩm')).toBeVisible();
    
    // Should see 2 rows
    await expect(page.getByText('Sản phẩm 1')).toBeVisible();
    await expect(page.getByText('Sản phẩm 2')).toBeVisible();

    // E-LIST-02
    const searchInput = page.getByPlaceholder('Tìm theo mã, tên...');
    await searchInput.fill('SP02');
    
    await expect(page.getByText('Sản phẩm 2')).toBeVisible();
    await expect(page.getByText('Sản phẩm 1')).not.toBeVisible();
  });

  test('E-LIST-03: Stock List negative quantity', async ({ page }) => {
    await page.click('[data-testid="ribbon-tab-TonKho"]');
    await page.locator('span:text-is("Tồn Kho")').click();
    
    await expect(page.getByText('Danh Mục Tồn Kho')).toBeVisible();
    
    // Find the cell with -3
    const cell = page.getByText('-3');
    await expect(cell).toBeVisible();
    await expect(cell).toHaveClass(/text-danger/);
  });
});
