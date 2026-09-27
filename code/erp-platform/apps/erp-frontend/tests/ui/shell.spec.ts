import { test, expect } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi } from '../helpers/mockApi';

test.describe('Shell tests', () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page, {});
  });

  test('E-SHELL-01: STAFF thấy ribbon user và tab bán hàng', async ({ page }) => {
    await loginAs(page, 'STAFF');
    await page.goto('/');
    const userBlock = page.locator('[data-testid="ribbon-user"]');
    await expect(userBlock).toContainText('staff_tp1');
    await expect(userBlock).toContainText('Nhân viên');
    const tabOrder = page.locator('[data-testid="tab-new-order"]');
    await expect(tabOrder).toBeVisible();
  });

  test('E-SHELL-02: STAFF bấm ChucNang tab', async ({ page }) => {
    await loginAs(page, 'STAFF');
    await page.goto('/');
    await page.click('[data-testid="ribbon-tab-ChucNang"]');
    await expect(page.locator('text=Xuất Trả Hàng Mua')).not.toBeVisible();
    await expect(page.locator('[data-testid="ribbon-btn-inbound"]')).toBeVisible();
    await expect(page.locator('[data-testid="ribbon-btn-return"]')).toBeVisible();
  });

  test('E-SHELL-03: Bấm Đăng xuất', async ({ page }) => {
    await loginAs(page, 'STAFF');
    await page.goto('/');
    await page.click('[data-testid="ribbon-logout"]');
    await expect(page.locator('[data-testid="login-form"]')).toBeVisible();
  });

  test('E-SHELL-04: CSS Tailwind được nạp', async ({ page }) => {
    await loginAs(page, 'STAFF');
    await page.goto('/');
    const bgColor = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
    expect(bgColor).toBe('rgb(245, 247, 250)');
  });
});
