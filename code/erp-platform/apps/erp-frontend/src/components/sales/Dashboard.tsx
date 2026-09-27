import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { TrendingUp, ShoppingCart, DollarSign, Package, Download, AlertCircle, RefreshCw } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { DataState } from '../../shared/components/DataState/DataState';
import { formatCurrency, formatNumber } from '../../shared/utils/format';
import { PERIOD_OPTIONS, PeriodKey, getPeriodRange } from './dashboardPeriod';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell } from 'recharts';

const StatCard = ({ title, value, change, icon: Icon, trend, subtitle }: any) => (
  <div className="relative overflow-hidden bg-white p-5 rounded-lg border border-line-strong shadow-sm hover:shadow-md transition-all duration-300">
    <div className="flex items-center justify-between mb-3">
      <div className={`w-10 h-10 rounded-md flex items-center justify-center ${
        trend === 'up' ? 'bg-green-50 text-green-600' : 
        trend === 'down' ? 'bg-red-50 text-red-600' : 
        'bg-primary-soft text-primary'
      }`}>
        <Icon size={20} />
      </div>
      {change && (
        <span
          className={`text-[12px] font-medium px-2 py-0.5 rounded flex items-center gap-1 ${
            trend === 'up' ? 'text-green-700 bg-green-50' : 
            'text-red-700 bg-red-50'
          }`}
        >
          {trend === 'down' && <AlertCircle size={12} />}
          {change}
        </span>
      )}
    </div>
    <div className="relative z-10">
      <h3 className="text-[13px] font-medium text-ink-muted mb-1">{title}</h3>
      <div className="text-[24px] font-bold text-ink mb-1">{value}</div>
      <p className="text-[12px] text-ink-lighter">{subtitle}</p>
    </div>
  </div>
);

const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-white border border-line p-3 rounded shadow-lg">
        <p className="font-semibold text-ink mb-2">{label}</p>
        {payload.map((p: any, i: number) => (
          <p key={i} className="text-[13px] text-ink" style={{ color: p.color }}>
            {p.name}: {p.name === 'Doanh thu' ? formatCurrency(p.value) : formatNumber(p.value)}
          </p>
        ))}
      </div>
    );
  }
  return null;
};

