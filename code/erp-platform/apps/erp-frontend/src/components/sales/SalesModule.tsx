import React, { useRef, useState } from 'react';
import { MdiModuleLayout, type SubViewType, type FormMode } from '../layout/MdiModuleLayout';
import { SalesOrderForm, type SalesOrderFormRef } from './SalesOrderForm';
import { SalesList } from './SalesList';
import { ApiService } from '../../api/ApiService';
import toast from 'react-hot-toast';

interface SalesModuleProps {
  initialSubView?: SubViewType;
  mode?: FormMode;
  initialData?: any;
}

export const SalesModule: React.FC<SalesModuleProps> = ({
  initialSubView = 'FORM',
  mode: initialMode = 'VIEW',
  initialData,
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const formRef = useRef<SalesOrderFormRef>(null);
  const [formData, setFormData] = useState<any>(initialData);

  const handleStateChange = (mode: FormMode, loading: boolean) => {
    setCurrentMode(mode);
    setIsLoading(loading);
  };

  const handleRowDoubleClick = async (orderId: string) => {
    try {
      setIsLoading(true);
      const res = await ApiService.SalesInvoice.getById(orderId);
      setFormData(res || {}); // Depending on whether backend returns data inside data
      setCurrentMode('VIEW');
      setActiveSubView('FORM');
    } catch (err: any) {
      console.error(err);
      toast.error('Không thể tải chi tiết đơn hàng: ' + err.message);
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
        <SalesOrderForm ref={formRef} mode={currentMode} initialData={formData} onStateChange={handleStateChange} />
      ) : (
        <div className="p-4 h-full">
          <SalesList setActiveTab={() => {}} onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
