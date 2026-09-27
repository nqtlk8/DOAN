import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../../api/ApiService';
import { DataState } from '../../shared/components/DataState/DataState';
import { formatCurrency } from '../../shared/utils/format';

interface InboundReceiptListProps {
  onRowDoubleClick?: (id: string) => void;
}

export const InboundReceiptList: React.FC<InboundReceiptListProps> = ({ onRowDoubleClick }) => {
  const { data: response, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['inboundReceipts'],
    queryFn: () => ApiService.InboundReceipt.getAll(),
  });

  const receipts = response || [];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-primary/10 text-blue-700 border-blue-200';
      case 'DRAFT':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'CANCELLED':
        return 'bg-red-50 text-red-700 border-red-200';
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
    <div className="h-full bg-white flex flex-col">
      <DataState
        isLoading={isLoading}
        isError={isError}
        error={error}
        onRetry={refetch}
        
        isEmpty={receipts.length === 0 && !isLoading}
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
              {receipts.map((receipt: any, index: number) => (
                <tr
                  key={receipt.id}
                  className="hover:bg-slate-50 cursor-pointer"
                  data-testid="inbound-list-row"
                  onDoubleClick={() => onRowDoubleClick?.(receipt.id)}
                >
                  <td className="text-center text-slate-500">{index + 1}</td>
                  <td className="font-medium text-primary">{receipt.receiptCode}</td>
                  <td>{new Date(receipt.createdAt).toLocaleString('vi-VN')}</td>
                  <td>{receipt.supplierName}</td>
                  <td className="text-right font-medium">{formatCurrency(receipt.totalAmount || 0)}</td>
                  <td className="text-center">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${getStatusColor(receipt.status)}`}>
                      {getStatusText(receipt.status)}
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