export const Dashboard: React.FC = () => {
  const [branchId, setBranchId] = useState<number | undefined>(undefined);
  const [period, setPeriod] = useState<PeriodKey>('month');
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');
  
  // Params actually used for fetching
  const [queryParams, setQueryParams] = useState<{ start: number; end: number }>(() => getPeriodRange('month'));

  const { data: branches = [] } = useQuery({ 
    queryKey: ['branches'], 
    queryFn: () => ApiService.Branch.getAll() as any 
  });

  const { data: metrics, isLoading: loading, isError, error, refetch: fetchMetrics, isFetching } = useQuery({
    queryKey: ['dashboardMetrics', branchId, queryParams.start, queryParams.end],
    queryFn: () => ApiService.Analytics.getDashboardMetrics(branchId, queryParams.start, queryParams.end) as any,
  });

  const handlePeriodChange = (newPeriod: PeriodKey) => {
    setPeriod(newPeriod);
    if (newPeriod !== 'custom') {
      setQueryParams(getPeriodRange(newPeriod));
    }
  };

  const handleApplyCustom = () => {
    if (!customStart || !customEnd) return;
    setQueryParams(getPeriodRange('custom', new Date(), { start: customStart, end: customEnd }));
  };

  const handleExport = async () => {
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
    }
  };

  const actions = (
    <div className="flex items-center gap-2 flex-wrap">
      <select 
        data-testid="dashboard-branch"
        value={branchId || ''} 
        onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : undefined)}
        className="h-8 w-[200px] bg-white border border-line-strong text-[13px] text-ink rounded-[4px] px-2 focus:outline-none focus:border-primary"
      >
        <option value="">Tất cả chi nhánh</option>
        {branches.map((b: any) => (<option key={b.id} value={b.id}>{b.name} ({b.branchCode})</option>))}
      </select>
      
      <select 
        data-testid="dashboard-period"
        value={period}
        onChange={(e) => handlePeriodChange(e.target.value as PeriodKey)}
        className="h-8 bg-white border border-line-strong text-[13px] text-ink rounded-[4px] px-2 focus:outline-none focus:border-primary"
      >
        {PERIOD_OPTIONS.map(o => (
          <option key={o.key} value={o.key}>{o.label}</option>
        ))}
      </select>

      {period === 'custom' && (
        <>
          <input 
            type="date" 
            data-testid="dashboard-date-start"
            value={customStart}
            onChange={e => setCustomStart(e.target.value)}
            className="h-8 bg-white border border-line-strong text-[13px] px-2 rounded-[4px] focus:outline-none focus:border-primary"
          />
          <span className="text-ink-muted">-</span>
          <input 
            type="date" 
            data-testid="dashboard-date-end"
            value={customEnd}
            onChange={e => setCustomEnd(e.target.value)}
            className="h-8 bg-white border border-line-strong text-[13px] px-2 rounded-[4px] focus:outline-none focus:border-primary"
          />
          <button 
            data-testid="dashboard-filter-btn"
            onClick={handleApplyCustom}
            className="h-8 px-3 bg-white border border-line-strong text-[13px] font-medium text-ink hover:bg-slate-50 rounded-[4px]"
          >
            Áp dụng
          </button>
        </>
      )}

      <button 
        data-testid="dashboard-export-btn"
        onClick={handleExport}
        className="h-8 flex items-center gap-1 bg-white border border-line-strong text-[13px] font-medium text-ink px-3 rounded-[4px] hover:bg-slate-50"
      >
        <Download size={14} />
        Xuất Excel
      </button>
      <button 
        onClick={() => fetchMetrics()}
        className="h-8 w-8 flex justify-center items-center bg-white border border-line-strong text-ink-subtle rounded-[4px] hover:bg-slate-50"
        title="Làm mới dữ liệu"
      >
        <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
      </button>
    </div>
  );

  return (
    <PageContainer data-testid="dashboard-page">
      <PageHeader 
        title="Tổng quan kinh doanh" 
        subtitle="Doanh thu, lợi nhuận và công nợ theo chi nhánh"
        actions={actions}
      />

      <DataState
        isLoading={loading}
        isError={isError}
        error={error}
        isEmpty={!metrics && !loading && !isError}
        onRetry={fetchMetrics}
        loadingType="spinner"
      >
        {metrics && (
        <div className="space-y-4">
          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
            <div data-testid="metric-revenue">
              <StatCard 
                title="Tổng Doanh Thu" 
                value={formatCurrency(metrics.totalRevenue)} 
                icon={DollarSign} 
                trend="up"
                subtitle="Tổng cộng dồn từ các đơn hàng"
              />
            </div>
            <StatCard 
              title="Lợi Nhuận Gộp" 
              value={formatCurrency(metrics.grossProfit)} 
              icon={TrendingUp}
              trend="up" 
              subtitle="Doanh thu trừ giá vốn"
            />
            <StatCard 
              title="Vòng Quay Tồn Kho" 
              value={metrics.inventoryTurnoverRatio?.toFixed(2) || '0.00'} 
              icon={Package} 
              trend="neutral"
              subtitle="Hệ số quay vòng vốn kho"
            />
            <div data-testid="metric-debt">
              <StatCard 
                title="Công Nợ Quá Hạn" 
                value={formatCurrency(metrics.totalOverdueDebt)} 
                icon={ShoppingCart} 
                trend={metrics.totalOverdueDebt > 0 ? "down" : "neutral"}
                change={metrics.totalOverdueDebt > 0 ? "Cần thu hồi" : ""}
                subtitle="Tổng nợ phải thu từ khách hàng"
              />
            </div>
          </div>

          {/* Charts Section */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div className="lg:col-span-2 bg-white rounded-lg border border-line shadow-sm p-5">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-[15px] font-semibold text-ink">Top Sản Phẩm Bán Chạy Nhất</h3>
              </div>
              <div className="h-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={metrics.topSellingProducts || []}
                    margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                    <XAxis 
                      dataKey="productName" 
                      axisLine={false} 
                      tickLine={false} 
                      tick={{ fill: '#64748b', fontSize: 12 }} 
                      dy={10}
                    />
                    <YAxis 
                      yAxisId="left"
                      axisLine={false} 
                      tickLine={false} 
                      tick={{ fill: '#64748b', fontSize: 12 }}
                      tickFormatter={(value) => `${value / 1000000}M`}
                    />
                    <YAxis 
                      yAxisId="right" 
                      orientation="right" 
                      axisLine={false} 
                      tickLine={false}
                      tick={{ fill: '#64748b', fontSize: 12 }}
                    />
                    <Tooltip content={<CustomTooltip />} />
                    <Legend wrapperStyle={{ paddingTop: '20px' }} />
                    <Bar yAxisId="left" dataKey="revenue" name="Doanh thu" radius={[4, 4, 0, 0]}>
                      {(metrics.topSellingProducts || []).map((entry: any, index: number) => (
                        <Cell key={`cell-${index}`} fill={['#0F62FE', '#3b82f6', '#6366f1', '#8b5cf6', '#ec4899'][index % 5]} />
                      ))}
                    </Bar>
                    <Bar yAxisId="right" dataKey="quantitySold" fill="#cbd5e1" name="Số lượng (SP)" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Top Products List */}
            <div className="bg-white rounded-lg border border-line shadow-sm p-5 flex flex-col h-[395px]">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-[15px] font-semibold text-ink">Chi Tiết Sản Phẩm</h3>
                <span className="text-[11px] font-medium bg-primary-soft text-primary px-2 py-0.5 rounded-full">
                  Top {metrics.topSellingProducts?.length || 0}
                </span>
              </div>
              <div className="overflow-y-auto pr-1 flex-grow custom-scrollbar">
                <div className="space-y-2">
                  {metrics.topSellingProducts?.map((product: any, i: number) => (
                    <div key={i} data-testid="top-product-row" className="flex items-center p-2 hover:bg-slate-50 rounded-[6px] transition-colors border border-transparent">
                      <div className="w-8 h-8 rounded-full bg-primary-soft text-primary flex items-center justify-center font-bold text-[13px] mr-3 shrink-0">
                        #{i + 1}
                      </div>
                      <div className="flex-grow min-w-0">
                        <p className="text-[13px] font-semibold text-ink truncate">{product.productName}</p>
                        <p className="text-[11px] text-ink-muted mt-0.5">{formatNumber(product.quantitySold)} sản phẩm</p>
                      </div>
                      <div className="text-right shrink-0 ml-3">
                        <p className="text-[13px] font-semibold text-primary-dark">{formatCurrency(product.revenue)}</p>
                      </div>
                    </div>
                  ))}
                  {(!metrics.topSellingProducts || metrics.topSellingProducts.length === 0) && (
                    <div className="flex flex-col items-center justify-center h-full text-ink-muted py-10">
                      <Package size={40} className="mb-3 opacity-20" />
                      <p className="text-[13px]">Chưa có dữ liệu bán hàng</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
        )}
      </DataState>
    </PageContainer>
  );
};
