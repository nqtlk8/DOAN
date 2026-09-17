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
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Danh Mục Công Nợ</h2>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-4 border-b border-slate-100">
          <div className="relative w-72">
            <input
              type="text"
              placeholder="Tìm kiếm đối tác..."
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">MÃ Đối Tác</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Tên Đối Tác</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Dư Nợ</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500 text-right">Thao Tác</th>
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
                  emptyTitle="Chưa có công nợ"
                  emptyMessage="Hệ thống chưa ghi nhận công nợ nào."
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
                <tr 
                  key={i} 
                  className="border-b border-slate-50 hover:bg-slate-50 transition-colors cursor-pointer group"
                  onDoubleClick={() => handleViewStatement(c)}
                >
                  <td className="px-4 py-2 text-sm text-slate-900">{c.customerCode || '-'}</td>
                  <td className="px-4 py-2 text-sm text-slate-900 font-medium">{c.customerName}</td>
                  <td className="px-4 py-2 text-sm text-slate-900">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(c.totalDebt || 0)}
                  </td>
                  <td className="px-4 py-2 text-right">
                    <button
                      onClick={() => handleViewStatement(c)}
                      className="inline-flex items-center gap-1 px-2 py-1 text-sm text-teal-600 hover:text-teal-700 hover:bg-teal-50 rounded transition-colors opacity-0 group-hover:opacity-100"
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
      </div>
    </div>
  );
};
