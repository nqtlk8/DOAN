import React from 'react';
import { ListTable } from '../../shared/components/DataState/ListTable';
import type { StockMovement } from '../../types/inventory';
import { useStockMovements } from '../../hooks/useStockMovements';
import { formatDateTime, formatNumber } from '../../shared/utils/format';

interface StockMovementListProps {
  productId: string;
}

const TYPE_LABEL: Record<string, { label: string; className: string }> = {
  INBOUND: { label: 'INBOUND', className: 'bg-success-soft text-success' },
  SALE: { label: 'SALE', className: 'bg-danger-soft text-danger' },
  RETURN: { label: 'RETURN', className: 'bg-primary-soft text-primary' },
};

export const StockMovementList: React.FC<StockMovementListProps> = ({ productId }) => {
  const { data: movements = [], isLoading, isError, error, refetch } = useStockMovements(productId);

  return (
    <div className="mt-4 space-y-2">
      <h3 className="text-[14px] font-semibold text-ink">Lịch Sử Movement</h3>
      <ListTable
        colCount={5}
        isLoading={!!isLoading}
        isError={!!isError}
        error={error as Error | null}
        onRetry={refetch}
        totalCount={movements.length}
        filteredCount={movements.length}
        emptyTitle="Chưa có movement"
        emptyMessage="Sản phẩm này chưa có lịch sử xuất nhập tồn."
        header={
          <tr>
            <th className="w-40">Thời gian</th>
            <th className="w-32">Loại</th>
            <th className="num w-24">SL</th>
            <th>Loại tham chiếu</th>
            <th>ID tham chiếu</th>
          </tr>
        }
      >
        {movements.map((m: StockMovement, i: number) => {
          const t = TYPE_LABEL[m.movementType] ?? { label: m.movementType, className: 'bg-slate-100 text-ink' };
          return (
            <tr key={m.id || i}>
              <td>{formatDateTime(m.createdAt)}</td>
              <td>
                <span className={`badge ${t.className}`}>{t.label}</span>
              </td>
              <td className={`num font-medium ${m.quantity > 0 ? 'text-success' : 'text-danger'}`}>
                {m.quantity > 0 ? '+' : ''}
                {formatNumber(m.quantity, 3)}
              </td>
              <td>{m.refType || '—'}</td>
              <td className="text-ink-subtle">{m.refId || '—'}</td>
            </tr>
          );
        })}
      </ListTable>
    </div>
  );
};
