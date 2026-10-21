import { formatNumber, normalizeSearch } from '../../shared/utils/format';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Eye, Wallet } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { useQuery } from '@tanstack/react-query';
import { ListTable } from '../../shared/components/DataState/ListTable';
import type { components } from '@erp/api-contract';
import { useTabs } from '../../context/TabContext';
import { DebtStatement } from './DebtStatement';
import { CashReceiptModule } from './CashReceiptModule';
import { useAuth, ROLES } from '../../context/AuthContext';

export const DebtList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const { openTab } = useTabs();
  const { hasRole } = useAuth();
  // Ở HQ, quản trị viên thấy công nợ của mọi chi nhánh: một khách có thể có một dòng ở mỗi chi nhánh,
  // nên cần cột Chi nhánh để phân biệt.
  const isAdmin = hasRole(ROLES.ADMIN);
  // Phiếu thu chỉ lập được ở chi nhánh (nhân viên); quản trị viên ở HQ chỉ xem.
  const canCollect = hasRole(ROLES.STAFF);
  const { data: branches = [] } = useQuery<any[]>({
    queryKey: ['branches'],
    queryFn: () => ApiService.Branch.getAll(),
    enabled: isAdmin,
  });
  const branchName = (id?: number | null) =>
    branches.find((b) => Number(b.id) === Number(id))?.name || (id != null ? `Chi nhánh ${id}` : '—');

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery<components['schemas']['ReceivableDebtResponseDto'][]>({
    queryKey: ['debts'],
    queryFn: () => ApiService.Debt.getAll(),
  });

  const items = response || [];
  const term = normalizeSearch(searchTerm);
  const filtered = items.filter(
    (c) => normalizeSearch(c.customerName).includes(term) || normalizeSearch(c.customerCode).includes(term),
  );

  const handleViewStatement = (debt: components['schemas']['ReceivableDebtResponseDto']) => {
    if (debt.customerId && debt.customerName) {
      const branchId = isAdmin ? debt.branchId ?? undefined : undefined;
      openTab(
        `debt-${debt.customerId}${branchId != null ? `-${branchId}` : ''}`,
        `SAO KÊ: ${debt.customerName.substring(0, 10).toUpperCase()}...`,
        <DebtStatement
          customerId={debt.customerId}
          customerName={debt.customerName}
          branchId={branchId}
          branchName={branchId != null ? branchName(branchId) : undefined}
        />,
        true
      );
    }
  };

  const handleCollect = (debt: components['schemas']['ReceivableDebtResponseDto']) => {
    if (!debt.customerId) return;
    openTab(
      `cash-receipt-${debt.customerId}`,
      `THU TIỀN: ${(debt.customerName || '').substring(0, 10).toUpperCase()}...`,
      <CashReceiptModule
        initialCustomer={{
          id: debt.customerId,
          customerCode: debt.customerCode,
          name: debt.customerName,
          phone: debt.phone,
          address: debt.address,
        }}
      />,
      true,
    );
  };

  return (
    <PageContainer data-testid="debt-page">
      <PageHeader 
        title="Danh Mục Công Nợ"
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
        colCount={isAdmin ? 7 : 6}
        isLoading={loading}
        isError={isError}
        error={error}
        onRetry={refetch}
        totalCount={items.length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có công nợ"
        emptyMessage="Chưa có khách hàng nào phát sinh công nợ."
        header={
          <tr>
            <th className="w-32">Mã KH</th>
            <th>Tên khách hàng</th>
            {isAdmin && <th className="w-52">Chi nhánh</th>}
            <th className="w-36">Số điện thoại</th>
            <th>Địa chỉ</th>
            <th className="num w-40">Tổng nợ</th>
            <th className={`${canCollect ? 'w-52' : 'w-28'} text-right`}>Thao tác</th>
          </tr>
        }
      >
        {filtered.map((c, i) => (
          <tr key={c.id ?? `${c.customerId}-${c.branchId}-${i}`} data-testid="debt-row" className="cursor-pointer" onDoubleClick={() => handleViewStatement(c)}>
            <td>{c.customerCode || '—'}</td>
            <td className="font-medium">{c.customerName}</td>
            {isAdmin && <td className="text-ink-muted">{branchName(c.branchId)}</td>}
            <td>{c.phone || '—'}</td>
            <td className="truncate max-w-xs">{c.address || '—'}</td>
            <td className={`num font-medium ${(c.totalDebt ?? 0) > 0 ? 'text-danger' : 'text-ink'}`}>
              {formatNumber(c.totalDebt ?? 0)}
            </td>
            <td className="text-right whitespace-nowrap">
              {canCollect && (c.totalDebt ?? 0) > 0 && (
                <button
                  type="button"
                  onClick={() => handleCollect(c)}
                  onDoubleClick={(e) => e.stopPropagation()}
                  className="btn btn-ghost h-7 text-success"
                  title="Lập phiếu thu cho khách này"
                  data-testid="debt-collect"
                >
                  <Wallet size={15} /> Thu tiền
                </button>
              )}
              <button type="button" onClick={() => handleViewStatement(c)} className="btn btn-ghost h-7 text-primary" title="Xem sao kê">
                <Eye size={15} /> Sao kê
              </button>
            </td>
          </tr>
        ))}
      </ListTable>
    </PageContainer>
  );
};
