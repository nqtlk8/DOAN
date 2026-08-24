import React, { useState } from 'react';
import { MdiModuleLayout } from '../../layout/MdiModuleLayout';
import { PurchaseOrderFormUI } from './PurchaseOrderFormUI';
import { usePurchaseOrder } from '../../hooks/usePurchaseOrder';

export const PurchaseOrderContainer = ({ initialData }: { initialData?: any }) => {
  const {
    mode,
    isLoading,
    error,
    handleAdd,
    handleEdit,
    handleCancel,
    handleDelete,
    handleExit,
    handleSubmit
  } = usePurchaseOrder(initialData, initialData ? 'VIEW' : 'ADD');

  // Local state for form fields (managed by container, passed to UI)
  const [items, setItems] = useState<any[]>(initialData?.items || []);
  const [distributorName, setDistributorName] = useState(initialData?.distributorName || '');
  
  // Handlers for UI
  const handleAddItem = () => {
    setItems([...items, { id: Date.now(), quantity: 1, unitPrice: 0 }]);
  };

  const handleSaveClick = () => {
    const payload = {
      distributorName,
      items
    };
    handleSubmit(payload, (res) => {
      alert('Save successful');
    });
  };

  return (
    <MdiModuleLayout
      activeSubView="FORM"
      onSubViewChange={() => {}}
      mode={mode}
      isLoading={isLoading}
      onAdd={handleAdd}
      onEdit={handleEdit}
      onCancel={handleCancel}
      onDelete={handleDelete}
      onExit={handleExit}
      onSave={handleSaveClick}
    >
      <PurchaseOrderFormUI 
        mode={mode}
        error={error}
        distributorName={distributorName}
        items={items}
        onAddItem={handleAddItem}
        // ... pass other handlers
      />
    </MdiModuleLayout>
  );
};
