import { normalizeSearch } from '../../shared/utils/format';
import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { ListTable } from '../../shared/components/DataState/ListTable';

export const BranchList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery({
    queryKey: ['branches'],
    queryFn: () => ApiService.Branch.getAll(),
  });

  const items: any[] = response || [];
  const term = normalizeSearch(searchTerm);
  const filtered = items.filter(
    (c) => normalizeSearch(c.name).includes(term) || normalizeSearch(c.branchCode).includes(term),
  );

  return (
    <PageContainer data-testid="branch-page">
      <PageHeader 
        title="Danh Sách Chi Nhánh"
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
        colCount={3}
        isLoading={loading}
        isError={isError}
        error={error}
        onRetry={refetch}
        totalCount={items.length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có chi nhánh"
        emptyMessage="Hệ thống chưa có chi nhánh nào."
        header={
          <tr>
            <th className="w-32">Mã CN</th>
            <th>Tên chi nhánh</th>
            <th>Địa chỉ</th>
          </tr>
        }
      >
        {filtered.map((c, i) => (
          <tr key={c.id ?? i} data-testid="branch-row">
            <td>{c.branchCode || '—'}</td>
            <td className="font-medium">{c.name}</td>
            <td>{c.address || '—'}</td>
          </tr>
        ))}
      </ListTable>
    </PageContainer>
  );
};
