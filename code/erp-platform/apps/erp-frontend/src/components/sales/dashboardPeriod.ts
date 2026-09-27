export type PeriodKey = 'today' | '7d' | 'month' | 'quarter' | 'year' | 'all' | 'custom';

export const PERIOD_OPTIONS: { key: PeriodKey; label: string }[] = [
  { key: 'today', label: 'Hôm nay' },
  { key: '7d', label: '7 ngày qua' },
  { key: 'month', label: 'Tháng này' },
  { key: 'quarter', label: 'Quý này' },
  { key: 'year', label: 'Năm nay' },
  { key: 'all', label: 'Toàn thời gian' },
  { key: 'custom', label: 'Tùy chọn...' },
];

export function getPeriodRange(
  key: PeriodKey,
  now = new Date(),
  custom?: { start: string; end: string }
): { start: number; end: number } {
  // Helpers
  const toDateKey = (d: Date) => {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return Number(`${y}${m}${dd}`);
  };

  const getStartOfQuarter = (d: Date) => {
    const q = Math.floor(d.getMonth() / 3);
    return new Date(d.getFullYear(), q * 3, 1);
  };

  const todayKey = toDateKey(now);

  switch (key) {
    case 'today':
      return { start: todayKey, end: todayKey };
    case '7d': {
      const start = new Date(now);
      start.setDate(now.getDate() - 6);
      return { start: toDateKey(start), end: todayKey };
    }
    case 'month': {
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      return { start: toDateKey(start), end: todayKey };
    }
    case 'quarter': {
      const start = getStartOfQuarter(now);
      return { start: toDateKey(start), end: todayKey };
    }
    case 'year': {
      const start = new Date(now.getFullYear(), 0, 1);
      return { start: toDateKey(start), end: todayKey };
    }
    case 'all':
      return { start: 20200101, end: 20301231 };
    case 'custom': {
      if (!custom || !custom.start || !custom.end) {
        // Fallback if custom is selected but no dates provided yet
        return { start: todayKey, end: todayKey };
      }
      return {
        start: Number(custom.start.replace(/-/g, '')),
        end: Number(custom.end.replace(/-/g, '')),
      };
    }
    default:
      // Default is month
      const start = new Date(now.getFullYear(), now.getMonth(), 1);
      return { start: toDateKey(start), end: todayKey };
  }
}
