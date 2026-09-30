import { test, expect, type Route } from '@playwright/test';
import { loginAs } from '../helpers/session';
import { mockApi, ok } from '../helpers/mockApi';

test.describe('Danh mục & tồn kho', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'STAFF');
    await mockApi(page, {
      '**/api/v1/catalog/products*': (route: Route) =>
        route.fulfill(
          ok([
            { id: 1, code: 'SP01', name: 'Sản phẩm 1', baseUnit: 'CAI', categoryId: 1, price: 50000 },
            { id: 2, code: 'SP02', name: 'Sản phẩm 2', baseUnit: 'HOP', categoryId: 1, price: 1500000 },
          ]),
        ),
      '**/api/v1/inventory/stock': (route: Route) =>
        route.fulfill(
          ok([
            { id: 's1', productId: 1, quantity: 10, productCode: 'SP01', productName: 'Sản phẩm 1', branchName: 'Kho trung tâm' },
            { id: 's2', productId: 2, quantity: -3, productCode: 'SP02', productName: 'Sản phẩm 2', branchName: 'Kho trung tâm' },
          ]),
        ),
    });
    await page.goto('/');
    await expect(page.getByTestId('ribbon-tab-DanhMuc')).toBeVisible();
  });

  test('E-LIST-01 & 02: Danh mục sản phẩm hiển thị và tìm kiếm (không dấu, theo mã)', async ({ page }) => {
    await page.getByTestId('ribbon-tab-DanhMuc').click();
    await page.getByTestId('ribbon-btn-products').click();

    await expect(page.getByRole('heading', { name: 'Danh Mục Sản Phẩm' })).toBeVisible();
    await expect(page.getByTestId('product-row')).toHaveCount(2);
    await expect(page.getByTestId('product-row').nth(1)).toContainText('1.500.000');

    const search = page.getByPlaceholder('Tìm theo mã, tên...');
    await search.fill('SP02');
    await expect(page.getByTestId('product-row')).toHaveCount(1);
    await expect(page.getByTestId('product-row')).toContainText('Sản phẩm 2');

    await search.fill('san pham 1'); // gõ không dấu vẫn tìm được
    await expect(page.getByTestId('product-row')).toHaveCount(1);
    await expect(page.getByTestId('product-row')).toContainText('Sản phẩm 1');

    await search.fill('khong-co');
    await expect(page.getByTestId('list-no-match')).toBeVisible();
  });

  test('E-LIST-03: Tồn kho âm hiển thị màu đỏ', async ({ page }) => {
    await page.getByTestId('ribbon-tab-TonKho').click();
    await page.getByTestId('ribbon-btn-stocks').click();

    await expect(page.getByRole('heading', { name: 'Danh Mục Tồn Kho' })).toBeVisible();
    const negative = page.getByTestId('stock-row').filter({ hasText: 'SP02' }).getByTestId('stock-qty');
    await expect(negative).toHaveText('-3');
    await expect(negative).toHaveClass(/text-danger/);
  });
});
