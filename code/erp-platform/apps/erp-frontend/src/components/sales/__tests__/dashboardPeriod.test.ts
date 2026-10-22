import { describe, it, expect } from 'vitest';
import { getPeriodRange } from '../dashboardPeriod';

describe('dashboardPeriod', () => {
  const mockNow = new Date(2026, 8, 27); // 27/09/2026 (Month is 0-indexed in JS)

  it('U-DASH-01: getPeriodRange month', () => {
    const result = getPeriodRange('month', mockNow);
    expect(result).toEqual({ start: 20260901, end: 20260927 });
  });

  it('U-DASH-02: getPeriodRange 7d', () => {
    const result = getPeriodRange('7d', mockNow);
    expect(result).toEqual({ start: 20260921, end: 20260927 });
  });

  it('U-DASH-03: getPeriodRange quarter', () => {
    const result = getPeriodRange('quarter', mockNow);
    expect(result.start).toBe(20260701);
    expect(result.end).toBe(20260927);
  });

  it('U-DASH-04: getPeriodRange custom', () => {
    const result = getPeriodRange('custom', mockNow, { start: '2026-01-01', end: '2026-01-31' });
    expect(result).toEqual({ start: 20260101, end: 20260131 });
  });
});
