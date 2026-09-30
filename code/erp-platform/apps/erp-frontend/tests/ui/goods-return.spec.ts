import { test, expect, type Route } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi, ok } from '../helpers/mockApi';
import { customers, products } from '../helpers/fixtures';

let returnStatus: 'DRAFT' | 'CONFIRMED' = 'DRAFT';
const returnDto = () => ({
  id: 'ret-1',
  returnCode: 'TH0001',
  status: returnStatus,
  customerId: customers[1].id,
  customerName: customers[1].name,
  reason: 'Hàng lỗi',
  totalAmount: 200000,
  createdAt: '2026-09-27T10:00:00',
  lines: [{ productId: 102, productCode: 'SP02', productName: 'Sản phẩm 2', quantity: 2, unitOfMeasure: 'HOP', unitPrice: 100000 }],
});

test.describe('Goods Return Form', () => {
  test.beforeEach(async ({ page }) => {
    returnStatus = 'DRAFT';
    await loginAs(page, 'STAFF');
    await mockApi(page, {
      '**/api/v1/customers*': (route: Route) => route.fulfill(ok(customers)),
      '**/api/v1/catalog/products*': (route: Route) => route.fulfill(ok(products)),
      '**/api/v1/goods-returns': (route: Route) =>
        route.request().method() === 'POST' ? route.fulfill(ok('ret-1')) : route.fulfill(ok([returnDto()])), // POST trả UUID dạng chuỗi
      '**/api/v1/goods-returns/ret-1': (route: Route) => route.fulfill(ok(returnDto())),
      '**/api/v1/goods-returns/ret-1/confirm': (route: Route) => {
        returnStatus = 'CONFIRMED';
        return route.fulfill(ok(null));
      },
    });
    await page.goto('/');
    await page.getByTestId('ribbon-tab-ChucNang').click();
    await page.getByTestId('ribbon-btn-return').click();
  });

  test('E-RET-01: Mở phiếu nhập lại hàng bán', async ({ page }) => {
    await expect(page.getByText('Phiếu nhập lại hàng bán')).toBeVisible();
    await expect(page.getByTestId('return-customer-combo')).toBeVisible();
  });

  test('E-RET-02 → 04: Chọn khách, thêm hàng, lưu và xác nhận trả hàng', async ({ page }) => {
    await page.getByTestId('return-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Trần Thị B').click();
    await page.getByTestId('return-add-line').click();
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 2').click();
    await expect(page.getByTestId('return-line-quantity')).toBeFocused();
    await page.keyboard.type('2');
    await page.getByTestId('return-reason').fill('Hàng lỗi');

    await expect(page.getByTestId('return-grand-total')).toContainText('200.000');

    const requestPromise = page.waitForRequest((req) => req.url().endsWith('/api/v1/goods-returns') && req.method() === 'POST');
    await page.getByTestId('btn-save').click();
    const payload = (await requestPromise).postDataJSON();
    expect(payload.customerId).toBe(customers[1].id);
    expect(payload.reason).toBe('Hàng lỗi');
    expect(payload.lines).toEqual([{ productId: 102, quantity: 2, unitPrice: 100000, unitOfMeasure: 'HOP' }]);

    await expect(page.getByText('Lưu phiếu trả thành công')).toBeVisible();
    await expect(page.getByTestId('doc-code')).toContainText('TH0001');

    await page.getByTestId('btn-confirm').click();
    await expect(page.getByText('Xác nhận trả hàng thành công')).toBeVisible();
    await expect(page.getByText('Đã xác nhận', { exact: true })).toBeVisible();
    await expect(page.getByTestId('btn-confirm')).toBeHidden();
  });

  test('E-RET-05: Danh sách phiếu trả có cột Khách hàng và mở lại được phiếu', async ({ page }) => {
    await page.getByTestId('subview-list').click();
    await expect(page.getByRole('columnheader', { name: 'Khách hàng' })).toBeVisible();
    const row = page.getByTestId('return-list-row').first();
    await expect(row).toContainText('TH0001');
    await expect(row).toContainText('Trần Thị B');
    await row.dblclick();
    await expect(page.getByTestId('return-partner-name')).toHaveText('Trần Thị B');
    await expect(page.getByTestId('return-line-row').first()).toContainText('Sản phẩm 2');
  });
});
