import React from 'react';
import { Search, Filter, MoreVertical, Eye, FileText } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import type { OrderListItem } from '../../types/sales';
import { useQuery } from '@tanstack/react-query';

interface SalesListProps {
  setActiveTab?: (tab: string) => void;
  onRowDoubleClick?: (orderId: string) => void;
}

export const SalesList: React.FC<SalesListProps> = ({ onRowDoubleClick }) => {
  const { data: response, isLoading } = useQuery({
    queryKey: ['salesOrders'],
    queryFn: () => ApiService.SalesInvoice.getAll(),
  });

  const orders: OrderListItem[] = response || [];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-primary/10 text-blue-700 border-blue-200';
      case 'DELIVERED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'PENDING':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'CANCELLED':
        return 'bg-red-50 text-red-700 border-red-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  const handleEdit = (order: OrderListItem) => {
    if (onRowDoubleClick) {
      onRowDoubleClick(order.orderId);
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-6 py-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-800">Đơn Bán Hàng</h2>

        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              placeholder="Tìm kiếm đơn hàng..."
              className="pl-9 pr-4 py-2 text-sm bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500/50 w-full sm:w-64"
            />
          </div>
          <button className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors">
            <Filter size={16} /> Lọc
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50/50 border-b border-slate-100">
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Mã Đơn</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Khách Hàng</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Ngày</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Tổng Tiền</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Trạng Thái</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-right">
                Thao Tác
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-500">
                  Đang tải đơn hàng...
                </td>
              </tr>
            ) : orders.length > 0 ? (
              orders.map((order) => (
                <tr
                  key={order.orderId}
                  className="hover:bg-slate-50/50 transition-colors group cursor-pointer"
                  onDoubleClick={() => {
                    if (onRowDoubleClick) onRowDoubleClick(order.orderId);
                  }}
                >
                  <td className="px-6 py-4 font-medium text-slate-900">{order.code || order.orderId}</td>
                  <td className="px-6 py-4 text-slate-600">{order.customerName || order.customerId}</td>
                  <td className="px-6 py-4 text-slate-500">{new Date(order.createdAt).toLocaleDateString()}</td>
                  <td className="px-6 py-4 font-medium text-slate-900">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.totalAmount)}</td>
                  <td className="px-6 py-4">
                    <span
                      className={`inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium border ${getStatusColor(order.status)}`}
                    >
                      {order.status === 'CONFIRMED' ? 'Đã Xác Nhận' : order.status === 'DRAFT' ? 'Nháp' : order.status === 'CANCELLED' ? 'Đã Hủy' : order.status === 'PENDING' ? 'Chờ Xử Lý' : order.status === 'DELIVERED' ? 'Đã Giao' : order.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => handleEdit(order)}
                        className="flex items-center gap-1 p-1.5 text-teal-600 hover:text-teal-700 hover:bg-teal-50 rounded-lg text-sm font-medium transition-colors"
                        title="Chi Tiết / Sửa"
                      >
                        <Eye size={16} /> <span className="hidden sm:inline">Chi Tiết / Sửa</span>
                      </button>
                      <button
                        className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg"
                        title="Phiếu Giao Hàng"
                      >
                        <FileText size={18} />
                      </button>
                      <button className="p-1.5 text-slate-400 hover:text-slate-900 hover:bg-slate-100 rounded-lg">
                        <MoreVertical size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-500">
                  Không có đơn hàng nào.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/30">
        <span className="text-sm text-slate-500">Showing 1 to 10 of 45 results</span>
        <div className="flex gap-1">
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50">
            Prev
          </button>
          <button className="px-3 py-1 bg-teal-600 text-white rounded-md text-sm font-medium">1</button>
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50">
            2
          </button>
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50">
            3
          </button>
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50">
            Next
          </button>
        </div>
      </div>
    </div>
  );
};
