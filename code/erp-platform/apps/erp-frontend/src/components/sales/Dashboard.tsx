import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useDashboardMetrics } from '../../hooks/useDashboardMetrics';
import { useStockAlerts } from '../../hooks/useStockAlerts';
import { StockAlertPanel } from './StockAlertPanel';
import toast from 'react-hot-toast';
import { DollarSign, Download, Loader2, Package, PackageOpen, RefreshCw, TrendingUp, Wallet } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ApiService } from '../../api/ApiService';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { DataState } from '../../shared/components/DataState/DataState';
import { formatCompact, formatCurrency, formatNumber } from '../../shared/utils/format';
import { PERIOD_OPTIONS, type PeriodKey, getPeriodRange } from './dashboardPeriod';
import type { DashboardMetricsDto } from '../../types/analytics';

type TopProduct = DashboardMetricsDto['topSellingProducts'][number];

interface StatCardProps {
  title: string;
  value: string;
  subtitle: string;
  icon: React.ElementType;
  tone?: 'primary' | 'danger';
  badge?: string;
  testId: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, subtitle, icon: Icon, tone = 'primary', badge, testId }) => (
  <div data-testid={testId} className="card p-4 flex flex-col gap-2">
    <div className="flex items-center justify-between gap-2">
      <h3 className="text-[12px] font-semibold uppercase tracking-wide text-ink-muted">{title}</h3>
      <span
        className={`w-8 h-8 rounded-md flex items-center justify-center shrink-0 ${
          tone === 'danger' ? 'bg-danger-soft text-danger' : 'bg-primary-soft text-primary'
        }`}
      >
        <Icon size={16} />
      </span>
    </div>
    <div className={`text-[22px] font-semibold tabular-nums leading-tight ${tone === 'danger' ? 'text-danger' : 'text-ink'}`}>
      {value}
    </div>
    <div className="flex items-center gap-2 text-[12px] text-ink-subtle">
      {badge && <span className="badge-danger">{badge}</span>}
      <span>{subtitle}</span>
    </div>
  </div>
);

const ChartTooltip = ({ active, payload }: { active?: boolean; payload?: { payload: TopProduct }[] }) => {
  if (!active || !payload?.length) return null;
  const p: TopProduct = payload[0].payload;
  return (
    <div className="card shadow-lg px-3 py-2 text-[13px]">
      <p className="font-semibold text-ink mb-1">{p.productName}</p>
      <p className="text-ink">Doanh thu: {formatCurrency(p.revenue)}</p>
      <p className="text-ink-muted">Số lượng: {formatNumber(p.quantitySold)} SP</p>
    </div>
  );
};

const truncate = (s: string, n = 22) => (s && s.length > n ? `${s.slice(0, n - 1)}…` : s);

const EmptyTop = () => (
  <div className="flex flex-col items-center justify-center py-10 text-ink-subtle">
    <PackageOpen size={32} className="mb-2" />
    <p className="text-[13px]">Chưa có dữ liệu bán hàng trong kỳ này</p>
  </div>
);

