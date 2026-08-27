import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';

export const BranchList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: response, isLoading: loading } = useQuery({
    queryKey: ['branches'],
    queryFn: () => ApiService.Branch.getAll(),
  });

  const items: any[] = response || [];
  const filtered = items.filter((c) => c.branchName?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Danh Sách Chi Nhánh</h2>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-4 border-b border-slate-100">
          <div className="relative w-72">
            <input
              type="text"
              placeholder="Tìm kiếm chi nhánh..."
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Mã CN</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Tên Chi Nhánh</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Địa Chỉ</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={3} className="px-6 py-8 text-center text-slate-900">Đang tải dữ liệu...</td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={3} className="px-6 py-8 text-center text-slate-900">Không tìm thấy dữ liệu.</td>
              </tr>
            ) : (
              filtered.map((c, i) => (
                <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-1.5 text-sm text-slate-900">{c.branchCode || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900 font-medium">{c.branchName}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{c.address || '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
