import { formatNumber, normalizeSearch } from '../../shared/utils/format';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { ListTable } from '../../shared/components/DataState/ListTable';
import type { components } from '@erp/api-contract';

export const StockList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery<components['schemas']['StockOnHandResponseDto'][]>({
    queryKey: ['stocks'],
    queryFn: () => ApiService.Stock.getAll(),
  });

  const items = response || [];
  const term = normalizeSearch(searchTerm);
  const filtered = items.filter(
    (c) => normalizeSearch(c.productName).includes(term) || normalizeSearch(c.productCode).includes(term),
  );

  return (
    <PageContainer data-testid="stock-page">
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

      <ListTable
        colCount={4}
        isLoading={loading}
        isError={isError}
        error={error}
        onRetry={refetch}
        totalCount={items.length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có tồn kho"
        emptyMessage="Hệ thống chưa ghi nhận tồn kho nào."
        header={
          <tr>
            <th className="w-36">Mã SP</th>
            <th>Tên sản phẩm</th>
            <th className="num w-32">Số lượng</th>
            <th className="w-48">Kho</th>
          </tr>
        }
      >
        {filtered.map((c, i) => (
          <tr key={c.id ?? i} data-testid="stock-row">
            <td>{c.productCode || '—'}</td>
            <td className="font-medium">{c.productName}</td>
            <td
              data-testid="stock-qty"
              className={`num font-medium ${c.quantity != null && c.quantity < 0 ? 'text-danger' : 'text-ink'}`}
            >
              {formatNumber(c.quantity ?? 0, 3)}
            </td>
            <td>{c.branchName || '—'}</td>
          </tr>
        ))}
      </ListTable>
    </PageContainer>
  );
};
