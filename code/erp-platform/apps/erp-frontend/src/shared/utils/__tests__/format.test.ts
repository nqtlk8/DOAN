import { describe, it, expect } from 'vitest';
import { formatNumber, formatCurrency, parseNumber, formatDate, toDateKey } from '../format';

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
});
