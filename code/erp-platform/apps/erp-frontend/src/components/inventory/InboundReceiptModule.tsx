import React, { useCallback, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../shared/errors/errorMessage';
import { MdiModuleLayout, type SubViewType } from '../layout/MdiModuleLayout';
import { InboundReceiptForm, type InboundReceiptFormRef } from './InboundReceiptForm';
import { InboundReceiptList } from './InboundReceiptList';
import { ApiService } from '../../api/ApiService';
import type { DocStatus, FormMode, InboundReceiptDto } from '../../types/documents';

interface InboundReceiptModuleProps {
  initialSubView?: SubViewType;
  mode?: FormMode;
  initialData?: InboundReceiptDto | null;
}

export const InboundReceiptModule: React.FC<InboundReceiptModuleProps> = ({
  initialSubView = 'FORM',
  mode: initialMode = 'ADD',
  initialData,
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [status, setStatus] = useState<DocStatus | undefined>(initialData?.status);
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState<InboundReceiptDto | null | undefined>(initialData);
  const [formKey, setFormKey] = useState(0);
  const formRef = useRef<InboundReceiptFormRef>(null);

  const handleStateChange = useCallback((mode: FormMode, loading: boolean, st?: DocStatus) => {
    setCurrentMode(mode);
    setIsLoading(loading);
    setStatus(st);
  }, []);

  const handleRowDoubleClick = async (receiptId: string) => {
    try {
      setIsLoading(true);
      const res = await ApiService.InboundReceipt.getById(receiptId);
      setFormData(res ?? null);
      setCurrentMode('VIEW');
      setFormKey((k) => k + 1);
      setActiveSubView('FORM');
    } catch (err) {
      toast.error('Không tải được chi tiết phiếu nhập: ' + errorMessage(err, ''));
    } finally {
      setIsLoading(false);
    }
  };

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
      onExit={() => formRef.current?.handleExit()}
    >
      {/* Giữ form luôn mounted để chuyển qua "Danh sách phiếu" rồi quay lại không mất dữ liệu đang nhập */}
      <div className={activeSubView === 'FORM' ? 'h-full' : 'hidden'}>
        <InboundReceiptForm
          key={`${formData?.id ?? 'new'}-${formKey}`}
          ref={formRef}
          mode={formData?.id ? 'VIEW' : initialMode}
          initialData={formData}
          onStateChange={handleStateChange}
        />
      </div>
      {activeSubView === 'LIST' && (
        <div className="p-4 h-full">
          <InboundReceiptList onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
