import React from 'react';
import { AlertTriangle, CheckCircle2, Loader2 } from 'lucide-react';
import type { StockAlertSummaryDto } from '../../types/analytics';
import { STOCK_ALERT_CONFIG } from '../../config/dashboard';
import { formatNumber } from '../../shared/utils/format';

interface StockAlertPanelProps {
    summary?: StockAlertSummaryDto;
    isLoading: boolean;
    isError: boolean;
    onRetry: () => void;
}

/** Class hiển thị theo mức cảnh báo — dùng token màu của design system (index.css), không hard-code màu. */
const TONE_CLASS = {
    danger: { badge: 'badge-danger', text: 'text-danger' },
    warning: { badge: 'badge-warning', text: 'text-warning' },
} as const;

/** Tồn kho có thể lẻ (numeric(18,3)) nên cho tối đa 3 chữ số thập phân. */
const formatQty = (v: number) => formatNumber(v, 3);

/**
 * Panel cảnh báo tồn kho trên Dashboard (component "dumb": chỉ nhận props, không gọi API).
 *
 * Cảnh báo tính theo tồn hiện tại nên không phụ thuộc kỳ báo cáo. Lỗi tải cảnh báo chỉ hiển thị trong panel
 * này, không ảnh hưởng khối KPI (hai query độc lập — xem sprint FD-4).
 */
export const StockAlertPanel: React.FC<StockAlertPanelProps> = ({ summary, isLoading, isError, onRetry }) => {
    if (isLoading) {
        return (
            <div className="card p-4 flex items-center gap-2 text-[13px] text-ink-muted" data-testid="stock-alert-panel">
                <Loader2 size={14} className="animate-spin" />
                <p>Đang tải dữ liệu cảnh báo...</p>
            </div>
        );
    }

    if (isError) {
        return (
            <div className="card p-4 flex items-center justify-between gap-3 bg-danger-soft" data-testid="stock-alert-panel">
                <p className="text-[13px] text-danger">Lỗi khi tải dữ liệu cảnh báo tồn kho.</p>
                <button type="button" onClick={onRetry} className="btn btn-secondary h-8">
                    Thử lại
                </button>
            </div>
        );
    }

    const alerts = summary?.alerts ?? [];
    const hasAlerts = alerts.length > 0;

    return (
        <div className="card overflow-hidden" data-testid="stock-alert-panel">
            <div className="px-4 h-12 flex items-center justify-between gap-3 border-b border-line">
                <h3 className="text-[15px] font-semibold text-ink flex items-center gap-2">
                    <AlertTriangle size={16} className={hasAlerts ? 'text-warning' : 'text-ink-subtle'} />
                    Cảnh báo tồn kho
                </h3>
                <span className="text-[12px] text-ink-subtle">Cảnh báo theo tồn hiện tại, không phụ thuộc kỳ báo cáo</span>
            </div>

            {hasAlerts && summary ? (
                <>
                    <p className="px-4 py-2 text-[13px] font-medium text-ink">
                        {summary.negativeCount} sản phẩm tồn âm &middot; {summary.lowStockCount} sản phẩm dưới ngưỡng
                    </p>
                    <div className="overflow-x-auto">
                        <table className="erp-table">
                            <thead>
                                <tr>
                                    <th className="w-24">Mức</th>
                                    <th>Chi nhánh</th>
                                    <th className="w-28">Mã SP</th>
                                    <th>Tên SP</th>
                                    <th className="num w-28">Tồn hiện tại</th>
                                    <th className="num w-24">Ngưỡng</th>
                                </tr>
                            </thead>
                            <tbody>
                                {alerts.map((alert) => {
                                    const config = STOCK_ALERT_CONFIG[alert.alertType];
                                    const tone = TONE_CLASS[config.tone];
                                    return (
                                        <tr key={`${alert.productId}-${alert.branchId}`} data-testid="stock-alert-row">
                                            <td>
                                                <span className={tone.badge}>{config.label}</span>
                                            </td>
                                            <td>{alert.branchName}</td>
                                            <td>{alert.productCode}</td>
                                            <td className="truncate max-w-[260px]" title={alert.productName}>
                                                {alert.productName}
                                            </td>
                                            <td className={`num font-medium ${tone.text}`}>{formatQty(alert.currentQuantity)}</td>
                                            <td className="num text-ink-muted">
                                                {alert.minQuantityThreshold === null || alert.minQuantityThreshold === undefined
                                                    ? '—'
                                                    : formatQty(alert.minQuantityThreshold)}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </>
            ) : (
                <p className="px-4 py-3 text-[13px] font-medium text-success flex items-center gap-2">
                    <CheckCircle2 size={14} />
                    Không có cảnh báo tồn kho
                </p>
            )}
        </div>
    );
};
