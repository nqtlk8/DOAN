import { test, expect } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi, ok } from '../helpers/mockApi';
import { customers, products } from '../helpers/fixtures';

test.describe('Sales Order Form', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'STAFF');
    await mockApi(page, {
      '**/api/v1/customers*': (route) => route.fulfill(ok(customers)),
      '**/api/v1/catalog/products*': (route) => route.fulfill(ok(products)),
      '**/api/v1/receivable-debts/*/balance': (route) => route.fulfill(ok(0)),
      '**/api/v1/sales-invoices': (route) => {
        if (route.request().method() === 'POST') {
          route.fulfill(ok({ id: 'inv-1', invoiceCode: 'HD0001' }));
        } else {
          route.fallback();
        }
      }
    });
    await page.goto('/');
  });

  test('E-SALE-01: Mở / thấy (Tự động) và nút lưu', async ({ page }) => {
    await expect(page.getByTestId('doc-code')).toContainText('(Tự động)');
    await expect(page.getByTestId('btn-save')).toBeVisible();
  });

  test('E-SALE-02 to E-SALE-04: Thêm sản phẩm, tính tiền, submit form', async ({ page }) => {
    // E-SALE-02
    await page.getByTestId('sales-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nguyễn Văn A').click();
    
    await page.getByTestId('sales-add-line').click();
    
    // Line item combo - dynamic id based on UUID, so we find it by prefix
    const lineCombo = page.locator('[data-testid^="sales-product-combo-"]');
    await lineCombo.click();
    await page.keyboard.type('SP01');
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click();
    
    const quantityInput = page.getByTestId('sales-line-quantity');
    await expect(quantityInput).toBeFocused();

    // E-SALE-03
    await page.keyboard.type('5'); // Overwrites due to select() on focus
    await page.keyboard.press('Tab'); // Move away to trigger blur/change if needed
    
    await expect(page.getByTestId('sales-line-total')).toContainText('250.000');
    await expect(page.getByTestId('sales-grand-total')).toContainText('250.000');
    
    // E-SALE-04
    const requestPromise = page.waitForRequest(req => req.url().includes('/api/v1/sales-invoices') && req.method() === 'POST');
    await page.getByTestId('btn-save').click();
    const req = await requestPromise;
    const payload = req.postDataJSON();
    
    expect(payload.customerId).toBe(customers[0].id);
    expect(payload.lines[0].productId).toBe(101);
    expect(payload.lines[0].quantity).toBe(5);
    expect(payload.lines[0].unitPrice).toBe(50000);
    expect(payload.lines[0].unitOfMeasure).toBe('CAI');

    await expect(page.getByTestId('doc-code')).toContainText('HD0001');
  });

  test('E-SALE-05: Form trống bấm btn-save báo lỗi', async ({ page }) => {
    await page.getByTestId('btn-save').click();
    await expect(page.getByText('Đơn hàng phải có ít nhất 1 sản phẩm')).toBeVisible();
  });

  test('E-SALE-06: Chọn khách qua tìm kiếm nâng cao 🔍 rồi lưu được đơn', async ({ page }) => {
    await page.getByTestId('sales-partner-advanced').click();
    await page.getByTestId('search-modal-row').filter({ hasText: 'Trần Thị B' }).click();
    await expect(page.getByTestId('sales-customer-combo')).toHaveValue('Trần Thị B');

    await page.getByTestId('sales-add-line').click();
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click();

    const requestPromise = page.waitForRequest((req) => req.url().includes('/api/v1/sales-invoices') && req.method() === 'POST');
    await page.getByTestId('btn-save').click();
    const payload = (await requestPromise).postDataJSON();
    expect(payload.customerId).toBe(customers[1].id);
    await expect(page.getByText('Đã xác nhận', { exact: true })).toBeVisible();
  });

  test('E-SALE-07: Sửa đơn giá + Enter ở dòng cuối tạo dòng mới', async ({ page }) => {
    await page.getByTestId('sales-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nguyễn Văn A').click();
    await page.getByTestId('sales-add-line').click();
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click();

    await expect(page.getByTestId('sales-line-quantity')).toBeFocused();
    await page.keyboard.type('3');
    await page.keyboard.press('Enter');
    await expect(page.getByTestId('sales-line-price')).toBeFocused();
    await page.keyboard.type('45000');
    await expect(page.getByTestId('sales-line-total')).toContainText('135.000');

    await page.keyboard.press('Enter'); // dòng cuối → thêm dòng mới, focus ô sản phẩm
    await expect(page.getByTestId('sales-line-row')).toHaveCount(2);
    await expect(page.getByTestId('combobox-dropdown')).toBeVisible();
    await page.keyboard.press('Escape');

    await page.getByTestId('sales-line-delete').nth(1).click();
    const requestPromise = page.waitForRequest((req) => req.url().includes('/api/v1/sales-invoices') && req.method() === 'POST');
    await page.getByTestId('btn-save').click();
    const payload = (await requestPromise).postDataJSON();
    expect(payload.lines).toHaveLength(1);
    expect(payload.lines[0]).toMatchObject({ productId: 101, quantity: 3, unitPrice: 45000 });
  });

  test('E-SALE-08: Dòng thiếu sản phẩm bị tô đỏ, không gửi request', async ({ page }) => {
    await page.getByTestId('sales-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nguyễn Văn A').click();
    await page.getByTestId('sales-add-line').click();
    await page.keyboard.press('Escape');
    await page.getByTestId('btn-save').click();
    await expect(page.getByText('Vui lòng kiểm tra lại thông tin nhập')).toBeVisible();
    await expect(page.locator('[data-testid="sales-line-row"] td.ring-danger')).toHaveCount(1);
  });
});

