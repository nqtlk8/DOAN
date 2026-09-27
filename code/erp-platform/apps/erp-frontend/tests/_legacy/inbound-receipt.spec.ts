import { test, expect } from '@playwright/test';

const mockSuppliers = {
  success: true,
  data: [
    { id: 1, name: 'Nhà cung cấp Test A' }
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
  data: { id: 'RECEIPT-TEST-123' },
};

const mockConfirmResponse = {
  success: true,
  data: { id: 'RECEIPT-TEST-123', status: 'CONFIRMED' },
};

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

test.describe('Inbound Receipt Module', () => {
  test.beforeEach(async ({ page }) => {
    // Mock APIs
    await page.route('**/api/v1/*suppliers*', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify(mockSuppliers) });
    });

    await page.route('**/api/v1/*products*', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify(mockProducts) });
    });

    await page.route('**/api/v1/**inbound**', async route => {
      if (route.request().method() === 'POST' && !route.request().url().includes('confirm')) {
        await route.fulfill({ status: 200, body: JSON.stringify(mockSaveDraftResponse) });
      } else {
        await route.fulfill({ status: 200, body: JSON.stringify(mockConfirmResponse) });
      }
    });

    await page.route('**/api/v1/customers*', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify({ success: true, data: [] }) });
    });

    await page.route('**/api/v1/catalog/products*', async route => {
      await route.fulfill({ status: 200, body: JSON.stringify(mockProducts) });
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

  test('TC-INBOUND-01: Tạo phiếu nhập hàng thành công', async ({ page }) => {
    // Navigate to root to trigger logged in state
    await page.goto('/');

    // Ensure the top ribbon is visible
    await expect(page.getByTestId('ribbon-tab-ChucNang')).toBeVisible();

    // Click the "Chức Năng" tab if needed
    await page.getByTestId('ribbon-tab-ChucNang').click();

    // Click the "Nhập Hàng" button
    await page.getByTestId('ribbon-btn-inbound').click();

    // Now the Inbound Receipt Module should be mounted
    // Wait for the supplier search button to appear
    await expect(page.getByTestId('inbound-supplier-search')).toBeVisible();

    // 1. Click "Tìm" (Nhà cung cấp)
    await page.getByTestId('inbound-supplier-search').click();
    
    // Select the first supplier in the modal
    await page.getByText('Nhà cung cấp Test A').click();

    // 2. Click "Thêm dòng" (Sản phẩm)
    await page.getByTestId('inbound-add-product').click();

    // Select the first product in the modal
    await page.getByText('Sản phẩm Test 1').click();

    // Change quantity to 5
    const quantityInput = page.locator('table input[type="number"]').first();
    await quantityInput.fill('5');

    // 3. Save draft
    await page.getByTestId('inbound-save-draft').click();

    // Assert the success message for draft
    await expect(page.getByText('Lưu phiếu nhập thành công')).toBeVisible();

    // 4. Confirm receipt
    await page.getByTestId('inbound-confirm').click();

    // Assert the success message for confirm
    await expect(page.getByText('Xác nhận phiếu nhập thành công')).toBeVisible();
  });
});
