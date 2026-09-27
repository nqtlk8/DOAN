import React, { useState, useEffect, useRef } from 'react';
import { QuickCreateDialog } from './QuickCreateDialog';
import { ApiService } from '../../../api/ApiService';
import { useMutation, useQueryClient } from '@tanstack/react-query';

interface QuickCreateSupplierProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: (supplier: any) => void;
  initialName?: string;
}

export const QuickCreateSupplier: React.FC<QuickCreateSupplierProps> = ({
  isOpen,
  onClose,
  onCreated,
  initialName = ''
}) => {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [error, setError] = useState('');
  
  const queryClient = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setName(initialName);
      setPhone('');
      setAddress('');
      setError('');
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [isOpen, initialName]);

  const createMutation = useMutation({
    mutationFn: (data: any) => ApiService.Catalog.createSupplier(data),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      onCreated(res);
      onClose();
    },
    onError: (err: any) => {
      setError(err.message || 'Lỗi khi tạo nhà cung cấp');
    }
  });

  const handleSave = () => {
    if (!name.trim()) {
      setError('Tên nhà cung cấp là bắt buộc');
      return;
    }
    createMutation.mutate({
      code: 'NCC-' + Date.now(),
      name: name.trim(),
      phone: phone.trim(),
      address: address.trim()
    });
  };

  return (
    <QuickCreateDialog
      isOpen={isOpen}
      onClose={onClose}
      title="Thêm nhanh Nhà cung cấp"
      onSave={handleSave}
      isSaving={createMutation.isPending}
    >
      {error && <div className="text-danger text-[13px]">{error}</div>}
      
      <div className="space-y-3">
        <div>
          <label className="erp-label mb-1 block">Tên nhà cung cấp <span className="text-danger">*</span></label>
          <input
            ref={inputRef}
            type="text"
            className="erp-input"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="Nhập tên nhà cung cấp"
          />
        </div>
        <div>
          <label className="erp-label mb-1 block">Số điện thoại</label>
          <input
            type="text"
            className="erp-input"
            value={phone}
            onChange={e => setPhone(e.target.value)}
            placeholder="Nhập số điện thoại"
          />
        </div>
        <div>
          <label className="erp-label mb-1 block">Địa chỉ</label>
          <input
            type="text"
            className="erp-input"
            value={address}
            onChange={e => setAddress(e.target.value)}
            placeholder="Nhập địa chỉ"
          />
        </div>
      </div>
    </QuickCreateDialog>
  );
};
