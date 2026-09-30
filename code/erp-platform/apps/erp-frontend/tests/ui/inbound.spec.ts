import { test, expect, type Route } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi, ok } from '../helpers/mockApi';
import { products, suppliers } from '../helpers/fixtures';

/** Trạng thái phiếu phía "server" giả, để GET sau khi xác nhận trả đúng CONFIRMED. */
let receiptStatus: 'DRAFT' | 'CONFIRMED' = 'DRAFT';

const receiptDto = () => ({
  id: 'rcpt-1',
  receiptCode: 'PN0001',
  status: receiptStatus,
  supplierId: suppliers[0].id,
  note: 'Nhập đầu tháng',
  createdAt: '2026-09-27T09:30:00',
  lines: [{ productId: 101, quantity: 5, unitCost: 40000, unitOfMeasure: 'CAI' }],
});

test.describe('Inbound Receipt Form', () => {
  test.beforeEach(async ({ page }) => {
    receiptStatus = 'DRAFT';
    await loginAs(page, 'STAFF');
    await mockApi(page, {
      '**/api/v1/suppliers*': (route: Route) => route.fulfill(ok(suppliers)),
      '**/api/v1/catalog/products*': (route: Route) => route.fulfill(ok(products)),
      '**/api/v1/inventory/inbound': (route: Route) =>
        route.request().method() === 'POST' ? route.fulfill(ok({ id: 'rcpt-1' })) : route.fulfill(ok([receiptDto()])),
      '**/api/v1/inventory/inbound/rcpt-1': (route: Route) => route.fulfill(ok(receiptDto())),
      '**/api/v1/inventory/inbound/rcpt-1/confirm': (route: Route) => {
        receiptStatus = 'CONFIRMED';
        return route.fulfill(ok(null));
      },
    });
    await page.goto('/');
    await page.getByTestId('ribbon-tab-ChucNang').click();
    await page.getByTestId('ribbon-btn-inbound').click();
  });

  test('E-INB-01: Mở phiếu nhập hàng', async ({ page }) => {
    await expect(page.getByText('Phiếu nhập hàng')).toBeVisible();
    await expect(page.getByTestId('inbound-supplier-combo')).toBeVisible();
    await expect(page.getByTestId('doc-code')).toContainText('(Tự động)');
  });

  test('E-INB-02 → 04: Chọn NCC, thêm hàng, sửa giá nhập, lưu và xác nhận', async ({ page }) => {
    // E-INB-02: chọn NCC + sản phẩm, sửa số lượng và giá nhập
    await page.getByTestId('inbound-supplier-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nhà cung cấp A').click();
    await page.getByTestId('inbound-add-line').click();
    await page.getByTestId('combobox-dropdown').getByText('Sản phẩm 1').click(); // ô sản phẩm dòng mới được focus sẵn

    const qty = page.getByTestId('inbound-line-quantity');
    await expect(qty).toBeFocused();
    await page.keyboard.type('5');
    await page.keyboard.press('Enter'); // Enter → sang ô giá nhập
    await expect(page.getByTestId('inbound-line-price')).toBeFocused();
    await page.keyboard.type('40000');

    await expect(page.getByTestId('inbound-line-code')).toHaveText('SP01');
    await expect(page.getByTestId('inbound-line-total')).toContainText('200.000');

    // E-INB-03: lưu → payload đúng (unitCost, không có unitPrice)
    const requestPromise = page.waitForRequest(
      (req) => req.url().endsWith('/api/v1/inventory/inbound') && req.method() === 'POST',
    );
    await page.getByTestId('btn-save').click();
    const payload = (await requestPromise).postDataJSON();
    expect(payload.supplierId).toBe(suppliers[0].id);
    expect(payload.lines).toEqual([{ productId: 101, quantity: 5, unitCost: 40000, unitOfMeasure: 'CAI' }]);

    await expect(page.getByText('Lưu phiếu nhập thành công')).toBeVisible();
    await expect(page.getByTestId('doc-code')).toContainText('PN0001');
    await expect(page.getByText('Nháp', { exact: true })).toBeVisible();

    // E-INB-04: xác nhận → badge đổi, nút Xác nhận biến mất
    await page.getByTestId('btn-confirm').click();
    await expect(page.getByText('Xác nhận phiếu nhập thành công')).toBeVisible();
    await expect(page.getByText('Đã xác nhận', { exact: true })).toBeVisible();
    await expect(page.getByTestId('btn-confirm')).toBeHidden();
  });

  test('E-INB-05: Form trống bấm Lưu báo lỗi', async ({ page }) => {
    await page.getByTestId('btn-save').click();
    await expect(page.getByText('Phiếu nhập phải có ít nhất 1 sản phẩm')).toBeVisible();
  });

  test('E-INB-06: Dòng chưa chọn sản phẩm bị tô đỏ, không gửi request', async ({ page }) => {
    let posted = false;
    page.on('request', (r) => {
      if (r.url().endsWith('/api/v1/inventory/inbound') && r.method() === 'POST') posted = true;
    });
    await page.getByTestId('inbound-supplier-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nhà cung cấp A').click();
    await page.getByTestId('inbound-add-line').click();
    await page.keyboard.press('Escape'); // đóng dropdown sản phẩm
    await page.getByTestId('btn-save').click();
    await expect(page.getByText('Vui lòng kiểm tra lại thông tin nhập.')).toBeVisible();
    await expect(page.locator('[data-testid="inbound-line-row"] td.ring-danger')).toHaveCount(1);
    expect(posted).toBe(false);
  });

  test('E-INB-07: Danh sách phiếu hiển thị tên NCC, tổng tiền; nhấp đúp mở phiếu có tên hàng', async ({ page }) => {
    await page.getByTestId('subview-list').click();
    const row = page.getByTestId('inbound-list-row').first();
    await expect(row).toContainText('PN0001');
    await expect(row).toContainText('Nhà cung cấp A');
    await expect(row).toContainText('200.000');
    await row.dblclick();
    await expect(page.getByTestId('inbound-partner-name')).toHaveText('Nhà cung cấp A');
    await expect(page.getByTestId('inbound-line-row').first()).toContainText('Sản phẩm 1');
  });

  test('E-INB-08: Chuyển sang Danh sách phiếu rồi quay lại không mất dữ liệu đang nhập', async ({ page }) => {
    await page.getByTestId('inbound-supplier-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nhà cung cấp A').click();
    await page.getByTestId('subview-list').click();
    await page.getByTestId('subview-form').click();
    await expect(page.getByTestId('inbound-supplier-combo')).toHaveValue('Nhà cung cấp A');
  });

  test('E-INB-09: Thoát khi đang nhập → hỏi xác nhận → đóng tab', async ({ page }) => {
    await page.getByTestId('inbound-supplier-combo').click();
    await page.getByTestId('combobox-dropdown').getByText('Nhà cung cấp A').click();
    await page.getByTestId('btn-exit').click();
    await page.getByTestId('confirm-dialog-confirm').click();
    await expect(page.getByTestId('tab-new-inbound')).toHaveCount(0);
  });
});
