export const formatNumber = (v: number | null | undefined, maxFractionDigits = 0): string => {
  if (v === null || v === undefined) return '';
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: maxFractionDigits }).format(v);
};

export const formatCurrency = (v: number | null | undefined): string => {
  if (v === null || v === undefined) return '';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v);
};

export const parseNumber = (s: string): number => {
  if (!s) return 0;
  const cleaned = s.replace(/[^\d,-]/g, '').replace(',', '.');
  const num = Number(cleaned);
  return isNaN(num) ? 0 : num;
};

export const formatDate = (v: string | Date | null | undefined): string => {
  if (!v) return '';
  const d = typeof v === 'string' ? new Date(v) : v;
  if (isNaN(d.getTime())) return '';
  const day = d.getDate().toString().padStart(2, '0');
  const month = (d.getMonth() + 1).toString().padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
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
