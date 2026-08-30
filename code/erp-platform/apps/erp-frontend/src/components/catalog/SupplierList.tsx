import React, { useState } from 'react';
import { Search, Plus, X, Edit2, Trash2 } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import type { Supplier } from '../../types/catalog';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../context/AuthContext';
import { DataState } from '../../shared/components/DataState/DataState';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { notify } from '../../shared/notifications/notification';

export const SupplierList: React.FC = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'admin';
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [confirmState, setConfirmState] = useState({ isOpen: false, id: '' });

  const [formData, setFormData] = useState<Partial<Supplier>>({});
  const [isEditing, setIsEditing] = useState(false);

  const { data: response, isLoading: loading, isError, error, refetch } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => ApiService.Catalog.getSuppliers(),
  });

  const suppliers: Supplier[] = response || [];

  const createMutation = useMutation({
    mutationFn: (data: any) => ApiService.Catalog.createSupplier(data),
    onSuccess: () => {
      notify.success('Đã tạo nhà phân phối thành công');
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      handleCloseModal();
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không thể tạo nhà phân phối');
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: string; payload: any }) => ApiService.Catalog.updateSupplier(data.id, data.payload),
    onSuccess: () => {
      notify.success('Đã cập nhật nhà phân phối thành công');
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      handleCloseModal();
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không thể cập nhật nhà phân phối');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => ApiService.Catalog.deleteSupplier(id),
    onSuccess: () => {
      notify.success('Đã xóa nhà phân phối thành công');
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      setConfirmState({ isOpen: false, id: '' });
    },
    onError: (err: any) => {
      notify.error(err.message || 'Không thể xóa nhà phân phối');
      setConfirmState({ isOpen: false, id: '' });
    }
  });

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
    if (isEditing && formData.id) {
      updateMutation.mutate({ id: formData.id, payload: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleDelete = (id: string) => {
    setConfirmState({ isOpen: true, id });
  };

  const submitting = createMutation.isPending || updateMutation.isPending;

  const filtered = suppliers.filter((d) => d.name?.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Danh Mục Nhà Phân Phối</h2>
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
              placeholder="Tìm kiếm NPP..."
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
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Mã NPP</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">
                Tên Nhà Phân Phối
              </th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Số Điện Thoại</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500">Khu Vực</th>
              <th className="px-4 py-2 text-xs font-medium uppercase tracking-wider text-slate-500 text-right">
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
                  isEmpty={suppliers.length === 0}
                  onRetry={refetch}
                  loadingType="table"
                  emptyTitle="Chưa có nhà phân phối"
                  emptyMessage="Hệ thống chưa có nhà phân phối nào."
                  emptyAction={
                    isAdmin && (
                      <button
                        onClick={() => handleOpenModal()}
                        className="flex items-center gap-2 bg-teal-600 text-white px-4 py-2 rounded-lg hover:bg-teal-700 transition-colors mt-2"
                      >
                        <Plus size={20} />
                        <span>Thêm mới</span>
                      </button>
                    )
                  }
                >
                  {suppliers.length > 0 && filtered.length === 0 ? (
                    <div className="p-8 text-center text-slate-500 bg-white">
                      Không tìm thấy kết quả nào phù hợp với "{searchTerm}"
                    </div>
                  ) : null}
                </DataState>
              </td>
            </tr>
            {!loading && !isError && filtered.length > 0 && (
              filtered.map((d) => (
                <tr key={d.id} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-1.5 text-sm text-slate-900">{d.id}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{d.code || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900 font-medium">{d.name}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{d.phone || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-slate-900">{d.region || '-'}</td>
                  <td className="px-4 py-1.5 text-sm text-right space-x-2">
                    <button
                      onClick={() => handleOpenModal(d)}
                      className="text-slate-400 hover:text-blue-600 transition-colors"
                      title={isAdmin ? "Sửa" : "Xem chi tiết"}
                    >
                      <Edit2 size={16} />
                    </button>
                    {isAdmin && (
                      <button
                        onClick={() => handleDelete(d.id)}
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
              <h3 className="text-lg font-bold text-slate-900">{isEditing ? 'Cập nhật' : 'Thêm mới'} Nhà Phân Phối</h3>
              <button onClick={handleCloseModal} className="text-slate-900 hover:text-slate-600">
                <X size={24} />
              </button>
            </div>
            <div className="p-4 space-y-4 max-h-[70vh] overflow-y-auto">
              {isEditing && (
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1">Mã nhà phân phối</label>
                  <input
                    type="text"
                    value={formData.code || ''}
                    disabled={true}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-100 text-slate-500 cursor-not-allowed"
                  />
                </div>
              )}
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Tên Nhà Phân Phối *</label>
                <input
                  type="text"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Số điện thoại</label>
                <input
                  type="text"
                  value={formData.phone || ''}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Email</label>
                <input
                  type="email"
                  value={formData.email || ''}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Địa chỉ</label>
                <input
                  type="text"
                  value={formData.address || ''}
                  onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Khu vực</label>
                <input
                  type="text"
                  value={formData.region || ''}
                  onChange={(e) => setFormData({ ...formData, region: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1">Mã số thuế</label>
                <input
                  type="text"
                  value={formData.taxCode || ''}
                  onChange={(e) => setFormData({ ...formData, taxCode: e.target.value })}
                  disabled={!isAdmin}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 text-slate-900 disabled:bg-slate-100 disabled:text-slate-500"
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
      <ConfirmDialog
        isOpen={confirmState.isOpen}
        title="Xóa nhà phân phối"
        message="Bạn có chắc chắn muốn xóa nhà phân phối này không? Hành động này không thể hoàn tác."
        onConfirm={async () => {
          await deleteMutation.mutateAsync(confirmState.id);
        }}
        onCancel={() => setConfirmState({ isOpen: false, id: '' })}
      />
    </div>
  );
};
