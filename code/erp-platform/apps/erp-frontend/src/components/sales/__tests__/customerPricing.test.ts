import { describe, it, expect } from 'vitest';
import type { DocumentLine, ResolvedPriceDto } from '../../../types/documents';
import {
  applyResolvedPrice,
  differsFromApplied,
  isLinePriceEdited,
  linePriceInfo,
  listPricePatch,
  SAVE_PRICE_NOTE,
} from '../customerPricing';

const line = (over: Partial<DocumentLine> = {}): DocumentLine => ({
  id: 'l1',
  productId: '101',
  productName: 'Sản phẩm 1',
  quantity: 1,
  unitPrice: 100000,
  priceSource: 'LIST',
  appliedPrice: 100000,
  listPrice: 100000,
  ...over,
});

const resolved = (over: Partial<ResolvedPriceDto> = {}): ResolvedPriceDto => ({
  productId: 101,
  unitPrice: 90000,
  source: 'CUSTOMER',
  listPrice: 100000,
  customerPrice: 90000,
  ...over,
});

describe('customerPricing', () => {
  it('U-CP-01: chọn sản phẩm -> tạm dùng giá niêm yết, không có giá -> NONE', () => {
    expect(listPricePatch({ id: 1, price: 50000 })).toEqual({
      priceSource: 'LIST', appliedPrice: 50000, listPrice: 50000, unitPrice: 50000,
    });
    expect(listPricePatch({ id: 1 })).toEqual({
      priceSource: 'NONE', appliedPrice: null, listPrice: null, unitPrice: 0,
    });
  });

  it('U-CP-02: dòng chưa sửa giá -> áp giá riêng của khách', () => {
    const next = applyResolvedPrice(line(), resolved());
    expect(next.unitPrice).toBe(90000);
    expect(next.priceSource).toBe('CUSTOMER');
    expect(next.appliedPrice).toBe(90000);
    expect(next.listPrice).toBe(100000);
  });

  it('U-CP-03: dòng đã sửa tay -> giữ giá đã sửa, chỉ cập nhật giá đang áp dụng', () => {
    const next = applyResolvedPrice(line({ unitPrice: 95000 }), resolved());
    expect(next.unitPrice).toBe(95000);
    expect(next.appliedPrice).toBe(90000);
    expect(isLinePriceEdited(next, true)).toBe(true);
  });

  it('U-CP-04: không có giá nào -> giữ giá nhân viên nhập', () => {
    const next = applyResolvedPrice(
      line({ priceSource: 'NONE', appliedPrice: null, listPrice: null, unitPrice: 0 }),
      resolved({ unitPrice: null, source: 'NONE', listPrice: null, customerPrice: null }),
    );
    expect(next.unitPrice).toBe(0);
    expect(next.priceSource).toBe('NONE');
  });

  it('U-CP-05: so giá theo 4 chữ số thập phân; tự nhập khi chưa có giá = đã sửa', () => {
    expect(differsFromApplied(line({ unitPrice: 100000.00001 }))).toBe(false);
    expect(differsFromApplied(line({ unitPrice: 100000.5 }))).toBe(true);
    expect(differsFromApplied(line({ appliedPrice: null, unitPrice: 5000 }))).toBe(true);
    expect(differsFromApplied(line({ appliedPrice: null, unitPrice: 0 }))).toBe(false);
  });

  it('U-CP-06: chỉ tô "đã sửa" khi đã chọn khách', () => {
    const edited = line({ unitPrice: 80000 });
    expect(isLinePriceEdited(edited, false)).toBe(false);
    expect(isLinePriceEdited(edited, true)).toBe(true);
    expect(isLinePriceEdited(line({ unitPrice: 80000, priceSource: undefined }), true)).toBe(false);
  });

  it('U-CP-07: nhãn "Giá KH"/"Niêm yết" và tooltip có giá niêm yết + ghi chú lưu giá', () => {
    const customer = linePriceInfo(line({ priceSource: 'CUSTOMER', appliedPrice: 90000, unitPrice: 90000 }), true);
    expect(customer).toMatchObject({ label: 'Giá KH', tone: 'customer', edited: false });
    expect(customer?.title).toContain('Giá niêm yết');
    expect(customer?.title).toContain('Giá riêng của khách');

    const list = linePriceInfo(line(), true);
    expect(list).toMatchObject({ label: 'Niêm yết', tone: 'list', edited: false });
    expect(list?.title).not.toContain(SAVE_PRICE_NOTE);

    const edited = linePriceInfo(line({ unitPrice: 120000 }), true);
    expect(edited?.edited).toBe(true);
    expect(edited?.title).toContain(SAVE_PRICE_NOTE);

    expect(linePriceInfo(line({ productId: '' }), true)).toBeNull();
  });
});
