import { test, expect } from '@playwright/test';

// Define the mock user and token that the frontend expects
const mockAdminResponse = {
  success: true,
  data: {
    role: "ADMIN",
    user: {
      id: "admin-123",
      username: "admin",
      role: "admin",
      fullName: "Admin User",
      email: "admin@erp.com"
    },
    accessToken: "mock-access-token",
    refreshToken: "mock-refresh-token"
  }
};

const mockCustomersResponse = {
  success: true,
  data: [
    { id: "1", code: "KH001", name: "Nguyễn Văn A", type: "RETAIL", isActive: true },
    { id: "2", code: "KH002", name: "Công ty ABC", type: "WHOLESALE", isActive: true }
  ]
};

test.describe('Customer flow', () => {

  test.beforeEach(async ({ page }) => {
    // Mock login session
    await page.goto('/');
    await page.evaluate((data) => {
      localStorage.setItem('user', JSON.stringify(data.user));
      localStorage.setItem('access_token', data.accessToken);
    }, mockAdminResponse.data);
    
    // Mock get customers
    await page.route('**/api/v1/customers', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCustomersResponse)
        });
      } else {
        await route.fallback();
      }
    });
  });

  test('TC-CUST-01: ADMIN mở được màn hình khách hàng', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Khách Hàng' }).click();
    const pageWrapper = page.getByTestId('customer-page');
    await expect(pageWrapper).toBeVisible();
    
    const rows = page.getByTestId('customer-row');
    await expect(rows).toHaveCount(2);

    const createBtn = page.getByTestId('customer-create-button');
    await expect(createBtn).toBeVisible();
  });

  test('TC-CUST-02: Tạo khách hàng hợp lệ', async ({ page }) => {
    let capturedPayload: any = null;

    await page.route('**/api/v1/customers', async route => {
      if (route.request().method() === 'POST') {
        capturedPayload = route.request().postDataJSON();
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { id: "new-uuid", code: "KH003", name: "Tân Khách", type: "RETAIL" }
          })
        });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Khách Hàng' }).click();
    await page.getByTestId('customer-create-button').click();
    
    await expect(page.getByTestId('customer-create-modal')).toBeVisible();

    await page.getByTestId('customer-code').fill('KH003');
    await page.getByTestId('customer-name').fill('Tân Khách');
    await page.getByTestId('customer-type').selectOption('RETAIL');
    
    await page.getByTestId('customer-save').click();

    // Verify modal is closed
    await expect(page.getByTestId('customer-create-modal')).not.toBeVisible();
    
    // Check if the payload was captured
    expect(capturedPayload).not.toBeNull();
  });

  test('TC-CUST-CONTRACT-01: Kiểm tra mismatch type -> customerType', async ({ page }) => {
    let capturedPayload: any = null;

    await page.route('**/api/v1/customers', async route => {
      if (route.request().method() === 'POST') {
        capturedPayload = route.request().postDataJSON();
        await route.fulfill({ status: 201, body: JSON.stringify({ success: true, data: { id: "new" } }) });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Khách Hàng' }).click();
    await page.getByTestId('customer-create-button').click();
    
    await page.getByTestId('customer-code').fill('KH003');
    await page.getByTestId('customer-name').fill('Tân Khách');
    await page.getByTestId('customer-type').selectOption('WHOLESALE');
    
    await page.getByTestId('customer-save').click();

    // Must have customerCode and customerType per contract
    expect(capturedPayload).toHaveProperty('customerCode', 'KH003');
    expect(capturedPayload).toHaveProperty('customerType', 'WHOLESALE');
    
    // Should NOT send code and type
    expect(capturedPayload).not.toHaveProperty('code');
    expect(capturedPayload).not.toHaveProperty('type');
  });

  test('TC-CUST-CONTRACT-02: Trường isActive không được gửi trong Create DTO', async ({ page }) => {
    let capturedPayload: any = null;

    await page.route('**/api/v1/customers', async route => {
      if (route.request().method() === 'POST') {
        capturedPayload = route.request().postDataJSON();
        await route.fulfill({ status: 201, body: JSON.stringify({ success: true, data: { id: "new" } }) });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Khách Hàng' }).click();
    await page.getByTestId('customer-create-button').click();
    await page.getByTestId('customer-name').fill('Tân Khách');
    await page.getByTestId('customer-save').click();

    expect(capturedPayload).not.toHaveProperty('isActive');
    expect(capturedPayload).not.toHaveProperty('id');
  });

  test('TC-CUST-03: Không nhập tên khách hàng', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Khách Hàng' }).click();
    await page.getByTestId('customer-create-button').click();
    
    // name is empty, save button should be disabled
    const saveBtn = page.getByTestId('customer-save');
    await expect(saveBtn).toBeDisabled();
  });

  test('TC-CUST-10: API Create Customer trả 500', async ({ page }) => {
    await page.route('**/api/v1/customers', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Server error' })
        });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Khách Hàng' }).click();
    await page.getByTestId('customer-create-button').click();
    await page.getByTestId('customer-name').fill('Tân Khách');
    await page.getByTestId('customer-save').click();

    // Modal should NOT close on error
    await expect(page.getByTestId('customer-create-modal')).toBeVisible();
    await expect(page.getByTestId('customer-save')).toBeEnabled();
  });

  test('TC-CUST-12: Double click nút Lưu', async ({ page }) => {
    let requestCount = 0;
    await page.route('**/api/v1/customers', async route => {
      if (route.request().method() === 'POST') {
        requestCount++;
        await new Promise(r => setTimeout(r, 500));
        await route.fulfill({ status: 201, body: JSON.stringify({ success: true, data: { id: "new" } }) });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Danh mục' }).click();
    await page.getByRole('button', { name: 'Khách Hàng' }).click();
    await page.getByTestId('customer-create-button').click();
    await page.getByTestId('customer-name').fill('Tân Khách');
    
    const saveBtn = page.getByTestId('customer-save');
    await saveBtn.click();
    // It should be disabled immediately
    await expect(saveBtn).toBeDisabled();
    
    // Try to double click forcefully (might throw if disabled but Playwright handles this)
    try { await saveBtn.click({ force: true }); } catch (e) {}

    // Wait a bit to ensure it doesn't fire twice
    await page.waitForTimeout(1000);
    expect(requestCount).toBe(1);
  });
});
