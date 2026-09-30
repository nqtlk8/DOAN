import { PageHeader } from '../../shared/components/Page/PageHeader';
import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../../api/ApiService';
import { ListTable } from '../../shared/components/DataState/ListTable';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import { formatDateTime, formatNumber } from '../../shared/utils/format';
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
      case 'RETURN': return 'text-warning bg-warning-soft'; // Giảm nợ
      case 'ADJUSTMENT': return 'text-primary bg-primary-soft';
      default: return 'text-ink-muted bg-app';
    }
  };

  return (
    <PageContainer>
      <PageHeader title={`Sao kê công nợ: ${customerName}`} subtitle="Lịch sử phát sinh và dư nợ theo từng chứng từ" />
      <ListTable
        colCount={6}
        isLoading={isLoading}
        isError={isError}
        error={error as Error | null}
        onRetry={refetch}
        totalCount={movements.length}
        filteredCount={movements.length}
        emptyTitle="Chưa có dữ liệu sao kê"
        emptyMessage="Khách hàng chưa có lịch sử phát sinh công nợ."
        header={
          <tr>
            <th className="w-40">Thời gian</th>
            <th className="w-44">Loại nghiệp vụ</th>
            <th className="w-40">Chứng từ</th>
            <th className="num w-36">Phát sinh</th>
            <th className="num w-36">Dư nợ</th>
            <th>Ghi chú</th>
          </tr>
        }
      >
        {movements.map((m) => {
          const amount = Number(m.amount || 0);
          return (
            <tr key={m.id} data-testid="debt-movement-row">
              <td className="whitespace-nowrap">{formatDateTime(m.createdAt) || '—'}</td>
              <td>
                <span className={`badge ${getMovementColor(m.movementType)}`}>{getMovementLabel(m.movementType)}</span>
              </td>
              <td>{m.referenceCode || m.refId || '—'}</td>
              <td className={`num font-medium ${amount > 0 ? 'text-danger' : 'text-success'}`}>
                {amount > 0 ? '+' : amount < 0 ? '−' : ''}
                {formatNumber(Math.abs(amount))}
              </td>
              <td className="num font-medium">{formatNumber(Number(m.balanceAfter || 0))}</td>
              <td className="text-ink-subtle truncate max-w-xs" title={m.note || ''}>
                {m.note || '—'}
              </td>
            </tr>
          );
        })}
      </ListTable>
    </PageContainer>
  );
};
