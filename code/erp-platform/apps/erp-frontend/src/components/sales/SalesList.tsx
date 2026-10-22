import React, { useState } from 'react';
import { Eye, Search } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import type { components } from '@erp/api-contract';
import { ApiService } from '../../api/ApiService';
import { ListTable } from '../../shared/components/DataState/ListTable';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatDateTime, formatNumber, normalizeSearch } from '../../shared/utils/format';

type Invoice = components['schemas']['SalesInvoiceResponseDto'];

interface SalesListProps {
  /** Giữ để tương thích chỗ gọi cũ, không còn dùng. */
  setActiveTab?: (tab: string) => void;
  onRowDoubleClick?: (orderId: string) => void;
}

export const SalesList: React.FC<SalesListProps> = ({ onRowDoubleClick }) => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: orders = [], isLoading, isError, error, refetch } = useQuery<Invoice[]>({
    queryKey: ['salesInvoices'],
    queryFn: () => ApiService.SalesInvoice.getAll(),
  });

  const term = normalizeSearch(searchTerm);
  const filtered = orders.filter(
    (o) => normalizeSearch(o.invoiceCode).includes(term) || normalizeSearch(o.customerName).includes(term),
  );

  const open = (o: Invoice) => {
    if (onRowDoubleClick && o.id) onRowDoubleClick(o.id);
  };

  return (
    <div className="flex flex-col h-full gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-[18px] font-semibold text-ink">Danh sách phiếu bán hàng</h2>
        <div className="relative">
          <Search className="absolute left-2.5 top-2 text-ink-subtle pointer-events-none" size={16} />
          <input
            type="text"
            data-testid="sales-list-search"
            placeholder="Tìm theo số phiếu, khách hàng…"
            className="erp-input h-8 pl-8 w-[280px]"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      <ListTable
        colCount={6}
        isLoading={isLoading}
        isError={isError}
        error={error as Error | null}
        onRetry={refetch}
        totalCount={orders.length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có phiếu bán hàng"
        emptyMessage="Hệ thống chưa ghi nhận phiếu bán hàng nào."
        header={
          <tr>
            <th className="w-36">Số phiếu</th>
            <th>Khách hàng</th>
            <th className="w-40">Ngày tạo</th>
            <th className="num w-36">Tổng tiền</th>
            <th className="w-32 text-center">Trạng thái</th>
            <th className="w-20 text-right">Xem</th>
          </tr>
        }
      >
        {filtered.map((o) => (
          <tr key={o.id} data-testid="sales-list-row" className="cursor-pointer" title="Nhấp đúp để xem phiếu" onDoubleClick={() => open(o)}>
            <td className="font-medium text-primary">{o.invoiceCode || '—'}</td>
            <td>{o.customerName || '—'}</td>
            <td className="text-ink-muted">{formatDateTime(o.createdAt)}</td>
            <td className="num font-medium">{formatNumber(o.totalAmount ?? 0)}</td>
            <td className="text-center" data-testid="sales-list-status">
              <StatusBadge status={o.status} />
            </td>
            <td className="text-right">
              <button
                type="button"
                onClick={() => open(o)}
                data-testid="sales-list-action-view"
                className="btn btn-ghost h-7 w-7 px-0 hover:text-primary"
                title="Xem chi tiết"
                aria-label="Xem chi tiết"
              >
                <Eye size={15} />
              </button>
            </td>
          </tr>
        ))}
      </ListTable>
    </div>
  );
};
