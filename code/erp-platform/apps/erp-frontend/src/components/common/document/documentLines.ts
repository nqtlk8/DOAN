import { useCallback, useState } from 'react';
import type { DocumentLine } from '../../../types/documents';

export type LineField = 'product' | 'quantity' | 'price';

/** Số chữ số thập phân cho số lượng (vd 1,5 kg). */
export const QTY_FRACTION_DIGITS = 3;

/**
 * Đưa con trỏ tới một ô trong bảng hàng hóa (dùng cho luồng bàn phím B7).
 * Ô sản phẩm là combobox do form cha render → tìm input bên trong wrapper.
 */
export const focusLineCell = (lineId: string, field: LineField) => {
  setTimeout(() => {
    const el = document.querySelector<HTMLElement>(`[data-line-id="${lineId}"][data-field="${field}"]`);
    const input = el?.tagName === 'INPUT' ? (el as HTMLInputElement) : el?.querySelector<HTMLInputElement>('input');
    input?.focus();
  }, 0);
};

/** Key lỗi dùng chung giữa form cha và GenericDocumentForm. */
export const lineErrorKey = (index: number, field: 'product' | 'quantity') => `line_${index}_${field}`;

let seq = 0;
/** Id tạm phía client cho một dòng hàng hóa. */
export const newLineId = () => `line_${Date.now().toString(36)}_${(seq++).toString(36)}`;

export const emptyLine = (): DocumentLine => ({
  id: newLineId(),
  productId: '',
  productCode: '',
  productName: '',
  unitOfMeasure: '',
  quantity: 1,
  unitPrice: 0,
});

/** Sản phẩm (từ API catalog) → các trường của dòng hàng hóa. */
export interface ProductLike {
  id?: number | string;
  code?: string;
  name?: string;
  baseUnit?: string;
  price?: number;
}
export const productToLinePatch = (p: ProductLike): Partial<DocumentLine> => ({
  productId: p.id != null ? String(p.id) : '',
  productCode: p.code || '',
  productName: p.name || '',
  unitOfMeasure: p.baseUnit || 'CAI',
  unitPrice: Number(p.price ?? 0),
});

/**
 * State danh sách dòng hàng hóa. MỌI thao tác đều dùng functional update (prev => …),
 * nên gọi nhiều lần liên tiếp trong cùng một sự kiện không làm mất dữ liệu.
 */
export function useDocumentLines(initial: DocumentLine[] | (() => DocumentLine[]) = []) {
  const [lines, setLines] = useState<DocumentLine[]>(initial);

  const addLine = useCallback(() => setLines((prev) => [...prev, emptyLine()]), []);
  const removeLine = useCallback((id: string) => setLines((prev) => prev.filter((l) => l.id !== id)), []);
  const updateLine = useCallback(
    (id: string, field: keyof DocumentLine, value: unknown) =>
      setLines((prev) => prev.map((l) => (l.id === id ? { ...l, [field]: value } : l))),
    [],
  );
  const patchLine = useCallback(
    (id: string, patch: Partial<DocumentLine>) =>
      setLines((prev) => prev.map((l) => (l.id === id ? { ...l, ...patch } : l))),
    [],
  );

  return { lines, setLines, addLine, removeLine, updateLine, patchLine };
}

/** Kiểm tra dòng hàng hóa; trả về map lỗi theo key của GenericDocumentForm. */
export function validateLines(lines: DocumentLine[]): Record<string, string> {
  const errors: Record<string, string> = {};
  lines.forEach((l, i) => {
    if (!l.productId) errors[lineErrorKey(i, 'product')] = 'Chọn sản phẩm';
    if (!(Number(l.quantity) > 0)) errors[lineErrorKey(i, 'quantity')] = 'Số lượng phải > 0';
  });
  return errors;
}

export const sumLines = (lines: DocumentLine[]) =>
  lines.reduce((s, l) => s + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0), 0);

export const sumQuantity = (lines: DocumentLine[]) => lines.reduce((s, l) => s + (Number(l.quantity) || 0), 0);

/** Bỏ một key khỏi object lỗi (vd xóa lỗi 'partner' khi đã chọn đối tác). */
export const omitKey = (obj: Record<string, string>, key: string): Record<string, string> => {
  if (!(key in obj)) return obj;
  const next = { ...obj };
  delete next[key];
  return next;
};
