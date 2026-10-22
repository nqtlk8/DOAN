import { test, expect, type Route } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi, ok } from '../helpers/mockApi';
import { customers, products } from '../helpers/fixtures';

/**
 * Sửa phiếu khi còn nháp: Lưu (nháp) -> Sửa (F3) -> Lưu (PUT) -> Xác nhận (F9).
 * Nút Sửa chỉ có khi phiếu là nháp; đang sửa thì không xác nhận được (phải Lưu hoặc Hủy trước).
 */

test.describe('Sửa phiếu trả hàng nháp', () => {
  let status: 'DRAFT' | 'CONFIRMED';
  let reason: string;
  let quantity: number;
  let putCount: number;
  const dto = () => ({
    id: 'ret-1',
    returnCode: 'TH0001',
    status,
    customerId: customers[1].id,
    customerName: customers[1].name,
    reason,
    totalAmount: quantity * 100000,
    lines: [{ productId: 102, productCode: 'SP02', productName: 'Sản phẩm 2', quantity, unitOfMeasure: 'HOP', unitPrice: 100000 }],
  });

  test.beforeEach(async ({ page }) => {
    status = 'DRAFT';
    reason = 'Hàng lỗi';
    quantity = 2;
    putCount = 0;
    await loginAs(page, 'STAFF');
    await mockApi(page, {
      '**/api/v1/customers*': (route: Route) => route.fulfill(ok(customers)),
      '**/api/v1/catalog/products*': (route: Route) => route.fulfill(ok(products)),
      '**/api/v1/goods-returns': (route: Route) =>
        route.request().method() === 'POST' ? route.fulfill(ok('ret-1')) : route.fulfill(ok([dto()])),
      '**/api/v1/goods-returns/ret-1': (route: Route) => {
        if (route.request().method() === 'PUT') {
          putCount++;
          const body = route.request().postDataJSON();
          reason = body.reason;
          quantity = body.lines[0].quantity;
          return route.fulfill(ok('ret-1'));
        }
        return route.fulfill(ok(dto()));
      },
      '**/api/v1/goods-returns/ret-1/confirm': (route: Route) => {
        status = 'CONFIRMED';
        return route.fulfill(ok(null));
      },
    });
    await page.goto('/');
    await page.getByTestId('ribbon-tab-ChucNang').click();
    await page.getByTestId('ribbon-btn-return').click();

    // Lập phiếu và lưu nháp.
    await page.getByTestId('return-customer-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Trần Thị B').click();
    await page.getByTestId('return-add-line').click();
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 2').click();
    await expect(page.getByTestId('return-line-quantity')).toBeFocused();
    await page.keyboard.type('2');
    await page.getByTestId('return-reason').fill('Hàng lỗi');
    await page.getByTestId('btn-save').click();
    await expect(page.getByTestId('doc-code')).toContainText('TH0001');
    await expect(page.getByText('Nháp', { exact: true })).toBeVisible();
  });

  test('E-EDIT-01: Sửa nháp -> Lưu gửi PUT (key sửa riêng) -> Xác nhận; đã xác nhận thì hết nút Sửa', async ({ page }) => {
    await expect(page.getByTestId('btn-edit')).toBeVisible();
    await page.getByTestId('btn-edit').click();

    await expect(page.getByTestId('doc-editing-badge')).toBeVisible();
    // Đang sửa: không có nút Xác nhận / Sửa; chỉ Lưu, Hủy.
    await expect(page.getByTestId('btn-confirm')).toBeHidden();
    await expect(page.getByTestId('btn-edit')).toBeHidden();
    await expect(page.getByTestId('btn-save')).toBeVisible();

    await page.getByTestId('return-reason').fill('Hàng móp');
    await page.getByTestId('return-line-quantity').fill('3');

    const req = page.waitForRequest((r) => r.url().endsWith('/api/v1/goods-returns/ret-1') && r.method() === 'PUT');
    await page.getByTestId('btn-save').click();
    const put = await req;
    expect(put.postDataJSON()).toMatchObject({
      customerId: customers[1].id,
      reason: 'Hàng móp',
      lines: [{ productId: 102, quantity: 3, unitPrice: 100000, unitOfMeasure: 'HOP' }],
    });
    expect(put.headers()['idempotency-key']).toMatch(/^goods-return-update:ret-1:/);

    await expect(page.getByText('Đã lưu thay đổi phiếu trả (nháp)')).toBeVisible();
    await expect(page.getByTestId('doc-editing-badge')).toBeHidden();
    await expect(page.getByText('Nháp', { exact: true })).toBeVisible();
    await expect(page.getByTestId('return-grand-total')).toContainText('300.000');
    expect(putCount).toBe(1);

    await page.getByTestId('btn-confirm').click();
    await expect(page.getByText('Xác nhận trả hàng thành công')).toBeVisible();
    await expect(page.getByText('Đã xác nhận', { exact: true })).toBeVisible();
    await expect(page.getByTestId('btn-edit')).toBeHidden();

    // F3 trên phiếu đã xác nhận: không vào chế độ sửa.
    await page.keyboard.press('F3');
    await expect(page.getByTestId('doc-editing-badge')).toBeHidden();
    expect(putCount).toBe(1);
  });

  test('E-EDIT-02: F3 vào sửa, Hủy -> Đồng ý: bỏ thay đổi, không gửi request', async ({ page }) => {
    await page.keyboard.press('F3');
    await expect(page.getByTestId('doc-editing-badge')).toBeVisible();
    await page.getByTestId('return-reason').fill('Đổi ý');

    await page.getByTestId('btn-cancel').click();
    await page.getByRole('button', { name: 'Đồng ý' }).click();

    await expect(page.getByTestId('doc-editing-badge')).toBeHidden();
    await expect(page.getByTestId('return-reason')).toHaveText('Hàng lỗi');
    await expect(page.getByTestId('btn-confirm')).toBeVisible();
    expect(putCount).toBe(0);
  });
});

