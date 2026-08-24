import React, { useState, useEffect } from 'react';
import { Search, Plus, X, Edit2, Trash2 } from 'lucide-react';
import { CatalogService } from '../../services/catalogService';
import type { Product } from '../../types/catalog';

export const ProductList: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState<Partial<Product>>({});
  const [isEditing, setIsEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await CatalogService.getProducts();
      setProducts(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

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

  const handleSave = async () => {
    setSubmitting(true);
    try {
      if (isEditing && formData.id) {
        await CatalogService.updateProduct(formData.id, formData);
      } else {
        await CatalogService.createProduct(formData);
      }
      handleCloseModal();
      fetchProducts();
    } catch (error) {
      console.error('Failed to save product', error);
      alert('Có lỗi xảy ra khi lưu sản phẩm.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Bạn có chắc muốn xóa sản phẩm này?')) {
      try {
        await CatalogService.deleteProduct(id);
        fetchProducts();
      } catch (error: any) {
        console.error('Failed to delete product', error);
        alert(error?.response?.data?.message || 'Không thể xóa do đã có giao dịch hoặc lỗi mạng.');
      }
    }
  };

  const filtered = products.filter(p => p.name?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Danh Mục Sản Phẩm</h2>
        <button 
          onClick={() => handleOpenModal()}
          className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={20} />
          <span>Thêm mới</span>
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-4 border-b border-slate-100">
          <div className="relative w-72">
            <input 
              type="text" 
              placeholder="Tìm kiếm sản phẩm..." 
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900"
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500 text-right">Thao tác</th>
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
              filtered.map(p => (
                <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-1.5 text-sm text-slate-900">{p.id}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{p.sku || p.code || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900 font-medium">{p.name}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{(p.price ?? p.basePrice)?.toLocaleString() || 0} đ</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{p.unit || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900 text-right">
                    <button onClick={() => handleOpenModal(p)} className="text-blue-600 hover:text-blue-800 mr-3">
                      <Edit2 size={18} />
                    </button>
                    <button onClick={() => handleDelete(p.id)} className="text-red-600 hover:text-red-800">
                      <Trash2 size={18} />
                    </button>
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
                  onChange={e => setFormData({...formData, name: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900" 
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Đơn giá bán</label>
                <input 
                  type="number" 
                  value={formData.price ?? formData.basePrice ?? ''}
                  onChange={e => setFormData({...formData, price: Number(e.target.value)})}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900" 
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Giá vốn (Cost)</label>
                <input 
                  type="number" 
                  value={formData.cost ?? ''}
                  onChange={e => setFormData({...formData, cost: Number(e.target.value)})}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900" 
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Đơn vị tính</label>
                <input 
                  type="text" 
                  value={formData.unit || ''}
                  onChange={e => setFormData({...formData, unit: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900" 
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Danh mục (Category)</label>
                <input 
                  type="text" 
                  value={formData.category || ''}
                  onChange={e => setFormData({...formData, category: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900" 
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Mô tả</label>
                <textarea 
                  value={formData.description || ''}
                  onChange={e => setFormData({...formData, description: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900"
                  rows={3}
                />
              </div>
            </div>
            <div className="p-4 border-t border-slate-100 flex justify-end gap-2">
              <button onClick={handleCloseModal} className="px-4 py-2 text-slate-900 bg-slate-100 rounded-lg hover:bg-slate-200">Hủy</button>
              <button 
                onClick={handleSave} 
                disabled={submitting || !formData.name}
                className="px-4 py-2 text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {submitting ? 'Đang lưu...' : 'Lưu'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
