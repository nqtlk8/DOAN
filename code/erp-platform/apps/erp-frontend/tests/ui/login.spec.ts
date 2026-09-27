import { test, expect } from '@playwright/test';

test.describe('Login E2E', () => {
  test('E-LOGIN-01: Mở / (chưa đăng nhập)', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('login-form')).toBeVisible();
    await expect(page.getByTestId('login-username')).toBeFocused();
  });

  test('E-LOGIN-02: Đăng nhập thành công Admin', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          success: true,
          data: { accessToken: 'fake-jwt', refreshToken: 'fake-jwt', role: 'ADMIN' },
          message: 'Success'
        }
      });
    });

    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          success: true,
          data: { id: 1, username: 'admin', role: 'ADMIN' }
        }
      });
    });
    
    await page.route('**/api/v1/catalog/products', async (route) => {
      await route.fulfill({ status: 200, json: { data: [] } });
    });

    await page.goto('/');
    
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('password');
    await page.keyboard.press('Enter');

    await expect(page.getByTestId('ribbon-user')).toContainText('admin');
  });

  test('E-LOGIN-03: Đăng nhập sai', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 401,
        json: {
          success: false,
          message: 'Sai thông tin đăng nhập'
        }
      });
    });

    await page.goto('/');
    
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('wrongpass');
    await page.getByTestId('login-submit').click();

    await expect(page.getByTestId('login-error')).toBeVisible();
    await expect(page.getByTestId('login-error')).toContainText('Sai thông tin đăng nhập');
    await expect(page.getByTestId('login-submit')).toBeEnabled();
  });
});
