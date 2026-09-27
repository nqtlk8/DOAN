import React, { useRef, useState } from 'react';
import { MdiModuleLayout, type SubViewType, type FormMode } from '../layout/MdiModuleLayout';
import { InboundReceiptForm, type InboundReceiptFormRef } from './InboundReceiptForm';
import { ApiService } from '../../api/ApiService';
import toast from 'react-hot-toast';
import { InboundReceiptList } from './InboundReceiptList';

export const InboundReceiptModule: React.FC<{ initialSubView?: SubViewType; mode?: FormMode; initialData?: any }> = ({
  initialSubView = 'FORM',
  mode: initialMode = 'VIEW',
  initialData,
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const formRef = useRef<InboundReceiptFormRef>(null);
  const [formData, setFormData] = useState<any>(initialData);

  const handleStateChange = (mode: FormMode, loading: boolean) => {
    setCurrentMode(mode);
    setIsLoading(loading);
  };

  const handleRowDoubleClick = async (receiptId: string) => {
    try {
      setIsLoading(true);
      const res = await ApiService.InboundReceipt.getById(receiptId);
      setFormData(res || {}); 
      setCurrentMode('VIEW');
      setActiveSubView('FORM');
    } catch (err: any) {
      console.error(err);
      toast.error('Lỗi tải chi tiết phiếu nhập: ' + err.message);
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
      hideConfirm={currentMode === 'VIEW' && formData?.status === 'CONFIRMED'}
      onAdd={() => formRef.current?.handleAdd()}
      onSave={() => formRef.current?.handleSubmit()}
      onCancel={() => formRef.current?.handleCancel()}
      onConfirm={() => formRef.current?.handleConfirm()}
      onExit={() => formRef.current?.handleExit()}
    >
      {activeSubView === 'FORM' ? (
        <InboundReceiptForm
          key={formData?.id ?? 'new'}
          ref={formRef}
          mode={currentMode}
          initialData={formData}
          onStateChange={handleStateChange}
        />
      ) : (
        <div className="p-4 h-full">
          <InboundReceiptList onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
