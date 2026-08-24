import React, { useRef, useState } from 'react';
import { MdiModuleLayout, type SubViewType, type FormMode } from '../layout/MdiModuleLayout';
import { SalesOrderForm, type SalesOrderFormRef } from './SalesOrderForm';
import { SalesList } from './SalesList';

interface SalesModuleProps {
  initialSubView?: SubViewType;
  mode?: FormMode;
  initialData?: any;
}

export const SalesModule: React.FC<SalesModuleProps> = ({ 
  initialSubView = 'FORM', 
  mode: initialMode = 'VIEW', 
  initialData 
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const formRef = useRef<SalesOrderFormRef>(null);

  const handleStateChange = (mode: FormMode, loading: boolean) => {
    setCurrentMode(mode);
    setIsLoading(loading);
  };

  const handleRowDoubleClick = (orderId: string) => {
    console.log('Double clicked order:', orderId);
    // In a real app, you would fetch data for orderId and pass it to the form.
    // For now, we'll just switch to FORM mode with VIEW state.
    setActiveSubView('FORM');
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
        <SalesOrderForm 
          ref={formRef} 
          mode={initialMode} 
          initialData={initialData} 
          onStateChange={handleStateChange}
        />
      ) : (
        <div className="p-4 h-full">
          <SalesList setActiveTab={() => {}} onRowDoubleClick={handleRowDoubleClick} />
        </div>
      )}
    </MdiModuleLayout>
  );
};
