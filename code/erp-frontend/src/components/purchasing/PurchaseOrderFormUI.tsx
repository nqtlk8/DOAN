import React from 'react';
// import { GenericDocumentForm } from '../common/document/GenericDocumentForm';

// This is the dumb component. It only receives data and callbacks via props.
export const PurchaseOrderFormUI = ({
  mode,
  orderCode,
  creator,
  branch,
  createdDate,
  note,
  distributorName,
  address,
  phone,
  contactPerson,
  oldDebt,
  totalAmount,
  discount,
  tax,
  advancePayment,
  remainingBalance,
  items,
  error,
  // Callbacks
  setCreatedDate,
  setNote,
  setAddress,
  setPhone,
  setContactPerson,
  setOldDebt,
  setDiscount,
  setTax,
  setAdvancePayment,
  onPartnerSearch,
  onProductSearch,
  onAddItem,
  onRemoveItem,
  onUpdateItem,
}: any) => {
  return (
    <div className="flex flex-col h-full bg-white relative">
      <div className="p-4 bg-slate-50 border-b">
        <h2 className="text-xl font-bold">{mode === 'ADD' ? 'Thêm mới PO' : `PO: ${orderCode}`}</h2>
        {error && <div className="text-red-500 mt-2">{error}</div>}
      </div>
      {/* 
        Here we would render GenericDocumentForm passing down the props. 
        For brevity, we skip pasting the whole GenericDocumentForm.
      */}
      <div className="p-4">
        {/* Render Form Columns (Internal, Partner, Financial) */}
        <div>NPP: {distributorName || 'Chưa chọn'}</div>
        <div>Tổng tiền: {totalAmount}</div>
        <button className="px-4 py-2 bg-blue-600 text-white rounded mt-4" onClick={onAddItem} disabled={mode === 'VIEW'}>Thêm sản phẩm</button>
        {/* Render Items Table */}
        <ul>
          {items.map((item: any) => (
            <li key={item.id}>{item.productName || 'Chọn SP'} - Qty: {item.quantity} - {item.unitPrice}</li>
          ))}
        </ul>
      </div>
    </div>
  );
};
