import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Plus, X, Edit, Trash2 } from 'lucide-react';
import type { Customer } from '../../types/catalog';
import { useAuth, ROLES } from '../../context/AuthContext';
import { DataState } from '../../shared/components/DataState/DataState';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { useCustomers } from '../../hooks/useCustomers';

export const CustomerList: React.FC = () => {
  const { hasRole } = useAuth();
  const isAdmin = hasRole(ROLES.ADMIN);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [confirmState, setConfirmState] = useState({ isOpen: false, id: '' });

  const [formData, setFormData] = useState<Partial<Customer & { branchId?: number }>>({});
  const [isEditing, setIsEditing] = useState(false);
  const [branches, setBranches] = useState<any[]>([]);

  React.useEffect(() => {
    import('../../api/ApiService').then(({ ApiService }) => {
      ApiService.Branch.getAll().then(setBranches).catch(console.error);
    });
  }, []);

  const {
    customers,
    isLoading: loading,
    isError,
    error,
    refetch,
    createCustomer,
    isCreating,
    updateCustomer,
    isUpdating,
    deleteCustomer
  } = useCustomers();

  const handleOpenModal = (customer?: Customer) => {
    if (customer) {
      setIsEditing(true);
      setFormData(customer);
    } else {
      setIsEditing(false);
      setFormData({});
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setFormData({});
    setIsEditing(false);
  };

  const handleSave = () => {
    const payload = {
      customerCode: formData.customerCode || formData.code,
      name: formData.name,
      phone: formData.phone,
      email: formData.email,
      address: formData.address,
      taxCode: formData.taxCode,
      branchId: formData.branchId ? Number(formData.branchId) : undefined
    };

    if (isEditing && formData.id) {
      updateCustomer({ id: String(formData.id), payload }, { onSuccess: handleCloseModal });
    } else {
      createCustomer(payload, { onSuccess: handleCloseModal });
    }
  };

  const handleDelete = (id: string) => {
    setConfirmState({ isOpen: true, id });
  };

  const submitting = isCreating || isUpdating;

  const filtered = customers.filter((c) => !c.isDeleted && c.name?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <PageContainer>
      <PageHeader 
        title="Danh Mục Khách Hàng"
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
            {isAdmin && (
              <button data-testid="customer-create-button" onClick={() => handleOpenModal()} className="btn-primary h-8 px-3 flex items-center">
                <Plus size={16} className="mr-1" />
                Thêm mới
              </button>
            )}
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
        >
          <table className="erp-table">
          <thead>
            <tr className="bg-app border-b border-line">
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Mã KH</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Tên KH</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Số Điện Thoại</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Địa Chỉ</th>
              {isAdmin && (
                <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle text-right">
                  Thao Tác
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={isAdmin ? 6 : 5} className="p-0">
                <DataState
                  isLoading={loading}
                  isError={isError}
                  error={error}
                  isEmpty={customers.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có khách hàng"
                  emptyMessage="Hệ thống chưa ghi nhận khách hàng nào."
                >
                  {customers.length > 0 && filtered.length === 0 ? (
                    <div className="p-8 text-center text-ink-subtle bg-surface">
                      Không tìm thấy kết quả nào phù hợp với "{searchTerm}"
                    </div>
                  ) : null}
                </DataState>
              </td>
            </tr>
            {!loading && !isError && filtered.length > 0 && (
              filtered.map((c, i) => (
                <tr data-testid="customer-row" key={c.id || i} className="border-b border-slate-50 hover:bg-app transition-colors">
                  <td className="px-4 py-1.5 text-sm text-ink">{c.customerCode || c.code || '-'}</td>
                  <td className="px-4 py-1.5 text-sm font-medium text-ink">{c.name}</td>
                  <td className="px-4 py-1.5 text-sm text-ink">{c.phone || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-ink truncate max-w-xs">{c.address || '-'}</td>
                  
                </tr>
              ))
            )}
          </tbody>
        </table>
        </DataState>
      </div>

      {isModalOpen && (
        <div data-testid="customer-create-modal" className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
          <div className="bg-surface rounded-xl shadow-xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-5 border-b border-line">
              <h3 className="text-lg font-bold text-ink">
                {isEditing ? 'Cập Nhật Khách Hàng' : 'Thêm Khách Hàng Mới'}
              </h3>
              <button
                onClick={handleCloseModal}
                className="text-ink-lighter hover:text-ink-subtle hover:bg-slate-100 p-1 rounded-md transition-colors"
              >
                <X size={20} />
              </button>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-ink mb-1">Mã KH</label>
                <input
                  data-testid="customer-code"
                  type="text"
                  value={formData.customerCode || formData.code || ''}
                  onChange={(e) => setFormData({ ...formData, customerCode: e.target.value, code: e.target.value })}
                  className="w-full px-3 py-2 bg-app border border-line rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="Tự động nếu để trống"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-ink mb-1">Tên KH <span className="text-danger">*</span></label>
                <input
                  data-testid="customer-name"
                  type="text"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 bg-app border border-line rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="VD: Nguyễn Văn A"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-ink mb-1">Số Điện Thoại</label>
                <input
                  data-testid="customer-phone"
                  type="text"
                  value={formData.phone || ''}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Email</label>
                <input
                  data-testid="customer-email"
                  type="email"
                  value={formData.email || ''}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Địa chỉ</label>
                <input
                  data-testid="customer-address"
                  type="text"
                  value={formData.address || ''}
                  onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Mã số thuế</label>
                <input
                  data-testid="customer-tax-code"
                  type="text"
                  value={formData.taxCode || ''}
                  onChange={(e) => setFormData({ ...formData, taxCode: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              {isAdmin && (
                <div>
                  <label className="block text-xs font-medium text-ink-subtle mb-1">Chi nhánh</label>
                  <select
                    value={formData.branchId || ''}
                    onChange={(e) => setFormData({ ...formData, branchId: e.target.value ? Number(e.target.value) : undefined })}
                    className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink"
                  >
                    <option value="">Dùng chung (Tất cả chi nhánh)</option>
                    {branches.map((b) => (
                      <option key={b.id} value={b.id}>{b.name}</option>
                    ))}
                  </select>
                </div>
              )}
            </div>
            <div className="flex justify-end gap-3 p-4 border-t border-line bg-app rounded-b-xl">
              <button
                data-testid="customer-cancel"
                onClick={handleCloseModal}
                className="px-4 py-2 text-ink bg-surface border border-line-strong rounded-lg hover:bg-app transition-colors"
              >
                {isAdmin ? 'Hủy' : 'Đóng'}
              </button>
              {isAdmin && (
                <button
                  data-testid="customer-save"
                  onClick={handleSave}
                  disabled={submitting || !formData.name}
                  className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-50"
                >
                  {submitting ? 'Đang lưu...' : 'Lưu lại'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </PageContainer>
  );
};
