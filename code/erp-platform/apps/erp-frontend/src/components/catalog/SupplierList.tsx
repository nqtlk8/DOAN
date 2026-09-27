import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Plus, X, Edit2, Trash2 } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import type { Supplier } from '../../types/catalog';
import { useSuppliers } from '../../hooks/useSuppliers';
import { useAuth } from '../../context/AuthContext';
import { DataState } from '../../shared/components/DataState/DataState';
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

  const filtered = suppliers.filter((d) => d.isActive !== false && d.name?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <PageContainer>
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
              <button onClick={() => handleOpenModal()} className="btn-primary h-8 px-3 flex items-center">
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">ID</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Mã NPP</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">
                Tên Nhà Phân Phối
              </th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Số Điện Thoại</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle text-right">
                Thao tác
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={5} className="p-0">
                <DataState
                  isLoading={loading}
                  isError={isError}
                  error={error}
                  isEmpty={suppliers.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có nhà phân phối"
                  emptyMessage="Hệ thống chưa có nhà phân phối nào."
                  emptyAction={
                    isAdmin && (
                      <button
                        onClick={() => handleOpenModal()}
                        className="flex items-center gap-2 bg-primary text-white px-4 py-2 rounded-lg hover:bg-primary-dark transition-colors mt-2"
                      >
                        <Plus size={20} />
                        <span>Thêm mới</span>
                      </button>
                    )
                  }
                >
                  {suppliers.length > 0 && filtered.length === 0 ? (
                    <div className="p-8 text-center text-ink-subtle bg-surface">
                      Không tìm thấy kết quả nào phù hợp với "{searchTerm}"
                    </div>
                  ) : null}
                </DataState>
              </td>
            </tr>
            {!loading && !isError && filtered.length > 0 && (
              filtered.map((d) => (
                <tr key={d.id} className="border-b border-slate-50 hover:bg-app transition-colors">
                  <td className="px-4 py-1.5 text-sm text-ink">{d.id}</td>
                  <td className="px-4 py-1.5 text-sm text-ink">{d.code || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-ink font-medium">{d.name}</td>
                  <td className="px-4 py-1.5 text-sm text-ink">{d.phone || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-ink-subtle text-right space-x-2">
                    <button
                      onClick={() => handleOpenModal(d)}
                      className="text-ink-lighter hover:text-primary transition-colors"
                      title={isAdmin ? "Sửa" : "Xem chi tiết"}
                    >
                      <Edit2 size={16} />
                    </button>
                    {isAdmin && (
                      <button
                        onClick={() => handleDelete(String(d.id))}
                        className="text-ink-lighter hover:text-danger transition-colors"
                        title="Xóa"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        </DataState>
      </div>

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
                  className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-50"
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
