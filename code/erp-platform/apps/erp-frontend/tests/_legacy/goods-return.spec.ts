import { test, expect } from '@playwright/test';

const mockStaffResponse = {
  success: true,
  data: {
    role: "STAFF",
    user: {
      id: "staff-123",
      username: "staff",
      role: "STAFF",
      fullName: "Staff User",
      email: "staff@erp.com"
    },
    accessToken: "mock-access-token",
    refreshToken: "mock-refresh-token"
  }
};

const mockCustomers = {
  success: true,
  data: [
    { id: 1, name: 'Khách hàng Test A' }
  ]
};

const mockProducts = {
  success: true,
  data: [
    { id: 100, name: 'Sản phẩm Test 1', basePrice: 50000, baseUnit: 'Cái' }
  ]
};

const mockSaveDraftResponse = {
  success: true,
  data: { id: 'RETURN-TEST-123' },
};

const mockConfirmResponse = {
  success: true,
  data: { id: 'RETURN-TEST-123', status: 'CONFIRMED' },
};

test.describe('Goods Return Module', () => {

  test.beforeEach(async ({ page }) => {
    // Mock APIs
    await page.route('**/api/v1/*customers*', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify(mockCustomers) });
    });

    await page.route('**/api/v1/*products*', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify(mockProducts) });
    });

    await page.route('**/api/v1/goods-return', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({ status: 200, body: JSON.stringify(mockSaveDraftResponse) });
      } else {
        await route.fallback();
      }
    });

    await page.route('**/api/v1/goods-return/*/confirm', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({ status: 200, body: JSON.stringify(mockConfirmResponse) });
      } else {
        await route.fallback();
      }
    });

    // Fallback mock for any other API to prevent hanging
    await page.route('**/api/v1/**', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify({ success: true, data: [] }) });
    });

    // Mock login session
    await page.goto('/');
    await page.evaluate((data) => {
      localStorage.setItem('user', JSON.stringify(data.user));
      localStorage.setItem('access_token', data.accessToken);
    }, mockStaffResponse.data);
  });

  test('TC-RETURN-01: Tạo phiếu trả hàng thành công', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByTestId('ribbon-tab-ChucNang')).toBeVisible();
    await page.getByTestId('ribbon-tab-ChucNang').click();
    await page.getByTestId('ribbon-btn-return').click();

    await expect(page.getByTestId('return-customer-search')).toBeVisible();

    // 1. Click "Tìm" (Khách hàng)
    await page.getByTestId('return-customer-search').click();
    await page.getByText('Khách hàng Test A').click();

    // 2. Click "Thêm dòng" (Sản phẩm)
    await page.getByTestId('return-add-product').click();
    await page.getByText('Sản phẩm Test 1').click();

    // Change quantity to 5
    const quantityInput = page.locator('table input[type="number"]').first();
    await quantityInput.fill('5');

    // 3. Save draft
    await page.getByTestId('return-save-draft').click();

    // Assert the success message for draft
    await expect(page.getByText('Lưu phiếu trả thành công')).toBeVisible();

    // 4. Confirm receipt
    await page.getByTestId('return-confirm').click();

    // Assert the success message for confirm
    await expect(page.getByText('Xác nhận trả hàng thành công')).toBeVisible();
  });
});
