import type { AlertType } from '../types/analytics';

/** Chu kỳ tự làm mới KPI và cảnh báo trên Dashboard (M-09). Không chạy khi tab bị ẩn. */
export const DASHBOARD_REFETCH_INTERVAL_MS = 60_000;

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
