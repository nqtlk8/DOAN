import React, { useState } from 'react';
import { Search, Plus, X, Edit2, Trash2 } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import type { Product } from '../../types/catalog';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../context/AuthContext';

export const ProductList: React.FC = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'admin';
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const [formData, setFormData] = useState<Partial<Product>>({});
  const [isEditing, setIsEditing] = useState(false);

  const { data: response, isLoading: loading } = useQuery({
    queryKey: ['products'],
    queryFn: () => ApiService.Catalog.getProducts(),
  });

  const products: Product[] = response?.data || [];

  const createMutation = useMutation({
    mutationFn: (data: any) => ApiService.Catalog.createProduct(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      handleCloseModal();
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: string; payload: any }) => ApiService.Catalog.updateProduct(data.id, data.payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      handleCloseModal();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => ApiService.Catalog.deleteProduct(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
  });

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
      updateMutation.mutate({ id: formData.id, payload: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleDelete = (id: string) => {
    if (window.confirm('Bản có chắc muốn xóa sản phẩm này?')) {
      deleteMutation.mutate(id);
    }
  };

  const submitting = createMutation.isPending || updateMutation.isPending;

  const filtered = products.filter((p) => p.name?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Danh Mục Sản Phẩm</h2>
        {isAdmin && (
          <button
            onClick={() => handleOpenModal()}
            className="flex items-center gap-2 bg-teal-600 text-white px-4 py-2 rounded-lg hover:bg-teal-700 transition-colors"
          >
            <Plus size={20} />
            <span>Thêm mới</span>
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-4 border-b border-slate-100">
          <div className="relative w-72">
            <input
              type="text"
              placeholder="Tìm kiếm sản phẩm..."
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">ID</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Mã (SKU)</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Tên Sản Phẩm</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Đơn Giá</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Đơn vị</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500 text-right">
                Thao tác
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-900">
                  Đang tải dữ liệu...
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-900">
                  Không tìm thấy dữ liệu.
                </td>
              </tr>
            ) : (
              filtered.map((p) => (
                <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-1.5 text-sm text-slate-900">{p.id}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{p.sku || p.code || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900 font-medium">{p.name}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">
                    {(p.price ?? p.basePrice)?.toLocaleString() || 0} đ
                  </td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{p.unit || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-right space-x-2">
                    <button
                      onClick={() => handleOpenModal(p)}
                      className="text-slate-400 hover:text-blue-600 transition-colors"
                      title={isAdmin ? "Sửa" : "Xem chi tiết"}
                    >
                      <Edit2 size={16} />
                    </button>
                    {isAdmin && (
                      <button
                        onClick={() => {
                          if (confirm('Xác nhận xóa?')) {
                            deleteMutation.mutate(p.id!);
                          }
                        }}
                        className="text-slate-400 hover:text-red-600 transition-colors"
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
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
            <div className="flex justify-between items-center p-4 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-900">{isEditing ? 'Cập nhật' : 'Thêm mới'} Sản Phẩm</h3>
              <button onClick={handleCloseModal} className="text-slate-900 hover:text-slate-600">
                <X size={24} />
              </button>
            </div>
            <div className="p-4 space-y-4 max-h-[70vh] overflow-y-auto">
              {isEditing && (
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1">Mã sản phẩm (SKU)</label>
                  <input
                    type="text"
                    value={formData.sku || formData.code || ''}
                    disabled={true}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-100 text-slate-500 cursor-not-allowed"
                  />
                </div>
              )}
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Tên sản phẩm *</label>
                <input
                  type="text"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Đơn giá bán</label>
                <input
                  type="number"
                  value={formData.price ?? formData.basePrice ?? ''}
                  onChange={(e) => setFormData({ ...formData, price: Number(e.target.value) })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Giá vốn (Cost)</label>
                <input
                  type="number"
                  value={formData.cost ?? ''}
                  onChange={(e) => setFormData({ ...formData, cost: Number(e.target.value) })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Đơn vị tính</label>
                <input
                  type="text"
                  value={formData.unit || ''}
                  onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Danh mục (Category)</label>
                <input
                  type="text"
                  value={formData.category || ''}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Mô tả</label>
                <textarea
                  value={formData.description || ''}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                  rows={3}
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 p-4 border-t border-slate-100 bg-slate-50 rounded-b-xl">
              <button
                onClick={handleCloseModal}
                className="px-4 py-2 text-slate-900 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
              >
                {isAdmin ? 'Hủy' : 'Đóng'}
              </button>
              {isAdmin && (
                <button
                  onClick={handleSave}
                  disabled={submitting || !formData.name}
                  className="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 transition-colors disabled:opacity-50"
                >
                  {submitting ? 'Đang lưu...' : 'Lưu lại'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
