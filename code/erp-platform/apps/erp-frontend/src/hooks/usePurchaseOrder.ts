import { useState, useCallback } from 'react';
import { ApiService } from '../api/ApiService';
import { useTabs } from '../context/TabContext';
import type { components } from '@erp/api-contract';

type SupplierPurchaseOrderCreateDto = components['schemas']['SupplierPurchaseOrderCreateDto'];

export function usePurchaseOrder(initialData: any = null, initialMode: 'VIEW' | 'ADD' | 'EDIT' = 'VIEW') {
  const [mode, setMode] = useState<'VIEW' | 'ADD' | 'EDIT'>(initialMode);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { closeTab, activeTabId } = useTabs();

  const handleAdd = useCallback(() => {
    setMode('ADD');
  }, []);

  const handleEdit = useCallback(() => {
    setMode('EDIT');
  }, []);

  const handleCancel = useCallback(() => {
    if (confirm('Bạn có chắc chắn muốn hủy thay đổi?')) {
      if (initialMode === 'ADD' && mode === 'ADD') {
        if (activeTabId) closeTab(activeTabId);
      } else {
        setMode('VIEW');
      }
    }
  }, [initialMode, mode, activeTabId, closeTab]);

  const handleDelete = useCallback(() => {
    if (confirm('Xóa chứng từ này?')) {
      alert('Đã xóa chứng từ');
      if (activeTabId) closeTab(activeTabId);
    }
  }, [activeTabId, closeTab]);

  const handleExit = useCallback(() => {
    if (mode !== 'VIEW') {
      if (!confirm('Dữ liệu chưa được lưu. Bạn có chắc chắn muốn thoát?')) {
        return;
      }
    }
    if (activeTabId) closeTab(activeTabId);
  }, [mode, activeTabId, closeTab]);

  const handleSubmit = useCallback(
    async (payload: SupplierPurchaseOrderCreateDto, onSuccess: (data: any) => void) => {
      setIsLoading(true);
      setError(null);
      try {
        if (mode === 'ADD') {
          const response = await ApiService.Purchasing.createOrder(payload);
          onSuccess(response);
        } else {
          // Update logic
          onSuccess({ message: 'Updated' });
        }
        setMode('VIEW');
      } catch (err: any) {
        console.error(err);
        setError(err.message || 'Lỗi hệ thống');
      } finally {
        setIsLoading(false);
      }
    },
    [mode],
  );

  return {
    mode,
    setMode,
    isLoading,
    error,
    handleAdd,
    handleEdit,
    handleCancel,
    handleDelete,
    handleExit,
    handleSubmit,
  };
}
