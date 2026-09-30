import { PageHeader } from '../../shared/components/Page/PageHeader';
import { PageContainer } from '../../shared/components/Page/PageContainer';
import React, { useState } from 'react';
import { Search, Plus, X, Edit2, Trash2 } from 'lucide-react';
import type { Product } from '../../types/catalog';
import { useAuth, ROLES } from '../../context/AuthContext';
import { ListTable } from '../../shared/components/DataState/ListTable';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { StockMovementList } from '../inventory/StockMovementList';
import { useProducts } from '../../hooks/useProducts';
import { formatNumber, normalizeSearch } from '../../shared/utils/format';

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

  const term = normalizeSearch(searchTerm);
  const filtered = products.filter(
    (p) => p.isActive !== false && (normalizeSearch(p.name).includes(term) || normalizeSearch(p.code).includes(term)),
  );

  return (
    <PageContainer data-testid="product-page">
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
              <button onClick={() => handleOpenModal()} className="btn btn-primary h-8">
                <Plus size={16} className="mr-1" />
                Thêm mới
              </button>
            )}
          </div>
        }
      />

      <ListTable
        colCount={6}
        isLoading={loading}
        isError={isError}
        error={error}
        onRetry={refetch}
        totalCount={products.length}
        filteredCount={filtered.length}
        searchTerm={searchTerm}
        emptyTitle="Chưa có sản phẩm"
        emptyMessage="Hệ thống chưa có sản phẩm nào."
        header={
          <tr>
            <th className="w-16">ID</th>
            <th className="w-36">Mã (SKU)</th>
            <th>Tên sản phẩm</th>
            <th className="num w-36">Đơn giá</th>
            <th className="w-24">Đơn vị</th>
            <th className="w-24 text-right">Thao tác</th>
          </tr>
        }
      >
        {filtered.map((p) => (
          <tr key={p.id} data-testid="product-row">
            <td className="text-ink-subtle">{p.id}</td>
            <td>{p.code || '—'}</td>
            <td className="font-medium">{p.name}</td>
            <td className="num">{formatNumber(p.price ?? 0)}</td>
            <td>{p.baseUnit || '—'}</td>
            <td className="text-right whitespace-nowrap">
                  <button type="button" onClick={() => handleOpenModal(p)} className="btn btn-ghost h-7 w-7 px-0 hover:text-primary" title={isAdmin ? 'Sửa' : 'Xem chi tiết'} aria-label={isAdmin ? 'Sửa' : 'Xem chi tiết'}>
                    <Edit2 size={15} />
                  </button>
                  {isAdmin && (
                    <button type="button" onClick={() => setConfirmState({ isOpen: true, id: String(p.id) })} className="btn btn-ghost h-7 w-7 px-0 hover:text-danger" title="Xóa" aria-label="Xóa">
                      <Trash2 size={15} />
                    </button>
                  )}
                </td>
          </tr>
        ))}
      </ListTable>

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
