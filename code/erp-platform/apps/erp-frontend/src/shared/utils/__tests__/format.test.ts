import { describe, it, expect } from 'vitest';
import { formatNumber, formatCurrency, parseNumber, formatDate, toDateKey, toInputDate } from '../format';

describe('format utilities', () => {
  it('U-FMT-01: formatNumber(1500000)', () => {
    expect(formatNumber(1500000)).toBe('1.500.000');
  });

  it('U-FMT-02: formatCurrency(250000)', () => {
    const res = formatCurrency(250000).replace(/ /g, ' ');
    expect(res).toContain('250.000');
    expect(res).toContain('₫');
  });

  it('U-FMT-03: parseNumber(\'1.500.000\')', () => {
    expect(parseNumber('1.500.000')).toBe(1500000);
  });

  it('U-FMT-04: toDateKey(new Date(2026, 8, 27, 1, 0))', () => {
    expect(toDateKey(new Date(2026, 8, 27, 1, 0))).toBe(20260927);
  });

  it('U-FMT-05: formatDate(\'2026-09-27\')', () => {
    expect(formatDate('2026-09-27')).toBe('27/09/2026');
  });
});
