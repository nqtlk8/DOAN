import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ApiService } from '../../api/ApiService';
import { DataState } from '../../shared/components/DataState/DataState';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatDateTime, formatNumber } from '../../shared/utils/format';
import type { InboundReceiptDto, SupplierDto } from '../../types/documents';

interface InboundReceiptListProps {
  onRowDoubleClick?: (id: string) => void;
}

/**
 * Danh sách phiếu nhập. API chỉ trả supplierId và lines (không có tên NCC, không có tổng tiền)
 * → tra tên NCC từ danh mục và tự tính tổng = Σ số lượng × giá nhập.
 */
export const InboundReceiptList: React.FC<InboundReceiptListProps> = ({ onRowDoubleClick }) => {
  const { data: receipts = [], isLoading, isError, error, refetch } = useQuery<InboundReceiptDto[]>({
    queryKey: ['inbound-receipts'],
    queryFn: () => ApiService.InboundReceipt.getAll(),
  });
  const { data: suppliers = [] } = useQuery<SupplierDto[]>({
    queryKey: ['suppliers'],
    queryFn: () => ApiService.Catalog.getSuppliers(),
  });

  const supplierName = useMemo(() => {
    const map = new Map(suppliers.map((s) => [String(s.id), s.name ?? '']));
    return (id?: string) => (id ? map.get(String(id)) ?? '—' : '—');
  }, [suppliers]);

  const total = (r: InboundReceiptDto) =>
    (r.lines ?? []).reduce((s, l) => s + (Number(l.quantity) || 0) * (Number(l.unitCost) || 0), 0);

  return (
    <div className="h-full bg-surface flex flex-col card overflow-hidden">
      <DataState
        isLoading={isLoading}
        isError={isError}
        error={error}
        onRetry={refetch}
        loadingType="table"
        isEmpty={receipts.length === 0}
        emptyTitle="Chưa có phiếu nhập"
        emptyMessage="Các phiếu nhập hàng đã lưu sẽ hiển thị ở đây."
      >
        <div className="flex-1 overflow-auto">
          <table className="erp-table">
            <thead className="sticky top-0">
              <tr>
                <th className="w-12 text-center">STT</th>
                <th>Số phiếu</th>
                <th>Ngày tạo</th>
                <th>Nhà cung cấp</th>
                <th className="num">Tổng tiền</th>
                <th className="w-32 text-center">Trạng thái</th>
                <th>Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              {receipts.map((r, index) => (
                <tr
                  key={r.id}
                  className="cursor-pointer"
                  data-testid="inbound-list-row"
                  title="Nhấp đúp để xem phiếu"
                  onDoubleClick={() => r.id && onRowDoubleClick?.(r.id)}
                >
                  <td className="text-center text-ink-subtle">{index + 1}</td>
                  <td className="font-medium text-primary">{r.receiptCode || '—'}</td>
                  <td>{formatDateTime(r.createdAt)}</td>
                  <td>{supplierName(r.supplierId)}</td>
                  <td className="num font-medium">{formatNumber(total(r))}</td>
                  <td className="text-center">
                    <StatusBadge status={r.status} />
                  </td>
                  <td className="text-ink-muted truncate max-w-[240px]">{r.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </DataState>
    </div>
  );
};
