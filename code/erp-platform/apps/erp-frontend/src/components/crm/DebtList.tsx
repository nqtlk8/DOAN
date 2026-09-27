import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Eye } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { DataState } from '../../shared/components/DataState/DataState';
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
  const filtered = items.filter((c) => c.customerName?.toLowerCase().includes(searchTerm.toLowerCase()));

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
    <PageContainer>
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

      <div className="card overflow-hidden">
        <DataState
          isLoading={loading}
          isError={isError}
          error={error}
          isEmpty={filtered.length === 0} 
          onRetry={refetch}
          loadingType="table"
        >
          <table className="erp-table">
          <thead>
            <tr className="bg-app border-b border-line">
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">MÃ Đối Tác</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Tên Đối Tác</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Số Điện Thoại</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Địa Chỉ</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Dư Nợ</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle text-right">Thao Tác</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={6} className="p-0">
                <DataState
                  isLoading={loading}
                  isError={isError}
                  error={error}
                  isEmpty={items.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có công nợ"
                  emptyMessage="Hệ thống chưa ghi nhận công nợ nào."
                >
                  {items.length > 0 && filtered.length === 0 ? (
                    <div className="p-8 text-center text-ink-subtle bg-surface">
                      Không tìm thấy kết quả nào phù hợp với "{searchTerm}"
                    </div>
                  ) : null}
                </DataState>
              </td>
            </tr>
            {!loading && !isError && filtered.length > 0 && (
              filtered.map((c, i) => (
                <tr 
                  key={i} 
                  className="border-b border-slate-50 hover:bg-app transition-colors cursor-pointer group"
                  onDoubleClick={() => handleViewStatement(c)}
                >
                  <td className="px-4 py-2 text-sm text-ink">{c.customerCode || '-'}</td>
                  <td className="px-4 py-2 text-sm text-ink font-medium">{c.customerName}</td>
                  <td className="px-4 py-2 text-sm text-ink">{c.phone || '-'}</td>
                  <td className="px-4 py-2 text-sm text-ink">{c.address || '-'}</td>
                  <td className="px-4 py-2 text-sm text-ink">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(c.totalDebt || 0)}
                  </td>
                  <td className="px-4 py-2 text-right">
                    <button
                      onClick={() => handleViewStatement(c)}
                      className="inline-flex items-center gap-1 px-2 py-1 text-sm text-primary hover:text-primary-dark hover:bg-primary-soft rounded transition-colors opacity-0 group-hover:opacity-100"
                      title="Xem sao kê"
                    >
                      <Eye size={16} /> Sao kê
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        </DataState>
      </div>
    </PageContainer>
  );
};
