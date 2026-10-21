import { test, expect } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi, ok } from '../helpers/mockApi';
import { customers, products } from '../helpers/fixtures';

/**
 * Không cho bán vượt tồn: backend trả 409 INSUFFICIENT_STOCK khi XÁC NHẬN hóa đơn
 * (InventoryFacadeImpl.recordSaleAndGetCost). Lưu nháp không trừ kho nên luôn thành công;
 * xác nhận bị từ chối thì phiếu vẫn là nháp.
 */
const MESSAGE = 'Không đủ hàng: [SP01] Sản phẩm 1 — tồn kho còn 3, cần 5. Vui lòng nhập hàng trước khi bán.';

test('E-SALE-11: Xác nhận vượt tồn -> hiện "Không đủ hàng", phiếu vẫn là nháp', async ({ page }) => {
  await loginAs(page, 'STAFF');
  await mockApi(page, {
    '**/api/v1/customers*': (route) => route.fulfill(ok(customers)),
    '**/api/v1/catalog/products*': (route) => route.fulfill(ok(products)),
    '**/api/v1/receivable-debts/*/balance': (route) => route.fulfill(ok(0)),
    '**/api/v1/sales-invoices': (route) => {
      if (route.request().method() === 'POST') return route.fulfill(ok({ id: 'inv-1', invoiceCode: 'HD0001' }));
      return route.fallback();
    },
    '**/api/v1/sales-invoices/*/confirm': (route) =>
      route.fulfill({
        status: 409,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: MESSAGE, errors: ['INSUFFICIENT_STOCK'] }),
      }),
  });
  await page.goto('/');

  await page.getByTestId('sales-customer-combo').click();
  await page.getByTestId('combobox-dropdown').getByText('Nguyễn Văn A').click();
  await page.getByTestId('sales-add-line').click();
  await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click();
  await expect(page.getByTestId('sales-line-quantity')).toBeFocused();
  await page.keyboard.type('5');
  await page.keyboard.press('Tab');

  await page.getByTestId('btn-save').click();
  await expect(page.getByTestId('doc-code')).toContainText('HD0001');
  await expect(page.getByText('Nháp', { exact: true })).toBeVisible();

  await page.getByTestId('btn-confirm').click();

  await expect(page.getByText(MESSAGE).first()).toBeVisible();
  // Xác nhận thất bại: phiếu vẫn là nháp, nút Xác nhận còn để thử lại sau khi nhập hàng
  await expect(page.getByText('Nháp', { exact: true })).toBeVisible();
  await expect(page.getByTestId('btn-confirm')).toBeVisible();
  await expect(page.getByTestId('sales-line-row')).toHaveCount(1);
});
