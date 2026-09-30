import { formatNumber, normalizeSearch } from '../../shared/utils/format';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Eye } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { ListTable } from '../../shared/components/DataState/ListTable';
import type { components } from '@erp/api-contract';
import { useTabs } from '../../context/TabContext';
import { DebtStatement } from './DebtStatement';

export const DebtList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const { openTab } = useTabs();

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery<components['schemas']['ReceivableDebtResponseDto'][]>({
    queryKey: ['debts'],
    queryFn: () => ApiService.Debt.getAll(),
  });

  const items = response || [];
  const term = normalizeSearch(searchTerm);
  const filtered = items.filter(
    (c) => normalizeSearch(c.customerName).includes(term) || normalizeSearch(c.customerCode).includes(term),
  );

  const handleViewStatement = (debt: components['schemas']['ReceivableDebtResponseDto']) => {
    if (debt.customerId && debt.customerName) {
      openTab(
        `debt-${debt.customerId}`,
        `SAO KÊ: ${debt.customerName.substring(0, 10).toUpperCase()}...`,
        <DebtStatement customerId={debt.customerId} customerName={debt.customerName} />,
        true
      );
    }
  };

  return (
    <PageContainer data-testid="debt-page">
      <PageHeader 
        title="Danh Mục Công Nợ"
        actions={
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search size={16} className="absolute left-2.5 top-2 text-ink-subtle" />
              <input
                type="text"
                placeholder="Tìm theo mã, tên..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="erp-input h-8 pl-8 w-[280px]"
              />
            </div>
            
          </div>
        }
      />

      <ListTable
        colCount={6}
        isLoading={loading}
        isError={isError}
        error={error}
        onRetry={refetch}
        totalCount={items.length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có công nợ"
        emptyMessage="Chưa có khách hàng nào phát sinh công nợ."
        header={
          <tr>
            <th className="w-32">Mã KH</th>
            <th>Tên khách hàng</th>
            <th className="w-36">Số điện thoại</th>
            <th>Địa chỉ</th>
            <th className="num w-40">Tổng nợ</th>
            <th className="w-28 text-right">Sao kê</th>
          </tr>
        }
      >
        {filtered.map((c, i) => (
          <tr key={c.customerId ?? i} data-testid="debt-row" className="cursor-pointer" onDoubleClick={() => handleViewStatement(c)}>
            <td>{c.customerCode || '—'}</td>
            <td className="font-medium">{c.customerName}</td>
            <td>{c.phone || '—'}</td>
            <td className="truncate max-w-xs">{c.address || '—'}</td>
            <td className={`num font-medium ${(c.totalDebt ?? 0) > 0 ? 'text-danger' : 'text-ink'}`}>
              {formatNumber(c.totalDebt ?? 0)}
            </td>
            <td className="text-right">
              <button type="button" onClick={() => handleViewStatement(c)} className="btn btn-ghost h-7 text-primary" title="Xem sao kê">
                <Eye size={15} /> Sao kê
              </button>
            </td>
          </tr>
        ))}
      </ListTable>
    </PageContainer>
  );
};
