// ---------------------------------------------------------------------------
// Giá theo khách trên phiếu bán: hệ thống đề xuất giá (giá riêng KH > niêm yết),
// nhân viên được sửa. Giá đã sửa được backend lưu làm giá riêng của khách khi lưu đơn
// (CustomerProductPriceService.applyFromInvoice) và tự áp lại cho đơn sau.
// ---------------------------------------------------------------------------
import type { DocumentLine, LinePriceInfo, PriceSource, ResolvedPriceDto } from '../../types/documents';
import { formatCurrency } from '../../shared/utils/format';
import type { ProductLike } from '../common/document/documentLines';

const PRICE_EPSILON = 0.00005; // backend lưu NUMERIC(19,4)
const samePrice = (a: number, b: number) => Math.abs(Number(a) - Number(b)) < PRICE_EPSILON;

export const SAVE_PRICE_NOTE = 'Giá này sẽ được lưu làm giá riêng cho khách khi lưu đơn.';

const PRICE_LABEL: Record<PriceSource, Pick<LinePriceInfo, 'label' | 'tone'>> = {
  CUSTOMER: { label: 'Giá KH', tone: 'customer' },
  LIST: { label: 'Niêm yết', tone: 'list' },
  NONE: { label: 'Chưa có giá', tone: 'none' },
};

/** Giá trên dòng khác giá hệ thống đang áp dụng (hoặc tự nhập khi chưa có giá nào). */
export const differsFromApplied = (l: DocumentLine) =>
  l.appliedPrice == null ? Number(l.unitPrice) > 0 : !samePrice(l.unitPrice, l.appliedPrice);

/** Dòng đã sửa giá -> sẽ được lưu làm giá riêng của khách (cần có khách và sản phẩm). */
export const isLinePriceEdited = (l: DocumentLine, hasCustomer: boolean) =>
  hasCustomer && !!l.productId && l.priceSource !== undefined && differsFromApplied(l);

/** Khi chọn sản phẩm: tạm dùng giá niêm yết chi nhánh (giá riêng của khách được tra sau). */
export const listPricePatch = (p: ProductLike): Partial<DocumentLine> => {
  const listPrice = p.price != null ? Number(p.price) : null;
  return {
    priceSource: listPrice != null ? 'LIST' : 'NONE',
    appliedPrice: listPrice,
    listPrice,
    unitPrice: listPrice ?? 0,
  };
};

/** Áp kết quả tra giá vào dòng; giữ nguyên giá nếu nhân viên đã sửa tay. */
export const applyResolvedPrice = (l: DocumentLine, r: ResolvedPriceDto): DocumentLine => {
  const keepManual = l.priceSource !== undefined && differsFromApplied(l);
  const applied = r.unitPrice == null ? null : Number(r.unitPrice);
  return {
    ...l,
    priceSource: r.source,
    appliedPrice: applied,
    listPrice: r.listPrice == null ? null : Number(r.listPrice),
    unitPrice: keepManual || applied == null ? l.unitPrice : applied,
  };
};

/** Nhãn nguồn giá + tooltip + cờ "đã sửa" cho ô đơn giá. */
export const linePriceInfo = (l: DocumentLine, hasCustomer: boolean): LinePriceInfo | null => {
  if (!l.productId || !l.priceSource) return null;
  const edited = isLinePriceEdited(l, hasCustomer);
  const tips = [`Giá niêm yết: ${l.listPrice != null ? formatCurrency(l.listPrice) : 'chưa có'}`];
  if (l.priceSource === 'CUSTOMER' && l.appliedPrice != null) {
    tips.push(`Giá riêng của khách: ${formatCurrency(l.appliedPrice)}`);
  }
  if (edited) tips.push(SAVE_PRICE_NOTE);
  return { ...PRICE_LABEL[l.priceSource], title: tips.join('\n'), edited };
};
