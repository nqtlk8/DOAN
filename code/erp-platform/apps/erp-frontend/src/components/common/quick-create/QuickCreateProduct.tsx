import React, { useState, useEffect, useRef } from 'react';
import { QuickCreateDialog } from './QuickCreateDialog';
import { ApiService } from '../../../api/ApiService';
import { useMutation, useQueryClient } from '@tanstack/react-query';

interface QuickCreateProductProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: (product: any) => void;
  initialName?: string;
}

export const QuickCreateProduct: React.FC<QuickCreateProductProps> = ({
  isOpen,
  onClose,
  onCreated,
  initialName = ''
}) => {
  const [name, setName] = useState('');
  const [baseUnit, setBaseUnit] = useState('CAI');
  const [error, setError] = useState('');
  
  const queryClient = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setName(initialName);
      setBaseUnit('CAI');
      setError('');
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [isOpen, initialName]);

  const createMutation = useMutation({
    mutationFn: (data: any) => ApiService.Catalog.createProduct(data),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      onCreated(res);
      onClose();
    },
    onError: (err: any) => {
      setError(err.message || 'Lỗi khi tạo sản phẩm');
    }
  });

  const handleSave = () => {
    if (!name.trim()) {
      setError('Tên sản phẩm là bắt buộc');
      return;
    }
    createMutation.mutate({
      code: 'PRD-' + Date.now(),
      name: name.trim(),
      categoryId: 1, // Default category
      baseUnit,
      isActive: true
    });
  };

  return (
    <QuickCreateDialog
      isOpen={isOpen}
      onClose={onClose}
      title="Thêm nhanh Sản phẩm"
      onSave={handleSave}
      isSaving={createMutation.isPending}
    >
      {error && <div className="text-danger text-[13px]">{error}</div>}
      
      <div className="space-y-3">
        <div>
          <label className="erp-label mb-1 block">Tên sản phẩm <span className="text-danger">*</span></label>
          <input
            ref={inputRef}
            type="text"
            className="erp-input"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="Nhập tên sản phẩm"
          />
        </div>
        <div>
          <label className="erp-label mb-1 block">Đơn vị tính</label>
          <select
            className="erp-input bg-surface"
            value={baseUnit}
            onChange={e => setBaseUnit(e.target.value)}
          >
            <option value="CAI">Cái</option>
            <option value="HOP">Hộp</option>
            <option value="THUNG">Thùng</option>
            <option value="KG">Kg</option>
          </select>
        </div>
      </div>
    </QuickCreateDialog>
  );
};
