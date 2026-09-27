import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../../api/ApiService';
import { DataState } from '../../shared/components/DataState/DataState';
import { formatCurrency } from '../../shared/utils/format';

interface GoodsReturnListProps {
  onRowDoubleClick?: (id: string) => void;
}

export const GoodsReturnList: React.FC<GoodsReturnListProps> = ({ onRowDoubleClick }) => {
  const { data: response, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['GoodsReturns'],
    queryFn: () => ApiService.GoodsReturn.getAll(),
  });

  const returns = response || [];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-primary/10 text-primary-dark border-primary-soft';
      case 'DRAFT':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'CANCELLED':
        return 'bg-danger-soft text-danger border-line';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'CONFIRMED': return 'Đã xác nhận';
      case 'DRAFT': return 'Nháp';
      case 'CANCELLED': return 'Đã hủy';
      default: return status;
    }
  };

  return (
    <div className="h-full bg-surface flex flex-col">
      <DataState
        isLoading={isLoading}
        isError={isError}
        error={error}
        onRetry={refetch}
        
        isEmpty={returns.length === 0 && !isLoading}
      >
        <div className="flex-1 overflow-auto">
          <table className="w-full erp-table">
            <thead>
              <tr>
                <th className="w-12 text-center">STT</th>
                <th>Số phiếu</th>
                <th>Ngày tạo</th>
                <th>Nhà cung cấp</th>
                <th className="text-right">Tổng tiền</th>
                <th className="text-center w-32">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {returns.map((retItem: any, index: number) => (
                <tr
                  key={retItem.id}
                  className="hover:bg-slate-50 cursor-pointer"
                  data-testid="return-list-row"
                  onDoubleClick={() => onRowDoubleClick?.(retItem.id)}
                >
                  <td className="text-center text-slate-500">{index + 1}</td>
                  <td className="font-medium text-primary">{retItem.returnCode}</td>
                  <td>{new Date(retItem.createdAt).toLocaleString('vi-VN')}</td>
                  <td>{retItem.customerName}</td>
                  <td className="text-right font-medium">{formatCurrency(retItem.totalAmount || 0)}</td>
                  <td className="text-center">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${getStatusColor(retItem.status)}`}>
                      {getStatusText(retItem.status)}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </DataState>
    </div>
  );
};

