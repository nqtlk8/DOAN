import React, { useState } from 'react';
import { Search, Filter, Eye, FileText, MoreVertical } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { DataState } from '../../shared/components/DataState/DataState';
import type { components } from '@erp/api-contract';

interface SalesListProps {
  setActiveTab?: (tab: string) => void;
  onRowDoubleClick?: (orderId: string) => void;
}

export const SalesList: React.FC<SalesListProps> = ({ onRowDoubleClick }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const { data: response, isLoading, isError, error, refetch } = useQuery<components['schemas']['SalesInvoiceResponseDto'][]>({
    queryKey: ['salesOrders', statusFilter],
    queryFn: () => ApiService.SalesInvoice.getAll(),
  });

  const orders = response || [];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return 'badge-success';
      case 'DELIVERED':
        return 'badge-success';
      case 'PENDING':
        return 'badge-warning';
      case 'CANCELLED':
        return 'badge-danger';
      case 'DRAFT':
        return 'badge-warning';
      default:
        return 'bg-app text-ink border-line';
    }
  };

  const handleEdit = (order: components['schemas']['SalesInvoiceResponseDto']) => {
    if (onRowDoubleClick && order.id) {
      onRowDoubleClick(order.id);
    }
  };

  return (
    <div className="flex flex-col h-full">
      <div className="px-6 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h2 className="text-2xl font-bold text-ink">Danh sách phiếu</h2>

        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-2.5 top-2 text-ink-subtle" size={16} />
            <input
              type="text"
              data-testid="sales-list-search"
              placeholder="Tìm kiếm phiếu..."
              className="erp-input h-8 pl-8 w-[280px]"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <button data-testid="sales-list-filter" className="btn-secondary h-8 px-3 flex items-center gap-2">
            <Filter size={16} /> Lọc
          </button>
        </div>
      </div>

      <div className="card overflow-hidden mx-6 mb-6">
        <DataState
          isLoading={isLoading}
          isError={isError}
          error={error}
          isEmpty={orders.length === 0}
          onRetry={refetch}
          loadingType="table"
          emptyTitle="Chưa có phiếu bán hàng"
          emptyMessage="Hệ thống chưa ghi nhận phiếu bán hàng nào."
        >
          <table className="erp-table">
            <thead>
              <tr className="bg-app border-b border-line">
                <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Mã Đơn</th>
                <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Khách Hàng</th>
                <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Ngày</th>
                <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle num">Tổng Tiền</th>
                <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Trạng Thái</th>
                <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle text-right">
                  Thao Tác
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {!isLoading && !isError && orders.length > 0 && (
                orders.map((order) => (
                  <tr
                    key={order.id}
                    data-testid="sales-list-row"
                    className="hover:bg-app transition-colors group cursor-pointer"
                    onDoubleClick={() => {
                      if (onRowDoubleClick && order.id) onRowDoubleClick(order.id);
                    }}
                  >
                    <td className="px-4 py-1.5 font-medium text-ink">{order.invoiceCode || '-'}</td>
                    <td className="px-4 py-1.5 text-ink-muted">{order.customerName || '-'}</td>
                    <td className="px-4 py-1.5 text-ink-subtle">{order.createdAt ? new Date(order.createdAt).toLocaleDateString() : '-'}</td>
                    <td className="px-4 py-1.5 font-medium text-ink num">{new Intl.NumberFormat('vi-VN').format(order.totalAmount || 0)}</td>
                    <td className="px-4 py-1.5">
                      <span
                        data-testid="sales-list-status"
                        className={`badge ${getStatusColor(order.status || '')}`}
                      >
                        {order.status === 'CONFIRMED' ? 'Đã Xác Nhận' : order.status === 'DRAFT' ? 'Nháp' : order.status === 'CANCELLED' ? 'Đã Hủy' : order.status === 'PENDING' ? 'Chờ Xử Lý' : order.status === 'DELIVERED' ? 'Đã Giao' : (order.status || 'Chưa rõ')}
                      </span>
                    </td>
                    <td className="px-4 py-1.5 text-right">
                      <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={() => handleEdit(order)}
                          data-testid="sales-list-action-view"
                          className="btn-ghost w-7 px-0"
                          title="Chi Tiết / Sửa"
                        >
                          <Eye size={16} />
                        </button>
                        <button
                          data-testid="sales-list-action-print"
                          className="btn-ghost w-7 px-0"
                          title="Phiếu Giao Hàng"
                        >
                          <FileText size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </DataState>
      </div>
    </div>
  );
};
