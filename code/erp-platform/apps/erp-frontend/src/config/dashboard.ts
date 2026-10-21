import type { AlertType } from '../types/analytics';

/** Chu kỳ tự làm mới KPI và cảnh báo trên Dashboard (M-09). Không chạy khi tab bị ẩn. */
export const DASHBOARD_REFETCH_INTERVAL_MS = 60_000;

/** Số dòng bảng "Chi tiết top sản phẩm"; 0 = tất cả (backend giới hạn an toàn 1000). Biểu đồ luôn top 10. */
export const TOP_LIMIT_OPTIONS: { value: number; label: string }[] = [
  { value: 10, label: 'Top 10' },
  { value: 20, label: 'Top 20' },
  { value: 50, label: 'Top 50' },
  { value: 100, label: 'Top 100' },
  { value: 0, label: 'Tất cả' },
];
export const DEFAULT_TOP_LIMIT = 10;
export const CHART_TOP_COUNT = 10;

export const STOCK_ALERT_CONFIG: Record<AlertType, { label: string; tone: 'danger' | 'warning' }> = {
    NEGATIVE_STOCK: {
        label: 'Tồn âm',
        tone: 'danger'
    },
    LOW_STOCK: {
        label: 'Sắp hết',
        tone: 'warning'
    }
};
