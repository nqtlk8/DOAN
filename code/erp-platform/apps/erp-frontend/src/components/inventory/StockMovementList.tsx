import { PageContainer } from '../../shared/components/Page/PageContainer';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import React from 'react';
import { DataState } from '../../shared/components/DataState/DataState';
import { StockMovement } from '../../types/inventory';
import { useStockMovements } from '../../hooks/useStockMovements';

interface StockMovementListProps {
  productId: string;
}

export const StockMovementList: React.FC<StockMovementListProps> = ({ productId }) => {
  const { data: movements, isLoading, isError, error, refetch } = useStockMovements(productId);

  return (
    <div className="bg-surface rounded-xl shadow-sm border border-line overflow-hidden mt-4">
      <div className="p-4 border-b border-line bg-app">
        <h3 className="font-semibold text-ink">Lịch Sử Movement</h3>
      </div>
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="bg-app border-b border-line">
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Thời gian</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Loại</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">SL</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Loại tham chiếu</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">ID tham chiếu</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td colSpan={5} className="p-0">
              <DataState
                isLoading={isLoading}
                isError={isError}
                error={error}
                isEmpty={!movements || movements.length === 0}
                onRetry={refetch}
                loadingType="table"
                emptyTitle="Chưa có movement"
                emptyMessage="Sản phẩm này chưa có lịch sử xuất nhập tồn."
              >{null}</DataState>
            </td>
          </tr>
          {!isLoading && !isError && movements && movements.length > 0 && (
            movements.map((m: StockMovement, i: number) => (
              <tr key={m.id || i} className="border-b border-slate-50 hover:bg-app">
                <td className="px-4 py-2 text-sm text-ink">
                  {new Date(m.createdAt).toLocaleString('vi-VN')}
                </td>
                <td className="px-4 py-2 text-sm">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                    m.movementType === 'INBOUND' ? 'bg-success-soft text-success' :
                    m.movementType === 'SALE' ? 'bg-danger-soft text-danger' :
                    m.movementType === 'RETURN' ? 'bg-primary-soft text-primary' : 'bg-slate-100 text-ink'
                  }`}>
                    {m.movementType}
                  </span>
                </td>
                <td className={`px-4 py-2 text-sm font-medium ${m.quantity > 0 ? 'text-success' : 'text-danger'}`}>
                  {m.quantity > 0 ? '+' : ''}{m.quantity}
                </td>
                <td className="px-4 py-2 text-sm text-ink">{m.refType || '-'}</td>
                <td className="px-4 py-2 text-sm text-ink-subtle">{m.refId || '-'}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};
