import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../../api/ApiService';
import { DataState } from '../../shared/components/DataState/DataState';
import type { components } from '@erp/api-contract';

interface DebtStatementProps {
  customerId: string;
  customerName: string;
}

export const DebtStatement: React.FC<DebtStatementProps> = ({ customerId, customerName }) => {
  const { data: response, isLoading, isError, error, refetch } = useQuery<components['schemas']['ReceivableDebtMovementResponseDto'][]>({
    queryKey: ['debt-movements', customerId],
    queryFn: () => ApiService.Debt.getMovements(customerId),
  });

  const movements = response || [];

  const formatCurrency = (amount: number | undefined) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
  };

  const getMovementLabel = (type: string | undefined) => {
    switch (type) {
      case 'OPENING_BALANCE': return 'Số dư đầu kỳ';
      case 'SALES_INVOICE': return 'Bán hàng';
      case 'SALES_PAYMENT': return 'Thanh toán/Trả trước';
      case 'GOODS_RETURN': return 'Khách trả hàng';
      default: return type || '-';
    }
  };

  const getMovementColor = (type: string | undefined) => {
    switch (type) {
      case 'OPENING_BALANCE': return 'text-slate-600 bg-slate-100';
      case 'SALES_INVOICE': return 'text-red-600 bg-red-50'; // Tăng nợ
      case 'SALES_PAYMENT': return 'text-green-600 bg-green-50'; // Giảm nợ
      case 'GOODS_RETURN': return 'text-amber-600 bg-amber-50'; // Giảm nợ
      default: return 'text-slate-600 bg-slate-50';
    }
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Sao Kê Công Nợ: {customerName}</h2>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-100">
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-slate-500">Thời gian</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-slate-500">Loại nghiệp vụ</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-slate-500">Chứng từ</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-slate-500 text-right">Phát sinh</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-slate-500 text-right">Dư nợ</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-slate-500">Ghi chú</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={6} className="p-0">
                <DataState
                  isLoading={isLoading}
                  isError={isError}
                  error={error}
                  isEmpty={movements.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có dữ liệu sao kê"
                  emptyMessage="Khách hàng chưa có lịch sử phát sinh công nợ."
                >
                  {null}
                </DataState>
              </td>
            </tr>
            {!isLoading && !isError && movements.length > 0 && (
              movements.map((m) => {
                const isIncrease = m.movementType === 'OPENING_BALANCE' || m.movementType === 'SALES_INVOICE';
                return (
                  <tr key={m.id} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 text-sm text-slate-900 whitespace-nowrap">
                      {m.createdAt ? new Date(m.createdAt).toLocaleString('vi-VN') : '-'}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${getMovementColor(m.movementType)}`}>
                        {getMovementLabel(m.movementType)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-900">{m.refId || '-'}</td>
                    <td className={`px-4 py-3 text-sm font-medium text-right ${isIncrease ? 'text-red-600' : 'text-green-600'}`}>
                      {isIncrease ? '+' : '-'}{formatCurrency(m.amount)}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-900 font-medium text-right">
                      {formatCurrency(m.balanceAfter)}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-500 truncate max-w-xs" title={m.note || ''}>
                      {m.note || '-'}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
