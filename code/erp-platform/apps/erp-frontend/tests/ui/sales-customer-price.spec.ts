import { test, expect, type Route } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi, ok } from '../helpers/mockApi';
import { customers, products } from '../helpers/fixtures';

/**
 * Giá theo khách (CustomerProductPriceService): giá riêng KH > giá niêm yết; giá sửa trên phiếu
 * được backend lưu làm giá riêng của khách khi lưu đơn.
 * Khách A có giá riêng 45.000 cho SP01 (niêm yết 50.000); khách B chưa có giá riêng.
 */
const resolveFor = (customerId: string, productIds: number[]) =>
  productIds.map((productId) => {
    const listPrice = products.find((p) => p.id === productId)?.price ?? null;
    if (customerId === customers[0].id && productId === 101) {
      return { productId, unitPrice: 45000, source: 'CUSTOMER', listPrice, customerPrice: 45000 };
    }
    return { productId, unitPrice: listPrice, source: listPrice == null ? 'NONE' : 'LIST', listPrice, customerPrice: null };
  });

test.describe('Sales Order Form — giá theo khách', () => {
  let resolveCalls: string[] = [];

  test.beforeEach(async ({ page }) => {
    resolveCalls = [];
    await loginAs(page, 'STAFF');
    await mockApi(page, {
      '**/api/v1/customers*': (route) => route.fulfill(ok(customers)),
      '**/api/v1/catalog/products*': (route) => route.fulfill(ok(products)),
      '**/api/v1/receivable-debts/*/balance': (route) => route.fulfill(ok(0)),
      '**/api/v1/customer-prices/*/resolve*': (route: Route) => {
        const url = new URL(route.request().url());
        resolveCalls.push(url.pathname + url.search);
        const customerId = url.pathname.split('/')[4];
        const ids = (url.searchParams.get('productIds') ?? '').split(',').filter(Boolean).map(Number);
        return route.fulfill(ok(resolveFor(customerId, ids)));
      },
      '**/api/v1/sales-invoices': (route) => {
        if (route.request().method() === 'POST') route.fulfill(ok({ id: 'inv-1', invoiceCode: 'HD0001' }));
        else route.fallback();
      },
    });
    await page.goto('/');
  });

  test('E-SALE-09: Khách có giá riêng -> tự áp "Giá KH" và gửi đúng giá khi lưu', async ({ page }) => {
    await page.getByTestId('sales-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nguyễn Văn A').click();
    await page.getByTestId('sales-add-line').click();
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click();

    await expect(page.getByTestId('sales-line-price-source')).toHaveText('Giá KH');
    await expect(page.getByTestId('sales-line-total')).toContainText('45.000');
    await expect(page.getByTestId('sales-line-row')).not.toHaveAttribute('data-price-edited');
    await expect(page.getByTestId('sales-price-note')).toHaveCount(0);
    expect(resolveCalls.some((c) => c.includes(customers[0].id) && c.includes('productIds=101'))).toBe(true);

    const requestPromise = page.waitForRequest((req) => req.url().includes('/api/v1/sales-invoices') && req.method() === 'POST');
    await page.getByTestId('btn-save').click();
    const payload = (await requestPromise).postDataJSON();
    expect(payload.lines[0]).toMatchObject({ productId: 101, unitPrice: 45000 });
  });

  test('E-SALE-10: Sửa giá -> dòng được tô nổi + ghi chú; đổi khách giữ giá đã sửa', async ({ page }) => {
    await page.getByTestId('sales-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Trần Thị B').click();
    await page.getByTestId('sales-add-line').click();
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click();
    await expect(page.getByTestId('sales-line-price-source')).toHaveText('Niêm yết');

    await expect(page.getByTestId('sales-line-quantity')).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.getByTestId('sales-line-price')).toBeFocused();
    await page.keyboard.type('47000');
    await page.keyboard.press('Tab');

    await expect(page.getByTestId('sales-line-row')).toHaveAttribute('data-price-edited', 'true');
    await expect(page.getByTestId('sales-price-note')).toContainText('Giá này sẽ được lưu làm giá riêng cho khách khi lưu đơn');

    // Đổi sang khách A (có giá riêng 45.000): dòng đã sửa tay giữ 47.000, nguồn giá chuyển "Giá KH"
    await page.getByTestId('sales-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nguyễn Văn A').click();
    await expect(page.getByTestId('sales-line-price-source')).toHaveText('Giá KH');
    await expect(page.getByTestId('sales-line-total')).toContainText('47.000');
    await expect(page.getByTestId('sales-line-row')).toHaveAttribute('data-price-edited', 'true');

    const requestPromise = page.waitForRequest((req) => req.url().includes('/api/v1/sales-invoices') && req.method() === 'POST');
    await page.getByTestId('btn-save').click();
    const payload = (await requestPromise).postDataJSON();
    expect(payload.customerId).toBe(customers[0].id);
    expect(payload.lines[0]).toMatchObject({ productId: 101, unitPrice: 47000 });
  });
});
