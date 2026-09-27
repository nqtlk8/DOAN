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

const mockProductsResponse = {
  success: true,
  data: [
    { id: 1, code: "SP001", name: "Sản phẩm 1", basePrice: 50000, isActive: true },
    { id: 2, code: "SP002", name: "Sản phẩm 2", basePrice: 100000, isActive: true }
  ]
};

test.describe('Products flow', () => {

  test.beforeEach(async ({ page }) => {
    // Mock APIs
    await page.route('**/api/v1/catalog/products*', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({ status: 200, body: JSON.stringify(mockProductsResponse) });
      } else {
        await route.fallback();
      }
    });

    // Fallback mock
    await page.route('**/api/v1/**', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify({ success: true, data: [] }) });
    });

    // Mock login session
    await page.goto('/');
    await page.evaluate((data) => {
      localStorage.setItem('user', JSON.stringify(data.user));
      localStorage.setItem('access_token', data.accessToken);
    }, mockAdminResponse.data);
  });

  test('TC-PROD-01: Xem danh sách sản phẩm', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Hàng Hóa' }).click();
    
    // Expect to see the products table
    await expect(page.getByText('Sản phẩm 1')).toBeVisible();
    await expect(page.getByText('Sản phẩm 2')).toBeVisible();
  });
});
