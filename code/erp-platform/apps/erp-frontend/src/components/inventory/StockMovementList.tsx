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
    <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden mt-4">
      <div className="p-4 border-b border-slate-100 bg-slate-50">
        <h3 className="font-semibold text-slate-900">Lịch Sử Movement</h3>
      </div>
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="bg-slate-50 border-b border-slate-100">
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Thời gian</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Loại</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">SL</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Loại tham chiếu</th>
            <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">ID tham chiếu</th>
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
              <tr key={m.id || i} className="border-b border-slate-50 hover:bg-slate-50">
                <td className="px-4 py-2 text-sm text-slate-900">
                  {new Date(m.createdAt).toLocaleString('vi-VN')}
                </td>
                <td className="px-4 py-2 text-sm">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                    m.movementType === 'INBOUND' ? 'bg-emerald-50 text-emerald-700' :
                    m.movementType === 'SALE' ? 'bg-rose-50 text-rose-700' :
                    m.movementType === 'RETURN' ? 'bg-blue-50 text-blue-700' : 'bg-slate-100 text-slate-700'
                  }`}>
                    {m.movementType}
                  </span>
                </td>
                <td className={`px-4 py-2 text-sm font-medium ${m.quantity > 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {m.quantity > 0 ? '+' : ''}{m.quantity}
                </td>
                <td className="px-4 py-2 text-sm text-slate-900">{m.refType || '-'}</td>
                <td className="px-4 py-2 text-sm text-slate-500">{m.refId || '-'}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};
