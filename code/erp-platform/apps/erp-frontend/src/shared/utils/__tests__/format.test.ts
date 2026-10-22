import { describe, it, expect } from 'vitest';
import {
  formatNumber,
  formatCurrency,
  parseNumber,
  formatDate,
  toDateKey,
  formatDraftNumber,
  escapeRegExp,
  normalizeSearch,
  formatCompact,
} from '../format';

describe('format utilities', () => {
  it('U-FMT-01: formatNumber(1500000)', () => {
    expect(formatNumber(1500000)).toBe('1.500.000');
  });

  it('U-FMT-02: formatCurrency(250000)', () => {
    const res = formatCurrency(250000).replace(/\s/g, ' ');
    expect(res).toContain('250.000');
  });

  it('U-FMT-03: parseNumber(\'1.500.000\')', () => {
    expect(parseNumber('1.500.000')).toBe(1500000);
  });

  it('U-FMT-04: formatDate', () => {
    expect(formatDate('2023-12-31T10:00:00')).toContain('31/12/2023');
  });

  it('U-FMT-05: formatDate chuỗi yyyy-MM-dd không lệch ngày', () => {
    expect(formatDate('2026-09-27')).toBe('27/09/2026');
  });

  it('U-FMT-06: toDateKey theo giờ địa phương', () => {
    expect(toDateKey(new Date(2026, 8, 27, 1, 0))).toBe(20260927);
  });

  it('U-FMT-07: parseNumber hiểu dấu phẩy thập phân và số âm', () => {
    expect(parseNumber('1.234,5')).toBe(1234.5);
    expect(parseNumber('-12.000')).toBe(-12000);
    expect(parseNumber('')).toBe(0);
  });

  it('U-FMT-08: formatDraftNumber giữ dấu phẩy khi đang gõ', () => {
    expect(formatDraftNumber('1500000')).toBe('1.500.000');
    expect(formatDraftNumber('1,', 3)).toBe('1,');
    expect(formatDraftNumber('1,2345', 3)).toBe('1,234');
    expect(formatDraftNumber('-', 0, true)).toBe('-');
    expect(formatDraftNumber('1,5', 0)).toBe('15'); // không cho thập phân → bỏ dấu phẩy
  });

  it('U-FMT-09: escapeRegExp an toàn với ký tự đặc biệt', () => {
    expect(() => new RegExp(escapeRegExp('(500ml) +84 [x]'))).not.toThrow();
  });

  it('U-FMT-10: normalizeSearch bỏ dấu tiếng Việt', () => {
    expect(normalizeSearch('Nguyễn Văn Đức')).toBe('nguyen van duc');
  });

  it('U-FMT-11: formatCompact cho trục biểu đồ', () => {
    expect(formatCompact(1500000)).toBe('1,5 tr');
    expect(formatCompact(850000)).toBe('850 N');
  });
});
