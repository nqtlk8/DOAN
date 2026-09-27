import { test, expect } from '@playwright/test';

// Define the mock user and token that the frontend expects
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
    accessToken: "header.eyJzdWIiOiAiMTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc0MDAwIiwgInJvbGUiOiAiQURNSU4ifQ.signature",
    refreshToken: "mock-refresh-token"
  }
};

const mockStaffResponse = (origin: string) => ({
  success: true,
  data: {
    role: "STAFF",
    user: {
      id: "staff-123",
      username: "staff_tp1",
      role: "STAFF",
      fullName: "Staff User",
      email: "staff@erp.com"
    },
    accessToken: "header.eyJzdWIiOiAiMTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc0MDAwIiwgInJvbGUiOiAiQURNSU4ifQ.signature",
    refreshToken: "mock-refresh-token",
    branchUrl: origin
  }
});

test.describe('Login flow', () => {

  test('TC-LOGIN-01: Đăng nhập ADMIN thành công', async ({ page }) => {
    // Intercept API Login
    await page.route('**/api/v1/auth/login', async route => {
      const request = route.request();
      const payload = request.postDataJSON();
      expect(payload.username).toBe('admin');
      expect(payload.password).toBe('password');

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockAdminResponse)
      });
    });

    await page.goto('/');

    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('password');
    await page.getByTestId('login-submit').click();

    // Verify localStorage has been set (it might happen slightly after)
    await expect(async () => {
      const user = await page.evaluate(() => localStorage.getItem('user'));
      expect(user).toBeTruthy();
      const parsedUser = JSON.parse(user as string);
      expect(parsedUser.role).toBe('ADMIN');
    }).toPass();

    await expect(page.getByTestId('login-form')).not.toBeVisible();
  });

  test('TC-LOGIN-02: Đăng nhập STAFF thành công', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async route => {
      const url = new URL(page.url());
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockStaffResponse(url.origin))
      });
    });

    await page.goto('/');
    await page.getByTestId('login-username').fill('staff_tp1');
    await page.getByTestId('login-password').fill('password');
    await page.getByTestId('login-submit').click();

    await expect(async () => {
      const user = await page.evaluate(() => localStorage.getItem('user'));
      expect(user).toBeTruthy();
      const parsedUser = JSON.parse(user as string);
      expect(parsedUser.role).toBe('STAFF');
    }).toPass();

    await expect(page.getByTestId('login-form')).not.toBeVisible();
  });

  test('TC-LOGIN-03: Sai username/password', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async route => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Sai thông tin đăng nhập' })
      });
    });

    await page.goto('/');
    await page.getByTestId('login-username').fill('admin_wrong');
    await page.getByTestId('login-password').fill('wrong_pass');
    await page.getByTestId('login-submit').click();

    // Verify error message
    const errorMsg = page.getByTestId('login-error');
    await expect(errorMsg).toBeVisible();
    await expect(errorMsg).toContainText('Sai thông tin đăng nhập');

    // Verify we are still on login page
    await expect(page.getByTestId('login-submit')).toBeVisible();
  });

  test('TC-LOGIN-04: Bỏ trống username', async ({ page }) => {
    await page.goto('/');
    // username is required, HTML5 validation prevents submit
    await page.getByTestId('login-username').fill('');
    await page.getByTestId('login-password').fill('password');
    
    // We try to click submit, it shouldn't trigger form submission if it's native HTML validation
    await page.getByTestId('login-submit').click();

    const usernameInput = page.getByTestId('login-username');
    const validity = await usernameInput.evaluate((el: HTMLInputElement) => el.validity.valueMissing);
    expect(validity).toBe(true);
  });

  test('TC-LOGIN-05: Bỏ trống password', async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('');
    
    await page.getByTestId('login-submit').click();

    const passwordInput = page.getByTestId('login-password');
    const validity = await passwordInput.evaluate((el: HTMLInputElement) => el.validity.valueMissing);
    expect(validity).toBe(true);
  });

  test('TC-LOGIN-06: API login trả lỗi 500', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Internal Server Error' })
      });
    });

    await page.goto('/');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('password');
    await page.getByTestId('login-submit').click();

    const errorMsg = page.getByTestId('login-error');
    await expect(errorMsg).toBeVisible();
    
    // UI should not crash, button should be active again
    const submitBtn = page.getByTestId('login-submit');
    await expect(submitBtn).toBeEnabled();
  });

  test('TC-LOGIN-07: API login bị network error', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async route => {
      await route.abort('failed'); // Simulate network failure
    });

    await page.goto('/');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('password');
    await page.getByTestId('login-submit').click();

    const errorMsg = page.getByTestId('login-error');
    await expect(errorMsg).toBeVisible();
    // Usually it displays something like "Network Error" or similar, we just verify an error is shown
    await expect(page.getByTestId('login-submit')).toBeEnabled();
  });

  test('TC-LOGIN-08: Trạng thái loading khi login', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async route => {
      // Don't fulfill immediately to keep loading state
      await new Promise(resolve => setTimeout(resolve, 500));
      await route.fulfill({ status: 200, body: JSON.stringify(mockAdminResponse) });
    });

    await page.goto('/');
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('password');
    
    const submitBtn = page.getByTestId('login-submit');
    await submitBtn.click();
    
    // Assert disabled and loading text
    await expect(submitBtn).toBeDisabled();
    await expect(submitBtn).toHaveText('Đang xử lý...');
  });

  test('TC-LOGIN-09: Session được khôi phục sau reload', async ({ page }) => {
    // Set localStorage explicitly
    await page.goto('/');
    await page.evaluate((data) => {
      localStorage.setItem('user', JSON.stringify(data.user));
      localStorage.setItem('access_token', data.accessToken);
    }, mockAdminResponse.data);

    // Reload page
    await page.reload();

    // Since we are logged in, we shouldn't see login form
    await expect(page.getByTestId('login-form')).not.toBeVisible();
  });

});
