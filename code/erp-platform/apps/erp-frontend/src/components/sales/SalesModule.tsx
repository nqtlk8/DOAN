import React, { useCallback, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../shared/errors/errorMessage';
import { MdiModuleLayout, type SubViewType } from '../layout/MdiModuleLayout';
import { SalesOrderForm, type SalesOrderFormRef } from './SalesOrderForm';
import { SalesList } from './SalesList';
import { ApiService } from '../../api/ApiService';
import type { DocStatus, FormMode, SalesInvoiceDto } from '../../types/documents';

interface SalesModuleProps {
  initialSubView?: SubViewType;
  mode?: FormMode;
  initialData?: SalesInvoiceDto | null;
}

export const SalesModule: React.FC<SalesModuleProps> = ({
  initialSubView = 'FORM',
  mode: initialMode = 'VIEW',
  initialData,
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [status, setStatus] = useState<DocStatus | undefined>(initialData?.status);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const formRef = useRef<SalesOrderFormRef>(null);
  const [formData, setFormData] = useState<SalesInvoiceDto | null | undefined>(initialData);
  const [formKey, setFormKey] = useState(0);

  const handleStateChange = useCallback((mode: FormMode, loading: boolean, st?: DocStatus) => {
    setCurrentMode(mode);
    setIsLoading(loading);
    setStatus(st);
  }, []);

  const handleRowDoubleClick = async (orderId: string) => {
    try {
      setIsLoading(true);
      const res = await ApiService.SalesInvoice.getById(orderId);
      setFormData(res ?? null);
      setCurrentMode('VIEW');
      setFormKey((k) => k + 1);
      setActiveSubView('FORM');
    } catch (err) {
      console.error(err);
      toast.error('Không thể tải chi tiết đơn hàng: ' + errorMessage(err, ''));
    } finally {
      setIsLoading(false);
    }
  };

  // Backend chưa có API sửa / xóa hóa đơn → không hiển thị nút Sửa, Xóa (tránh thao tác "giả").
  return (
    <MdiModuleLayout
      activeSubView={activeSubView}
      onSubViewChange={setActiveSubView}
      mode={currentMode}
      isLoading={isLoading}
      hideConfirm={status !== 'DRAFT'}
      onAdd={() => formRef.current?.handleAdd()}
      onSave={() => formRef.current?.handleSubmit()}
      onCancel={() => formRef.current?.handleCancel()}
      onConfirm={() => formRef.current?.handleConfirm()}
      onPrint={() => formRef.current?.handlePrint()}
      onExit={() => formRef.current?.handleExit()}
    >
      {/* Giữ form luôn mounted: chuyển qua "Danh sách phiếu" rồi quay lại không mất dữ liệu đang nhập */}
      <div className={activeSubView === 'FORM' ? 'h-full' : 'hidden'}>
        <SalesOrderForm
          // key: remount khi mở đơn khác để form đọc lại initialData (useState chỉ đọc lần đầu)
          key={`${formData?.id ?? 'new'}-${formKey}`}
          ref={formRef}
          mode={formData?.id ? 'VIEW' : initialMode}
          initialData={formData}
          onStateChange={handleStateChange}
        />
      </div>
      {activeSubView === 'LIST' && (
        <div className="p-4 h-full">
          <SalesList setActiveTab={() => {}} onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
