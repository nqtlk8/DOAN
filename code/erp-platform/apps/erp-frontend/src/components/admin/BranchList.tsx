import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { DataState } from '../../shared/components/DataState/DataState';

export const BranchList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery({
    queryKey: ['branches'],
    queryFn: () => ApiService.Branch.getAll(),
  });

  const items: any[] = response || [];
  const filtered = items.filter((c) => c.name?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <PageContainer>
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

      <div className="card overflow-hidden">
        <DataState
          isLoading={loading}
          isError={isError}
          error={error}
          isEmpty={filtered.length === 0} 
          onRetry={refetch}
          loadingType="table"
          emptyTitle="Chưa có chi nhánh"
          emptyMessage="Hệ thống chưa có chi nhánh nào hoặc không tìm thấy."
        >
          <table className="erp-table">
            <thead>
              <tr>
                <th>Mã CN</th>
                <th>Tên Chi Nhánh</th>
                <th>Địa Chỉ</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c, i) => (
                <tr key={i}>
                  <td>{c.branchCode || '-'}</td>
                  <td className="font-medium text-ink">{c.name}</td>
                  <td>{c.address || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </DataState>
      </div>
    </PageContainer>
  );
};