test('E-EDIT-03: Hóa đơn nháp đã bị xác nhận ở nơi khác -> Lưu sửa báo lỗi, form về trạng thái mới', async ({ page }) => {
  let invoiceStatus: 'DRAFT' | 'CONFIRMED' = 'DRAFT';
  await loginAs(page, 'STAFF');
  await mockApi(page, {
    '**/api/v1/customers*': (route) => route.fulfill(ok(customers)),
    '**/api/v1/catalog/products*': (route) => route.fulfill(ok(products)),
    '**/api/v1/receivable-debts/*/balance': (route) => route.fulfill(ok(0)),
    '**/api/v1/sales-invoices': (route) => {
      if (route.request().method() === 'POST') return route.fulfill(ok({ id: 'inv-1', invoiceCode: 'HD0001' }));
      return route.fallback();
    },
    '**/api/v1/sales-invoices/inv-1': (route) => {
      if (route.request().method() === 'PUT') {
        return route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Chỉ hóa đơn nháp (DRAFT) mới được sửa.', errors: ['INVALID_STATUS'] }),
        });
      }
      return route.fulfill(ok({ id: 'inv-1', invoiceCode: 'HD0001', status: invoiceStatus }));
    },
  });
  await page.goto('/');

  await page.getByTestId('sales-customer-combo').click();
  await page.getByTestId('combobox-dropdown').getByText('Nguyễn Văn A').click();
  await page.getByTestId('sales-add-line').click();
  await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click();
  await expect(page.getByTestId('sales-line-quantity')).toBeFocused();
  await page.keyboard.type('2');
  await page.keyboard.press('Tab');
  await page.getByTestId('btn-save').click();
  await expect(page.getByTestId('doc-code')).toContainText('HD0001');

  await page.getByTestId('btn-edit').click();
  await expect(page.getByTestId('doc-editing-badge')).toBeVisible();
  await page.getByTestId('sales-line-quantity').fill('9');

  invoiceStatus = 'CONFIRMED'; // người khác vừa xác nhận
  await page.getByTestId('btn-save').click();

  await expect(page.getByText('Phiếu không còn ở trạng thái nháp').first()).toBeVisible();
  await expect(page.getByTestId('doc-editing-badge')).toBeHidden();
  await expect(page.getByText('Đã xác nhận', { exact: true })).toBeVisible();
  await expect(page.getByTestId('btn-edit')).toBeHidden();
  await expect(page.getByTestId('btn-confirm')).toBeHidden();
  // Nội dung trả về bản đã lưu (SL 2), không giữ số đang sửa dở.
  await expect(page.getByTestId('sales-line-row').first()).toContainText('2');
  await expect(page.getByTestId('sales-total-qty')).toHaveText('2');
});
