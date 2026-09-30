export const formatNumber = (v: number | null | undefined, maxFractionDigits = 0): string => {
  if (v === null || v === undefined || Number.isNaN(v)) return '';
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: maxFractionDigits }).format(v);
};

export const formatCurrency = (v: number | null | undefined): string => {
  if (v === null || v === undefined || Number.isNaN(v)) return '';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v);
};

/** Rút gọn số lớn cho trục biểu đồ: 1.500.000 → "1,5 tr", 850.000 → "850 N". */
export const formatCompact = (v: number | null | undefined): string => {
  const n = Number(v ?? 0);
  const abs = Math.abs(n);
  if (abs >= 1_000_000_000) return `${formatNumber(n / 1_000_000_000, 1)} tỷ`;
  if (abs >= 1_000_000) return `${formatNumber(n / 1_000_000, 1)} tr`;
  if (abs >= 1_000) return `${formatNumber(n / 1_000, 1)} N`;
  return formatNumber(n);
};

/** Đọc số theo kiểu Việt Nam: "." phân cách hàng nghìn, "," là dấu thập phân. */
export const parseNumber = (s: string): number => {
  if (!s) return 0;
  const negative = s.trim().startsWith('-');
  const cleaned = s.replace(/[^\d,]/g, '').replace(',', '.');
  if (cleaned === '' || cleaned === '.') return 0;
  const num = Number(cleaned);
  if (Number.isNaN(num)) return 0;
  return negative ? -num : num;
};

/**
 * Định dạng chuỗi người dùng ĐANG GÕ: chèn dấu phân cách hàng nghìn cho phần nguyên,
 * giữ nguyên dấu "," và phần thập phân (kể cả "1," hoặc "1,50") để không mất ký tự khi gõ.
 */
export const formatDraftNumber = (raw: string, maxFractionDigits = 0, allowNegative = false): string => {
  const negative = allowNegative && raw.trim().startsWith('-');
  const hasComma = maxFractionDigits > 0 && raw.includes(',');
  // Ô số nguyên: bỏ mọi ký tự không phải số (dán "1,500,000" vẫn ra 1.500.000).
  const [intRaw, fracRaw = ''] = hasComma ? raw.split(',') : [raw, ''];
  const intDigits = intRaw.replace(/\D/g, '').replace(/^0+(?=\d)/, '');
  const fracDigits = fracRaw.replace(/\D/g, '').slice(0, maxFractionDigits);
  const intFormatted = intDigits ? new Intl.NumberFormat('vi-VN').format(Number(intDigits)) : hasComma ? '0' : '';
  return `${negative ? '-' : ''}${intFormatted}${hasComma ? `,${fracDigits}` : ''}`;
};

/** Escape ký tự đặc biệt để dùng chuỗi người dùng gõ trong RegExp. */
export const escapeRegExp = (s: string): string => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

/** Bỏ dấu tiếng Việt + chữ thường, dùng cho tìm kiếm phía client. */
export const normalizeSearch = (s: string | null | undefined): string =>
  (s ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim();

/** Chuỗi ngày: 'yyyy-MM-dd' được hiểu là ngày địa phương (không bị lệch múi giờ). */
const toDate = (v: string | Date): Date => {
  if (v instanceof Date) return v;
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(v);
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  return new Date(v);
};

export const formatDate = (v: string | Date | null | undefined): string => {
  if (!v) return '';
  const d = toDate(v);
  if (isNaN(d.getTime())) return '';
  const day = d.getDate().toString().padStart(2, '0');
  const month = (d.getMonth() + 1).toString().padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
};

export const formatDateTime = (v: string | Date | null | undefined): string => {
  if (!v) return '';
  const d = toDate(v);
  if (isNaN(d.getTime())) return '';
  const hh = d.getHours().toString().padStart(2, '0');
  const mm = d.getMinutes().toString().padStart(2, '0');
  return `${formatDate(d)} ${hh}:${mm}`;
};

export const toDateKey = (d: Date): number => {
  if (!d || isNaN(d.getTime())) return 0;
  const year = d.getFullYear();
  const month = (d.getMonth() + 1).toString().padStart(2, '0');
  const day = d.getDate().toString().padStart(2, '0');
  return parseInt(`${year}${month}${day}`, 10);
};

export const toInputDate = (d: Date): string => {
  if (!d || isNaN(d.getTime())) return '';
  const year = d.getFullYear();
  const month = (d.getMonth() + 1).toString().padStart(2, '0');
  const day = d.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
};
