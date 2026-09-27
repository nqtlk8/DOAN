import { test, expect } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi } from '../helpers/mockApi';
import { customers, products } from '../helpers/fixtures';

test.describe('SearchableCombobox tests', () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page, {
      '**/api/v1/customers?search=*': async (route) => {
        const url = new URL(route.request().url());
        const search = url.searchParams.get('search') || '';
        const filtered = customers.filter(c => c.name.toLowerCase().includes(search.toLowerCase()));
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: filtered }),
        });
      },
      '**/api/v1/catalog/products?*': async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: products }),
        });
      },
    });
  });

  test('E-CB-01: Click sales-customer-combo and type Nguy', async ({ page }) => {
    await loginAs(page, 'STAFF');
    await page.goto('/');
    
    // Đảm bảo Form Bán Hàng hiển thị bằng cách đợi data-testid="sales-customer-combo"
    const combo = page.locator('[data-testid="sales-customer-combo"]');
    await expect(combo).toBeVisible();
    
    await combo.click();
    await combo.fill('Nguy');
    
    const dropdown = page.locator('[data-testid="combobox-dropdown"]');
    await expect(dropdown).toBeVisible();
    
    // Chờ 1 chút để debounced fetch hoàn tất và render kết quả
    await expect(page.locator('text=Nguyễn Văn A')).toBeVisible();
    
    // Kiểm tra màu nền trong suốt
    const bgColor = await dropdown.evaluate((el) => window.getComputedStyle(el).backgroundColor);
    expect(bgColor).toBe('rgb(255, 255, 255)');
  });

  test('E-CB-02: Select Nguyễn Văn A', async ({ page }) => {
    await loginAs(page, 'STAFF');
    await page.goto('/');
    
    const combo = page.locator('[data-testid="sales-customer-combo"]');
    await combo.click();
    await combo.fill('Nguy');
    
    const option = page.locator('text=Nguyễn Văn A');
    await option.click();
    
    // Kiểm tra ô input có value 'Nguyễn Văn A'
    await expect(combo).toHaveValue('Nguyễn Văn A');
  });

  test('E-CB-03: Add multiple lines and test dropdown position', async ({ page }) => {
    await loginAs(page, 'STAFF');
    await page.goto('/');
    
    // Bấm Thêm Hàng 12 lần (data-testid="sales-add-line")
    const btnAdd = page.locator('[data-testid="sales-add-line"]');
    for (let i = 0; i < 12; i++) {
      await btnAdd.click();
    }
    
    // Click vào ô sản phẩm ở dòng cuối (index 12)
    const lastCombo = page.locator('[data-testid^="sales-product-combo-"]').last();
    await lastCombo.scrollIntoViewIfNeeded();
    await lastCombo.click();
    
    const dropdown = page.locator('[data-testid="combobox-dropdown"]');
    await expect(dropdown).toBeVisible();
    
    // boundingBox
    const box = await dropdown.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      const viewport = page.viewportSize();
      expect(viewport).not.toBeNull();
      if (viewport) {
        expect(box.y).toBeGreaterThanOrEqual(0);
        expect(box.y + box.height).toBeLessThanOrEqual(viewport.height);
      }
    }
  });
});
