import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { DataState } from '../../shared/components/DataState/DataState';
import type { components } from '@erp/api-contract';

export const StockList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery<components['schemas']['StockOnHandResponseDto'][]>({
    queryKey: ['stocks'],
    queryFn: () => ApiService.Stock.getAll(),
  });

  const items = response || [];
  const filtered = items.filter((c) => c.productName?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <PageContainer>
      <PageHeader 
        title="Danh Mục Tồn Kho"
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Mã SP</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Tên SP</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle num">Số Lượng</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Kho</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={4} className="p-0">
                <DataState
                  isLoading={loading}
                  isError={isError}
                  error={error}
                  isEmpty={items.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có tồn kho"
                  emptyMessage="Hệ thống chưa ghi nhận tồn kho nào."
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
                <tr key={i} className="border-b border-slate-50 hover:bg-app transition-colors">
                  <td className="px-4 py-1.5 text-sm text-ink">{c.productCode || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-ink font-medium">{c.productName}</td>
                  <td className={`px-4 py-1.5 text-sm num ${c.quantity != null && c.quantity < 0 ? 'text-danger' : 'text-ink'}`}>
                    {c.quantity || 0}
                  </td>
                  <td className="px-4 py-1.5 text-sm text-ink">{c.branchName || '-'}</td>
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
