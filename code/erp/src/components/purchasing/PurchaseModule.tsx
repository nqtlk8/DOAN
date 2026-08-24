import React, { useRef, useState } from 'react';
import { MdiModuleLayout, type SubViewType, type FormMode } from '../layout/MdiModuleLayout';
import { PurchaseOrderForm, type PurchaseOrderFormRef } from './PurchaseOrderForm';
import { PurchaseOrderList } from './PurchaseOrderList';

interface PurchaseModuleProps {
  initialSubView?: SubViewType;
  mode?: FormMode;
  initialData?: any;
}

export const PurchaseModule: React.FC<PurchaseModuleProps> = ({ 
  initialSubView = 'FORM', 
  mode: initialMode = 'VIEW', 
  initialData 
}) => {
  const [activeSubView, setActiveSubView] = useState<SubViewType>(initialSubView);
  const [currentMode, setCurrentMode] = useState<FormMode>(initialMode);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const formRef = useRef<PurchaseOrderFormRef>(null);

  const handleStateChange = (mode: FormMode, loading: boolean) => {
    setCurrentMode(mode);
    setIsLoading(loading);
  };

  const handleRowDoubleClick = (orderId: string) => {
    console.log('Double clicked order:', orderId);
    // TODO: fetch data for orderId and pass it to the form
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
        <PurchaseOrderForm 
          ref={formRef} 
          mode={initialMode} 
          initialData={initialData} 
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
