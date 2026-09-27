import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Plus, X, Edit2, Trash2 } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import type { Product } from '../../types/catalog';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth, ROLES } from '../../context/AuthContext';
import { DataState } from '../../shared/components/DataState/DataState';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { StockMovementList } from '../inventory/StockMovementList';
import { useProducts } from '../../hooks/useProducts';

export const ProductList: React.FC = () => {
  const { hasRole } = useAuth();
  const isAdmin = hasRole(ROLES.ADMIN);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [confirmState, setConfirmState] = useState({ isOpen: false, id: '' });

  const [formData, setFormData] = useState<Partial<Product>>({});
  const [isEditing, setIsEditing] = useState(false);

  const { query, createMutation, updateMutation, deleteMutation } = useProducts();
  const { data: response, isLoading: loading, isError, error, refetch } = query;

  const products: Product[] = (response as any) || [];

  const handleOpenModal = (product?: Product) => {
    if (product) {
      setIsEditing(true);
      setFormData(product);
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
    if (isEditing && formData.id) {
      updateMutation.mutate({
        id: Number(formData.id),
        payload: {
          name: formData.name,
          categoryId: formData.categoryId,
          baseUnit: formData.baseUnit,
          isActive: formData.isActive,
        }
      }, { onSuccess: handleCloseModal });
    } else {
      createMutation.mutate({
        code: formData.code || `PRD-${Date.now()}`,
        name: formData.name,
        categoryId: formData.categoryId || 1,
        baseUnit: formData.baseUnit || 'CAI',
        isActive: formData.isActive ?? true,
      }, { onSuccess: handleCloseModal });
    }
  };


  const submitting = createMutation.isPending;

  const filtered = products.filter((p) => 
    p.isActive !== false && 
    (p.name?.toLowerCase().includes(searchTerm.toLowerCase()) || 
     p.code?.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <PageContainer>
      <PageHeader 
        title="Danh Mục Sản Phẩm"
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Mã (SKU)</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Tên Sản Phẩm</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Đơn Giá</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle">Đơn vị</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-ink-subtle text-right">
                Thao tác
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={6} className="p-0">
                <DataState
                  isLoading={loading}
                  isError={isError}
                  error={error}
                  isEmpty={products.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có sản phẩm"
                  emptyMessage="Hệ thống chưa có sản phẩm nào. Hãy tạo sản phẩm đầu tiên."
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
                  {products.length > 0 && filtered.length === 0 ? (
                    <div className="p-8 text-center text-ink-subtle bg-surface">
                      Không tìm thấy kết quả nào phù hợp với "{searchTerm}"
                    </div>
                  ) : null}
                </DataState>
              </td>
            </tr>
            {!loading && !isError && filtered.length > 0 && (
              filtered.map((p) => (
                <tr key={p.id} className="border-b border-slate-50 hover:bg-app transition-colors">
                  <td className="px-4 py-1.5 text-sm text-ink">{p.id}</td>
                  <td className="px-4 py-1.5 text-sm text-ink">{p.code || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-ink font-medium">{p.name}</td>
                  <td className="px-4 py-1.5 text-sm text-ink">
                    {p.price?.toLocaleString() || 0} đ
                  </td>
                  <td className="px-4 py-1.5 text-sm text-ink">{p.baseUnit || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-right space-x-2">
                    <button
                      onClick={() => handleOpenModal(p)}
                      className="text-ink-lighter hover:text-primary transition-colors"
                      title={isAdmin ? "Sửa" : "Xem chi tiết"}
                    >
                      <Edit2 size={16} />
                    </button>
                    {isAdmin && (
                      <button
                        onClick={() => setConfirmState({ isOpen: true, id: p.id.toString() })}
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
          <div className="bg-surface rounded-xl shadow-xl w-full max-w-3xl">
            <div className="flex justify-between items-center p-4 border-b border-line">
              <h3 className="text-lg font-bold text-ink">{isEditing ? 'Cập nhật' : 'Thêm mới'} Sản Phẩm</h3>
              <button onClick={handleCloseModal} className="text-ink hover:text-ink-muted">
                <X size={24} />
              </button>
            </div>
            <div className="p-4 space-y-4 max-h-[70vh] overflow-y-auto">
              {isEditing && (
                <div>
                  <label className="block text-xs font-medium text-ink-subtle mb-1">Mã sản phẩm</label>
                  <input
                    type="text"
                    value={formData.code || ''}
                    disabled={true}
                    className="w-full px-3 py-2 border border-line-strong rounded-lg bg-slate-100 text-ink-subtle cursor-not-allowed"
                  />
                </div>
              )}
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Tên sản phẩm *</label>
                <input
                  type="text"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              {isEditing && (
                <div>
                  <label className="block text-xs font-medium text-ink-subtle mb-1">Đơn giá bán (Từ Price List)</label>
                  <input
                    type="number"
                    value={formData.price ?? ''}
                    disabled={true}
                    className="w-full px-3 py-2 border border-line-strong rounded-lg bg-slate-100 text-ink-subtle cursor-not-allowed"
                  />
                </div>
              )}
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Đơn vị tính</label>
                <input
                  type="text"
                  value={formData.baseUnit || ''}
                  onChange={(e) => setFormData({ ...formData, baseUnit: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-ink-subtle mb-1">Danh mục (Category ID)</label>
                <input
                  type="number"
                  value={formData.categoryId || ''}
                  onChange={(e) => setFormData({ ...formData, categoryId: Number(e.target.value) })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-line-strong rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-ink disabled:bg-slate-100 disabled:text-ink-subtle"
                />
              </div>
              {isEditing && formData.id && (
                <div className="pt-4 border-t border-line">
                  <StockMovementList productId={formData.id.toString()} />
                </div>
              )}
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
        title="Xóa sản phẩm"
        message="Bạn có chắc chắn muốn xóa sản phẩm này không? Hành động này không thể hoàn tác."
        onConfirm={() => {
          deleteMutation.mutate(Number(confirmState.id), {
            onSuccess: () => setConfirmState({ isOpen: false, id: '' })
          });
        }}
        onCancel={() => setConfirmState({ isOpen: false, id: '' })}
      />
    </PageContainer>
  );
};
