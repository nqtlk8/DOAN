import React, { useState } from 'react';
import { Search, Filter, MoreVertical, Eye, FileText } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { DataState } from '../../shared/components/DataState/DataState';

interface PurchaseOrderListProps {
  onRowDoubleClick?: (orderId: string) => void;
}

export const PurchaseOrderList: React.FC<PurchaseOrderListProps> = ({ onRowDoubleClick }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const { data: response, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['purchaseOrders', statusFilter],
    queryFn: () => ApiService.InboundReceipt.getAll(),
  });

  const orders: any[] = response || [];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'PENDING':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'CANCELLED':
        return 'bg-red-50 text-red-700 border-red-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  const handleEdit = (order: any) => {
    if (onRowDoubleClick) {
      onRowDoubleClick(order.id);
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-6 py-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-800">Phiếu Nhập Hàng (PO)</h2>

        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              placeholder="Tìm kiếm phiếu nhập..."
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
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Mã Phiếu</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Nhà Phân Phối</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Ngày Nhập</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Tổng Tiền</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Trạng Thái</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-right">
                Thao Tác
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            <tr>
              <td colSpan={6} className="p-0">
                <DataState
                  isLoading={isLoading}
                  isError={isError}
                  error={error}
                  isEmpty={orders.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có phiếu nhập kho"
                  emptyMessage="Hệ thống chưa có phiếu nhập kho nào."
                />
              </td>
            </tr>
            {!isLoading && !isError && orders.length > 0 && (
              orders.map((order) => (
                <tr
                  key={order.id}
                  className="hover:bg-slate-50/50 transition-colors group cursor-pointer"
                  onDoubleClick={() => {
                    if (onRowDoubleClick) onRowDoubleClick(order.id);
                  }}
                >
                  <td className="px-6 py-4 font-medium text-slate-900">{order.orderNumber || order.id}</td>
                  <td className="px-6 py-4 text-slate-600">{order.distributorName || order.distributorId}</td>
                  <td className="px-6 py-4 text-slate-500">{new Date(order.orderDate).toLocaleDateString()}</td>
                  <td className="px-6 py-4 font-medium text-slate-900">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.totalAmount || 0)}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium border ${getStatusColor(order.status)}`}
                    >
                      {order.status === 'CONFIRMED' ? 'Đã Xác Nhận' : order.status === 'DRAFT' ? 'Nháp' : order.status === 'RECEIVED' ? 'Đã Nhập Kho' : order.status === 'CANCELLED' ? 'Đã Hủy' : order.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => handleEdit(order)}
                        className="flex items-center gap-1 p-1.5 text-teal-600 hover:text-teal-700 hover:bg-teal-50 rounded-lg text-sm font-medium transition-colors"
                        title="Chi Tiết / Sửa"
                      >
                        <Eye size={16} /> <span className="hidden sm:inline">Sửa</span>
                      </button>
                      <button className="p-1.5 text-slate-400 hover:text-slate-900 hover:bg-slate-100 rounded-lg">
                        <MoreVertical size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/30">
        <span className="text-sm text-slate-500">Đang hiển thị {orders.length} kết quả</span>
        <div className="flex gap-1">
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50">
            Prev
          </button>
          <button className="px-3 py-1 bg-teal-600 text-white rounded-md text-sm font-medium">1</button>
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50">
            Next
          </button>
        </div>
      </div>
    </div>
  );
};
