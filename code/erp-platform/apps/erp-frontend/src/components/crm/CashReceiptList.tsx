import React, { useState } from 'react';
import { Eye, Search } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../../api/ApiService';
import { ListTable } from '../../shared/components/DataState/ListTable';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatDate, formatNumber, normalizeSearch } from '../../shared/utils/format';
import type { CashReceiptDto } from '../../types/cashReceipt';

interface CashReceiptListProps {
  onRowDoubleClick?: (id: string) => void;
}

export const CashReceiptList: React.FC<CashReceiptListProps> = ({ onRowDoubleClick }) => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: receipts = [], isLoading, isError, error, refetch } = useQuery<CashReceiptDto[]>({
    queryKey: ['cashReceipts'],
    queryFn: () => ApiService.CashReceipt.getAll(),
  });

  const term = normalizeSearch(searchTerm);
  const filtered = receipts.filter(
    (r) =>
      normalizeSearch(r.receiptCode).includes(term) ||
      normalizeSearch(r.customerName).includes(term) ||
      normalizeSearch(r.customerCode).includes(term),
  );

  const open = (r: CashReceiptDto) => {
    if (onRowDoubleClick && r.id) onRowDoubleClick(r.id);
  };

  return (
    <div className="flex flex-col h-full gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-[18px] font-semibold text-ink">Danh sách phiếu thu</h2>
        <div className="relative">
          <Search className="absolute left-2.5 top-2 text-ink-subtle pointer-events-none" size={16} />
          <input
            type="text"
            data-testid="receipt-list-search"
            placeholder="Tìm theo số phiếu, khách hàng…"
            className="erp-input h-8 pl-8 w-[280px]"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      <ListTable
        colCount={9}
        isLoading={isLoading}
        isError={isError}
        error={error as Error | null}
        onRetry={refetch}
        totalCount={receipts.length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có phiếu thu"
        emptyMessage="Chi nhánh chưa lập phiếu thu nào."
        header={
          <tr>
            <th className="w-32">Số phiếu</th>
            <th className="w-28">Ngày thu</th>
            <th>Khách hàng</th>
            <th className="num w-32">Số tiền thu</th>
            <th className="num w-32">Nợ trước</th>
            <th className="num w-32">Nợ sau</th>
            <th className="w-28 text-center">Trạng thái</th>
            <th>Diễn giải</th>
            <th className="w-16 text-right">Xem</th>
          </tr>
        }
      >
        {filtered.map((r) => (
          <tr
            key={r.id}
            data-testid="receipt-list-row"
            className={`cursor-pointer ${r.status === 'CANCELLED' ? 'text-ink-subtle' : ''}`}
            title="Nhấp đúp để xem phiếu"
            onDoubleClick={() => open(r)}
          >
            <td className={`font-medium ${r.status === 'CANCELLED' ? 'line-through' : 'text-primary'}`}>{r.receiptCode}</td>
            <td>{formatDate(r.receiptDate)}</td>
            <td className="truncate max-w-xs">
              {r.customerName || '—'}
              {r.customerCode ? <span className="text-ink-subtle"> ({r.customerCode})</span> : null}
            </td>
            <td className="num font-medium">{formatNumber(r.amount, 4)}</td>
            <td className="num">{formatNumber(r.debtBefore, 4)}</td>
            <td className="num">{formatNumber(r.debtAfter, 4)}</td>
            <td className="text-center" data-testid="receipt-list-status">
              <StatusBadge status={r.status} />
            </td>
            <td className="truncate max-w-xs text-ink-muted" title={r.note || ''}>
              {r.note || '—'}
            </td>
            <td className="text-right">
              <button
                type="button"
                onClick={() => open(r)}
                className="btn btn-ghost h-7 w-7 px-0 hover:text-primary"
                title="Xem phiếu"
                aria-label="Xem phiếu"
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
