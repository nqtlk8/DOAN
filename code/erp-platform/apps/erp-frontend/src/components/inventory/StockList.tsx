import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { DataState } from '../../shared/components/DataState/DataState';

export const StockList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery({
    queryKey: ['stocks'],
    queryFn: () => ApiService.Stock.getAll(),
  });

  const items: any[] = response || [];
  const filtered = items.filter((c) => c.productName?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Danh Mục Tồn Kho</h2>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-4 border-b border-slate-100">
          <div className="relative w-72">
            <input
              type="text"
              placeholder="Tìm kiếm sản phẩm..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900"
            />
            <Search className="absolute left-3 top-2.5 text-slate-900" size={20} />
          </div>
        </div>

        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-100">
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Mã SP</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Tên SP</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Số Lượng</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Kho</th>
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
                    <div className="p-8 text-center text-slate-500 bg-white">
                      Không tìm thấy kết quả nào phù hợp với "{searchTerm}"
                    </div>
                  ) : null}
                </DataState>
              </td>
            </tr>
            {!loading && !isError && filtered.length > 0 && (
              filtered.map((c, i) => (
                <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-1.5 text-sm text-slate-900">{c.productCode || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900 font-medium">{c.productName}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{c.quantity || 0}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{c.warehouse || '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
