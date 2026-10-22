import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../../api/ApiService';
import { DataState } from '../../shared/components/DataState/DataState';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatDateTime, formatNumber } from '../../shared/utils/format';
import type { GoodsReturnResponse } from '../../types/documents';

interface GoodsReturnListProps {
  onRowDoubleClick?: (id: string) => void;
}

export const GoodsReturnList: React.FC<GoodsReturnListProps> = ({ onRowDoubleClick }) => {
  const { data: returns = [], isLoading, isError, error, refetch } = useQuery<GoodsReturnResponse[]>({
    queryKey: ['goods-returns'],
    queryFn: () => ApiService.GoodsReturn.getAll(),
  });

  const total = (r: GoodsReturnResponse) =>
    r.totalAmount ??
    (r.lines ?? []).reduce((s, l) => s + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0), 0);

  return (
    <div className="h-full bg-surface flex flex-col card overflow-hidden">
      <DataState
        isLoading={isLoading}
        isError={isError}
        error={error}
        onRetry={refetch}
        loadingType="table"
        isEmpty={returns.length === 0}
        emptyTitle="Chưa có phiếu nhập lại hàng bán"
        emptyMessage="Các phiếu trả hàng đã lưu sẽ hiển thị ở đây."
      >
        <div className="flex-1 overflow-auto">
          <table className="erp-table">
            <thead className="sticky top-0">
              <tr>
                <th className="w-12 text-center">STT</th>
                <th>Số phiếu</th>
                <th>Ngày tạo</th>
                <th>Khách hàng</th>
                <th className="num">Tổng tiền trả</th>
                <th className="w-32 text-center">Trạng thái</th>
                <th>Lý do</th>
              </tr>
            </thead>
            <tbody>
              {returns.map((r, index) => (
                <tr
                  key={r.id}
                  className="cursor-pointer"
                  data-testid="return-list-row"
                  title="Nhấp đúp để xem phiếu"
                  onDoubleClick={() => onRowDoubleClick?.(r.id)}
                >
                  <td className="text-center text-ink-subtle">{index + 1}</td>
                  <td className="font-medium text-primary">{r.returnCode || '—'}</td>
                  <td>{formatDateTime(r.createdAt)}</td>
                  <td>{r.customerName || '—'}</td>
                  <td className="num font-medium">{formatNumber(total(r))}</td>
                  <td className="text-center">
                    <StatusBadge status={r.status} />
                  </td>
                  <td className="text-ink-muted truncate max-w-[240px]">{r.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </DataState>
    </div>
  );
};
