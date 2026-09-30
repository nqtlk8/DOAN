import { normalizeSearch } from '../../shared/utils/format';
import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Plus, X, Edit2, Trash2 } from 'lucide-react';
import type { Supplier } from '../../types/catalog';
import { useSuppliers } from '../../hooks/useSuppliers';
import { useAuth } from '../../context/AuthContext';
import { ListTable } from '../../shared/components/DataState/ListTable';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';

export const SupplierList: React.FC = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [confirmState, setConfirmState] = useState({ isOpen: false, id: '' });

  const [formData, setFormData] = useState<Partial<Supplier>>({});
  const [isEditing, setIsEditing] = useState(false);

  const { query, createMutation, updateMutation, deleteMutation } = useSuppliers();
  const { data: response, isLoading: loading, isError, error, refetch } = query;

  const suppliers: Supplier[] = (response as any) || [];

  const handleOpenModal = (supplier?: Supplier) => {
    if (supplier) {
      setIsEditing(true);
      setFormData(supplier);
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
      code: formData.code || `SUP-${Date.now()}`,
      name: formData.name,
      phone: formData.phone,
      email: formData.email,
      address: formData.address,
      taxCode: formData.taxCode,
    };
    if (isEditing && formData.id) {
      updateMutation.mutate({ id: String(formData.id), payload }, { onSuccess: handleCloseModal });
    } else {
      createMutation.mutate(payload, { onSuccess: handleCloseModal });
    }
  };

  const handleDelete = (id: string) => {
    setConfirmState({ isOpen: true, id });
  };

  const submitting = createMutation.isPending || updateMutation.isPending;

  const term = normalizeSearch(searchTerm);
  const filtered = suppliers.filter(
    (d) =>
      d.isActive !== false &&
      (normalizeSearch(d.name).includes(term) || normalizeSearch(d.code).includes(term) || (d.phone ?? '').includes(searchTerm.trim())),
  );

  return (
    <PageContainer data-testid="supplier-page">
      <PageHeader 
        title="Danh Mục Nhà Phân Phối"
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
              <button onClick={() => handleOpenModal()} className="btn btn-primary h-8">
                <Plus size={16} className="mr-1" />
                Thêm mới
              </button>
            )}
          </div>
        }
      />

      <ListTable
        colCount={5}
        isLoading={loading}
        isError={isError}
        error={error}
        onRetry={refetch}
        totalCount={suppliers.filter((d) => d.isActive !== false).length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có nhà phân phối"
        emptyMessage="Hệ thống chưa có nhà phân phối nào."
        header={
          <tr>
            <th className="w-16">ID</th>
            <th className="w-36">Mã NPP</th>
            <th>Tên nhà phân phối</th>
            <th className="w-36">Số điện thoại</th>
            <th className="w-24 text-right">Thao tác</th>
          </tr>
        }
      >
        {filtered.map((d) => (
          <tr key={d.id} data-testid="supplier-row">
            <td className="text-ink-subtle truncate max-w-[80px]" title={String(d.id)}>{String(d.id).slice(0, 8)}</td>
            <td>{d.code || '—'}</td>
            <td className="font-medium">{d.name}</td>
            <td>{d.phone || '—'}</td>
            <td className="text-right whitespace-nowrap">
                  <button type="button" onClick={() => handleOpenModal(d)} className="btn btn-ghost h-7 w-7 px-0 hover:text-primary" title={isAdmin ? 'Sửa' : 'Xem chi tiết'} aria-label={isAdmin ? 'Sửa' : 'Xem chi tiết'}>
                    <Edit2 size={15} />
                  </button>
                  {isAdmin && (
                    <button type="button" onClick={() => handleDelete(String(d.id))} className="btn btn-ghost h-7 w-7 px-0 hover:text-danger" title="Xóa" aria-label="Xóa">
                      <Trash2 size={15} />
                    </button>
                  )}
                </td>
          </tr>
        ))}
      </ListTable>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-surface rounded-xl shadow-xl w-full max-w-md">
            <div className="flex justify-between items-center p-4 border-b border-line">
              <h3 className="text-lg font-bold text-ink">{isEditing ? 'Cập nhật' : 'Thêm mới'} Nhà Phân Phối</h3>
              <button onClick={handleCloseModal} className="text-ink hover:text-ink-muted">
                <X size={24} />
              </button>
            </div>
            <div className="p-4 space-y-4 max-h-[70vh] overflow-y-auto">
              {isEditing && (
                <div>
                  <label className="block text-xs font-medium text-ink-subtle mb-1">Mã nhà phân phối</label>
                  <input
                    type="text"
                    value={formData.code || ''}
                    disabled={true}
                    className="w-full px-3 py-2 border border-line-strong rounded-lg bg-slate-100 text-ink-subtle cursor-not-allowed"
                  />
                </div>
              )}
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Tên Nhà Phân Phối *</label>
                <input
                  type="text"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Số điện thoại</label>
                <input
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
                  type="text"
                  value={formData.taxCode || ''}
                  onChange={(e) => setFormData({ ...formData, taxCode: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 p-4 border-t border-line bg-app rounded-b-xl">
              <button
                onClick={handleCloseModal}
                className="px-4 py-2 text-ink bg-surface border border-line-strong rounded-lg hover:bg-app transition-colors"
              >
                {isAdmin ? 'Hủy' : 'Đóng'}
              </button>
              {isAdmin && (
                <button
                  onClick={handleSave}
                  disabled={submitting || !formData.name}
                  className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-hover transition-colors disabled:opacity-50"
                >
                  {submitting ? 'Đang lưu...' : 'Lưu lại'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
      <ConfirmDialog
        isOpen={confirmState.isOpen}
        title="Xóa nhà phân phối"
        message="Bạn có chắc chắn muốn xóa nhà phân phối này không? Hành động này không thể hoàn tác."
        onConfirm={async () => {
          await deleteMutation.mutateAsync(confirmState.id);
          setConfirmState({ isOpen: false, id: '' });
        }}
        onCancel={() => setConfirmState({ isOpen: false, id: '' })}
      />
    </PageContainer>
  );
};