export const Dashboard: React.FC = () => {
  const [branchId, setBranchId] = useState<number | undefined>(undefined);
  const [period, setPeriod] = useState<PeriodKey>('month');
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');
  const [exporting, setExporting] = useState(false);

  // Tham số thực sự dùng để gọi API (kỳ "Tùy chọn" chỉ áp dụng khi bấm "Áp dụng").
  const [queryParams, setQueryParams] = useState<{ start: number; end: number }>(() => getPeriodRange('month'));

  const { data: branches = [] } = useQuery<{ id?: number; name?: string; branchCode?: string }[]>({
    queryKey: ['branches'],
    queryFn: () => ApiService.Branch.getAll(),
  });

  const {
    data: metrics,
    isLoading,
    isError,
    error,
    refetch: refetchMetrics,
    isFetching,
    dataUpdatedAt: metricsUpdatedAt,
  } = useDashboardMetrics(branchId, queryParams.start, queryParams.end);

  const {
    data: alertsSummary,
    isLoading: isAlertsLoading,
    isError: isAlertsError,
    refetch: refetchAlerts,
    dataUpdatedAt: alertsUpdatedAt,
  } = useStockAlerts(branchId);

  const refetchAll = () => {
    refetchMetrics();
    refetchAlerts();
  };
  
  const lastUpdated = Math.max(metricsUpdatedAt || 0, alertsUpdatedAt || 0);


  const handlePeriodChange = (next: PeriodKey) => {
    setPeriod(next);
    if (next !== 'custom') setQueryParams(getPeriodRange(next));
  };

  const handleApplyCustom = () => {
    if (!customStart || !customEnd) {
      toast.error('Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc');
      return;
    }
    if (customStart > customEnd) {
      toast.error('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc');
      return;
    }
    setQueryParams(getPeriodRange('custom', new Date(), { start: customStart, end: customEnd }));
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      const blob = await ApiService.Analytics.exportExcel(branchId, queryParams.start, queryParams.end);
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `bao-cao-kinh-doanh-${queryParams.start}-${queryParams.end}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Export failed', err);
      toast.error('Xuất Excel thất bại');
    } finally {
      setExporting(false);
    }
  };

  const selectClass =
    'h-8 bg-surface border border-line-strong text-[13px] text-ink rounded-[4px] px-2 focus:outline-none focus:border-primary';

  const actions = (
    <>
      <select
        data-testid="dashboard-branch"
        aria-label="Chi nhánh"
        value={branchId ?? ''}
        onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : undefined)}
        className={`${selectClass} w-[200px]`}
      >
        <option value="">Tất cả chi nhánh</option>
        {branches.map((b) => (
          <option key={b.id} value={b.id}>
            {b.name}
            {b.branchCode ? ` (${b.branchCode})` : ''}
          </option>
        ))}
      </select>

      <select
        data-testid="dashboard-period"
        aria-label="Kỳ báo cáo"
        value={period}
        onChange={(e) => handlePeriodChange(e.target.value as PeriodKey)}
        className={selectClass}
      >
        {PERIOD_OPTIONS.map((o) => (
          <option key={o.key} value={o.key}>
            {o.label}
          </option>
        ))}
      </select>

      {period === 'custom' && (
        <>
          <input
            type="date"
            aria-label="Từ ngày"
            data-testid="dashboard-date-start"
            value={customStart}
            onChange={(e) => setCustomStart(e.target.value)}
            className={selectClass}
          />
          <span className="text-ink-muted">–</span>
          <input
            type="date"
            aria-label="Đến ngày"
            data-testid="dashboard-date-end"
            value={customEnd}
            onChange={(e) => setCustomEnd(e.target.value)}
            className={selectClass}
          />
          <button type="button" data-testid="dashboard-filter-btn" onClick={handleApplyCustom} className="btn btn-secondary h-8">
            Áp dụng
          </button>
        </>
      )}

      {lastUpdated > 0 && (
        <span className="text-xs text-gray-500 mr-2 flex items-center">
          Cập nhật lúc {new Date(lastUpdated).toLocaleTimeString('vi-VN')}
        </span>
      )}
      <button
        type="button"
        onClick={refetchAll}
        aria-label="Làm mới"
        title="Làm mới dữ liệu"
        className="btn btn-secondary h-8 w-8 px-0"
      >
        <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
      </button>
      <button
        type="button"
        data-testid="dashboard-export-btn"
        onClick={handleExport}
        disabled={exporting}
        className="btn btn-primary h-8"
      >
        {exporting ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
        Xuất Excel
      </button>
    </>
  );

  const top: TopProduct[] = metrics?.topSellingProducts ?? [];
  const chartHeight = Math.max(240, top.length * 44);

  return (
    <PageContainer data-testid="dashboard-page">
      <PageHeader
        title="Tổng quan kinh doanh"
        subtitle="Doanh thu, lợi nhuận và công nợ theo chi nhánh"
        actions={actions}
      />

      <StockAlertPanel summary={alertsSummary} isLoading={isAlertsLoading} isError={isAlertsError} onRetry={refetchAlerts} />

      <DataState
        isLoading={isLoading}
        isError={isError}
        error={error as Error | null}
        isEmpty={!metrics && !isLoading && !isError}
        onRetry={refetchAll}
        loadingType="spinner"
      >
        {metrics && (
          <div className="space-y-4">
            {/* 4 thẻ KPI */}
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
              <StatCard
                testId="metric-revenue"
                title="Tổng doanh thu"
                value={formatCurrency(metrics.totalRevenue)}
                icon={DollarSign}
                subtitle="Tổng tiền các hóa đơn trong kỳ"
              />
              <StatCard
                testId="metric-profit"
                title="Lợi nhuận gộp"
                value={formatCurrency(metrics.grossProfit)}
                icon={TrendingUp}
                subtitle="Doanh thu trừ giá vốn"
              />
              <StatCard
                testId="metric-turnover"
                title="Vòng quay tồn kho"
                value={formatNumber(metrics.inventoryTurnoverRatio ?? 0, 2)}
                icon={Package}
                subtitle="Hệ số quay vòng vốn kho"
              />
              <StatCard
                testId="metric-debt"
                title="Tổng công nợ phải thu"
                value={formatCurrency(metrics.totalReceivableDebt)}
                icon={Wallet}
                tone={metrics.totalReceivableDebt > 0 ? 'danger' : 'primary'}
                badge={metrics.totalReceivableDebt > 0 ? 'Cần thu hồi' : undefined}
                subtitle="Tổng nợ khách hàng theo chi nhánh"
              />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
              {/* Biểu đồ cột ngang: một chuỗi (doanh thu), một màu, một trục */}
              <div className="lg:col-span-7 card p-4" data-testid="top-products-chart">
                <h3 className="text-[15px] font-semibold text-ink mb-3">Top sản phẩm theo doanh thu</h3>
                {top.length === 0 ? (
                  <EmptyTop />
                ) : (
                  <div style={{ height: chartHeight }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={top} layout="vertical" margin={{ top: 0, right: 24, left: 0, bottom: 0 }}>
                        <CartesianGrid horizontal={false} stroke="var(--color-line)" />
                        <XAxis
                          type="number"
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'var(--color-ink-subtle)', fontSize: 12 }}
                          tickFormatter={(v) => formatCompact(v)}
                        />
                        <YAxis
                          type="category"
                          dataKey="productName"
                          width={160}
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'var(--color-ink)', fontSize: 12 }}
                          tickFormatter={(v) => truncate(String(v))}
                        />
                        <Tooltip content={<ChartTooltip />} cursor={{ fill: 'var(--color-primary-soft)' }} />
                        <Bar dataKey="revenue" name="Doanh thu" fill="var(--color-primary)" radius={[0, 4, 4, 0]} barSize={18} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                )}
              </div>

              {/* Bảng chi tiết — cũng là "table view" cho người không đọc được biểu đồ */}
              <div className="lg:col-span-5 card overflow-hidden flex flex-col" data-testid="top-products-table">
                <div className="px-4 h-12 flex items-center justify-between border-b border-line">
                  <h3 className="text-[15px] font-semibold text-ink">Chi tiết top sản phẩm</h3>
                  <span className="badge-neutral">Top {top.length}</span>
                </div>
                {top.length === 0 ? (
                  <EmptyTop />
                ) : (
                  <div className="overflow-auto">
                    <table className="erp-table">
                      <thead>
                        <tr>
                          <th className="w-10 text-center">#</th>
                          <th>Sản phẩm</th>
                          <th className="num w-24">SL bán</th>
                          <th className="num w-36">Doanh thu</th>
                        </tr>
                      </thead>
                      <tbody>
                        {top.map((p, i) => (
                          <tr key={p.productId ?? i} data-testid="top-product-row">
                            <td className="text-center text-ink-subtle">{i + 1}</td>
                            <td className="truncate max-w-[220px]" title={p.productName}>
                              {p.productName}
                            </td>
                            <td className="num">{formatNumber(p.quantitySold)}</td>
                            <td className="num font-medium">{formatNumber(p.revenue)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Bảng Sản phẩm bán chậm */}
              <div className="lg:col-span-12 card overflow-hidden flex flex-col" data-testid="slow-products-table">
                <div className="px-4 h-12 flex items-center justify-between border-b border-line">
                  <h3 className="text-[15px] font-semibold text-ink">Sản phẩm bán chậm</h3>
                  {metrics.slowMovingProducts?.length > 0 && (
                    <span className="badge-neutral">{metrics.slowMovingProducts.length}</span>
                  )}
                </div>
                {(!metrics.slowMovingProducts || metrics.slowMovingProducts.length === 0) ? (
                  <div className="p-8 text-center text-ink-subtle flex flex-col items-center justify-center gap-2">
                    <PackageOpen size={32} className="text-line-dark" />
                    <p>Không có sản phẩm tồn kho mà không bán trong kỳ</p>
                  </div>
                ) : (
                  <div className="overflow-auto max-h-[300px]">
                    <table className="erp-table">
                      <thead className="sticky top-0 bg-white">
                        <tr>
                          <th className="w-10 text-center">#</th>
                          <th className="w-32">Mã SP</th>
                          <th>Tên SP</th>
                          <th className="num w-24">Tồn hiện tại</th>
                        </tr>
                      </thead>
                      <tbody>
                        {metrics.slowMovingProducts.map((p, i) => (
                          <tr key={p.productId ?? i} data-testid="slow-product-row">
                            <td className="text-center text-ink-subtle">{i + 1}</td>
                            <td>{p.productCode}</td>
                            <td className="truncate max-w-[220px]" title={p.productName}>
                              {p.productName}
                            </td>
                            <td className="num font-medium text-red-600">{formatNumber(p.currentStock)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </DataState>
    </PageContainer>
  );
};
