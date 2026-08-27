import React, { useRef, useState } from 'react';
import { MdiModuleLayout, type SubViewType, type FormMode } from '../layout/MdiModuleLayout';
import { PurchaseOrderForm, type PurchaseOrderFormRef } from './PurchaseOrderForm';
import { PurchaseOrderList } from './PurchaseOrderList';
import { ApiService } from '../../api/ApiService';
import toast from 'react-hot-toast';

interface PurchaseModuleProps {
  initialSubView?: SubViewType;
  mode?: FormMode;
  initialData?: any;
}

export const PurchaseModule: React.FC<PurchaseModuleProps> = ({
  initialSubView = 'FORM',
  mode: initialMode = 'VIEW',
  initialData,
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const formRef = useRef<PurchaseOrderFormRef>(null);
  const [formData, setFormData] = useState<any>(initialData);

  const handleStateChange = (mode: FormMode, loading: boolean) => {
    setCurrentMode(mode);
    setIsLoading(loading);
  };

  const handleRowDoubleClick = async (orderId: string) => {
    try {
      setIsLoading(true);
      const res = await ApiService.InboundReceipt.getById(orderId);
      setFormData(res || {});
      setCurrentMode('VIEW');
      setActiveSubView('FORM');
    } catch (err: any) {
      console.error(err);
      toast.error('Không thể tải chi tiết phiếu nhập: ' + err.message);
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
      onAdd={() => formRef.current?.handleAdd()}
      onEdit={() => formRef.current?.handleEdit()}
      onSave={() => formRef.current?.handleSubmit()}
      onCancel={() => formRef.current?.handleCancel()}
      onDelete={() => formRef.current?.handleDelete()}
      onPrint={() => formRef.current?.handlePrint()}
      onExit={() => formRef.current?.handleExit()}
    >
      {activeSubView === 'FORM' ? (
        <PurchaseOrderForm
          ref={formRef}
          mode={currentMode}
          initialData={formData}
          onStateChange={handleStateChange}
        />
      ) : (
        <div className="p-4 h-full">
          <PurchaseOrderList onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
