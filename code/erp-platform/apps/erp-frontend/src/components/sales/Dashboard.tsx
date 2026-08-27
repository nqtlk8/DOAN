import React, { useEffect, useState } from 'react';
import { TrendingUp, ShoppingCart, DollarSign, Package, Download, AlertCircle, RefreshCw } from 'lucide-react';
import { getDashboardMetrics, exportDashboardExcel, DashboardMetricsDto } from '../../services/DashboardApi';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell } from 'recharts';

const formatCurrency = (value: number | undefined) => {
  if (value === undefined) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const StatCard = ({ title, value, change, icon: Icon, trend, subtitle }: any) => (
  <div className="relative overflow-hidden bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-lg transition-all duration-300 group">
    <div className="absolute top-0 right-0 p-4 opacity-5 transform translate-x-2 -translate-y-2 group-hover:scale-110 transition-transform duration-300">
      <Icon size={100} />
    </div>
    <div className="flex items-center justify-between mb-4 relative z-10">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
        trend === 'up' ? 'bg-emerald-100 text-emerald-600' : 
        trend === 'down' ? 'bg-rose-100 text-rose-600' : 
        'bg-indigo-100 text-indigo-600'
      }`}>
        <Icon size={24} />
      </div>
      {change && (
        <span
          className={`text-xs font-semibold px-2.5 py-1 rounded-full flex items-center gap-1 ${
            trend === 'up' ? 'text-emerald-700 bg-emerald-50 border border-emerald-200' : 
            'text-rose-700 bg-rose-50 border border-rose-200'
          }`}
        >
          {trend === 'down' && <AlertCircle size={12} />}
          {change}
        </span>
      )}
    </div>
    <div className="relative z-10">
      <h3 className="text-slate-500 text-sm font-medium tracking-wide uppercase">{title}</h3>
      <p className="text-2xl font-extrabold text-slate-800 mt-1">{value}</p>
      {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
    </div>
  </div>
);

const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-white p-4 rounded-xl shadow-xl border border-slate-100">
        <p className="font-bold text-slate-800 mb-2">{label}</p>
        <p className="text-emerald-600 font-semibold text-sm">
          Doanh thu: {formatCurrency(payload[0].value)}
        </p>
        <p className="text-indigo-600 font-semibold text-sm">
          Số lượng: {payload[1].value} SP
        </p>
      </div>
    );
  }
  return null;
};

export const Dashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<DashboardMetricsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [branchId, setBranchId] = useState<number | undefined>(undefined);
  const [period, setPeriod] = useState<string>('all'); 

  const getDatesFromPeriod = () => {
    const now = new Date();
    const today = parseInt(now.toISOString().slice(0,10).replace(/-/g, ''));
    let start = 20200101;
    let end = 20301231;
    if (period === 'month') {
      start = parseInt(now.toISOString().slice(0,8).replace(/-/g, '') + '01');
      end = today;
    } else if (period === 'year') {
      start = parseInt(now.getFullYear().toString() + '0101');
      end = today;
    }
    return { start, end };
  };

  const fetchMetrics = async () => {
    setLoading(true);
    try {
      const { start, end } = getDatesFromPeriod();
      const data = await getDashboardMetrics(branchId, start, end);
      setMetrics(data);
    } catch (error) {
      console.error("Failed to load metrics", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMetrics();
  }, [branchId, period]);

  const handleExport = () => {
    const { start, end } = getDatesFromPeriod();
    exportDashboardExcel(branchId, start, end);
  };

  return (
    <div className="space-y-8 bg-slate-50/50 min-h-screen p-2">
      {/* Header Section */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Tổng Quan Kinh Doanh</h1>
          <p className="text-sm text-slate-500 mt-1">Phân tích hiệu suất doanh thu và tồn kho của cửa hàng</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <select 
            value={branchId || ''} 
            onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : undefined)}
            className="bg-white border border-slate-200 text-sm font-medium text-slate-700 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm cursor-pointer"
          >
            <option value="">Tất cả chi nhánh</option>
            <option value="1">Chi nhánh Trung Tâm (1)</option>
            <option value="2">Chi nhánh Quận 2 (2)</option>
          </select>
          <select 
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="bg-white border border-slate-200 text-sm font-medium text-slate-700 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm cursor-pointer"
          >
            <option value="all">Toàn thời gian</option>
            <option value="month">Tháng này</option>
            <option value="year">Năm nay</option>
          </select>
          <button 
            onClick={handleExport}
            className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2.5 rounded-lg hover:bg-indigo-700 transition-all shadow-sm shadow-indigo-200 text-sm font-semibold active:scale-95"
          >
            <Download size={18} />
            Xuất Excel
          </button>
          <button 
            onClick={fetchMetrics}
            className="p-2.5 bg-white border border-slate-200 text-slate-600 rounded-lg hover:bg-slate-50 transition-colors shadow-sm"
            title="Làm mới dữ liệu"
          >
            <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Main Content */}
      {loading || !metrics ? (
        <div className="flex flex-col justify-center items-center h-96 space-y-4">
           <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-100 border-t-indigo-600"></div>
           <p className="text-slate-500 font-medium animate-pulse">Đang tải dữ liệu...</p>
        </div>
      ) : (
        <div className="space-y-8 animate-in fade-in duration-500">
          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
            <StatCard 
              title="Tổng Doanh Thu" 
              value={formatCurrency(metrics.totalRevenue)} 
              icon={DollarSign} 
              trend="up"
              subtitle="Tổng cộng dồn từ các đơn hàng"
            />
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
            <StatCard 
              title="Công Nợ Quá Hạn" 
              value={formatCurrency(metrics.totalOverdueDebt)} 
              icon={ShoppingCart} 
              trend={metrics.totalOverdueDebt > 0 ? "down" : "neutral"}
              change={metrics.totalOverdueDebt > 0 ? "Cần thu hồi" : ""}
              subtitle="Tổng nợ phải thu từ khách hàng"
            />
          </div>

          {/* Charts Section */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-lg font-bold text-slate-800">Top Sản Phẩm Bán Chạy Nhất</h3>
              </div>
              <div className="h-[350px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={metrics.topSellingProducts}
                    margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
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
                    <Bar yAxisId="left" dataKey="revenue" name="Doanh thu" radius={[6, 6, 0, 0]}>
                      {metrics.topSellingProducts?.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={['#10b981', '#3b82f6', '#6366f1', '#8b5cf6', '#ec4899'][index % 5]} />
                      ))}
                    </Bar>
                    <Bar yAxisId="right" dataKey="quantitySold" fill="#cbd5e1" name="Số lượng (SP)" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Top Products List */}
            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col h-[450px]">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-lg font-bold text-slate-800">Chi Tiết Sản Phẩm</h3>
                <span className="text-xs font-semibold bg-indigo-50 text-indigo-700 px-2.5 py-1 rounded-full">Top {metrics.topSellingProducts?.length || 0}</span>
              </div>
              <div className="overflow-y-auto pr-2 flex-grow custom-scrollbar">
                <div className="space-y-4">
                  {metrics.topSellingProducts?.map((product, i) => (
                    <div key={i} className="flex items-center p-3 hover:bg-slate-50 rounded-xl transition-colors border border-transparent hover:border-slate-100">
                      <div className="w-10 h-10 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center font-bold text-sm mr-4 shrink-0">
                        #{i + 1}
                      </div>
                      <div className="flex-grow min-w-0">
                        <p className="text-sm font-bold text-slate-800 truncate">{product.productName}</p>
                        <p className="text-xs text-slate-500 mt-0.5">{product.quantitySold} sản phẩm</p>
                      </div>
                      <div className="text-right shrink-0 ml-4">
                        <p className="text-sm font-bold text-emerald-600">{formatCurrency(product.revenue)}</p>
                      </div>
                    </div>
                  ))}
                  {(!metrics.topSellingProducts || metrics.topSellingProducts.length === 0) && (
                    <div className="flex flex-col items-center justify-center h-full text-slate-400 py-10">
                      <Package size={48} className="mb-4 opacity-20" />
                      <p className="text-sm">Chưa có dữ liệu bán hàng</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

