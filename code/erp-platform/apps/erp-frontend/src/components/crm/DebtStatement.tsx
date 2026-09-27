import { PageHeader } from '../../shared/components/Page/PageHeader';
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
      case 'INVOICE': return 'Bán hàng';
      case 'PAYMENT': return 'Thanh toán/Trả trước';
      case 'RETURN': return 'Khách trả hàng';
      case 'ADJUSTMENT': return 'Điều chỉnh';
      default: return type || '-';
    }
  };

  const getMovementColor = (type: string | undefined) => {
    switch (type) {
      case 'OPENING_BALANCE': return 'text-ink-muted bg-slate-100';
      case 'INVOICE': return 'text-danger bg-danger-soft'; // Tăng nợ
      case 'PAYMENT': return 'text-success bg-success-soft'; // Giảm nợ
      case 'RETURN': return 'text-amber-600 bg-amber-50'; // Giảm nợ
      case 'ADJUSTMENT': return 'text-primary bg-primary-soft';
      default: return 'text-ink-muted bg-app';
    }
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-ink">Sao Kê Công Nợ: {customerName}</h2>
      </div>

      <div className="bg-surface rounded-xl shadow-sm border border-line overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-app border-b border-line">
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-ink-subtle">Thời gian</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-ink-subtle">Loại nghiệp vụ</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-ink-subtle">Chứng từ</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-ink-subtle text-right">Phát sinh</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-ink-subtle text-right">Dư nợ</th>
              <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-ink-subtle">Ghi chú</th>
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
                const isIncrease = (m.amount || 0) > 0;
                const displayAmount = Math.abs(m.amount || 0);
                return (
                  <tr key={m.id} className="border-b border-slate-50 hover:bg-app transition-colors">
                    <td className="px-4 py-3 text-sm text-ink whitespace-nowrap">
                      {m.createdAt ? new Date(m.createdAt).toLocaleString('vi-VN') : '-'}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${getMovementColor(m.movementType)}`}>
                        {getMovementLabel(m.movementType)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-ink">{m.referenceCode || m.refId || '-'}</td>
                    <td className={`px-4 py-3 text-sm font-medium text-right ${isIncrease ? 'text-danger' : 'text-success'}`}>
                      {isIncrease ? '+' : '-'}{formatCurrency(displayAmount)}
                    </td>
                    <td className="px-4 py-3 text-sm text-ink font-medium text-right">
                      {formatCurrency(m.balanceAfter)}
                    </td>
                    <td className="px-4 py-3 text-sm text-ink-subtle truncate max-w-xs" title={m.note || ''}>
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
