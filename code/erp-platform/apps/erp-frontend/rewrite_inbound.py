import os

form_content = '''import { useState, useEffect, useMemo, forwardRef, useImperativeHandle } from 'react';
import { ApiService } from '../../api/ApiService';
import { useAuth } from '../../context/AuthContext';
import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';
import { QuickCreateSupplier } from '../common/quick-create/QuickCreateSupplier';
import { QuickCreateProduct } from '../common/quick-create/QuickCreateProduct';
import { SearchableCombobox } from '../common/SearchableCombobox';
import toast from 'react-hot-toast';
import { SearchModal } from '../common/SearchModal';
import type { Supplier } from '../../types/catalog';

export type FormMode = 'VIEW' | 'ADD' | 'EDIT';

export interface InboundReceiptFormProps {
  mode?: FormMode;
  initialData?: any;
  onStateChange?: (mode: FormMode, isLoading: boolean) => void;
}

export interface InboundReceiptFormRef {
  handleAdd: () => void;
  handleSubmit: () => Promise<void>;
  handleCancel: () => void;
  handleConfirm: () => Promise<void>;
  handleExit: () => void;
}

export const InboundReceiptForm = forwardRef<InboundReceiptFormRef, InboundReceiptFormProps>(
  ({ mode = 'ADD', initialData, onStateChange }, ref) => {
    const { user } = useAuth();
    
    // UI states
    const [currentMode, setCurrentMode] = useState<FormMode>(mode);
    const [isLoading, setIsLoading] = useState(false);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    
    // Dialogs
    const [showSupplierSearch, setShowSupplierSearch] = useState(false);
    const [showProductSearch, setShowProductSearch] = useState(false);
    const [activeLineIndex, setActiveLineIndex] = useState<number>(-1);

    // Form data states
    const [receiptId, setReceiptId] = useState(initialData?.id || '');
    const [receiptCode, setReceiptCode] = useState(initialData?.receiptCode || '');
    const [status, setStatus] = useState<'DRAFT' | 'CONFIRMED' | 'CANCELLED' | ''>(initialData?.status || (receiptId ? 'DRAFT' : ''));
    const [createdDate, setCreatedDate] = useState(initialData?.createdAt || new Date().toISOString());
    const [branch] = useState(initialData?.branch || 'CN Trung t\u00e2m');
    const creator = initialData?.creator || user?.username || 'Admin';

    const [supplierId, setSupplierId] = useState<string>(initialData?.supplierId || '');
    const [supplierName, setSupplierName] = useState<string>(initialData?.supplierName || '');
    const [supplierPhone, setSupplierPhone] = useState<string>(initialData?.supplierPhone || '');
    const [supplierAddress, setSupplierAddress] = useState<string>(initialData?.supplierAddress || '');
    const [note, setNote] = useState<string>(initialData?.note || '');

    const [lines, setLines] = useState<OrderItem[]>(() => {
      if (initialData?.lines && Array.isArray(initialData.lines)) {
        return initialData.lines.map((l: any, i: number) => ({
          id: line_,
          productId: l.productId?.toString() || '',
          productCode: l.productCode || '',
          productName: l.productName || '',
          unitOfMeasure: l.unitOfMeasure || 'CAI',
          quantity: l.quantity || 1,
          unitPrice: l.unitCost || l.unitPrice || 0
        }));
      }
      return [];
    });

    useEffect(() => {
      onStateChange?.(currentMode, isLoading);
    }, [currentMode, isLoading, onStateChange]);

    useImperativeHandle(ref, () => ({
      handleAdd: () => {
        setCurrentMode('ADD');
        setReceiptId('');
        setReceiptCode('');
        setStatus('');
        setSupplierId('');
        setSupplierName('');
        setSupplierPhone('');
        setSupplierAddress('');
        setNote('');
        setLines([]);
        setFieldErrors({});
      },
      handleSubmit: async () => {
        if (!validateForm()) return;
        setIsLoading(true);
        try {
          const payload = {
            supplierId,
            note,
            lines: lines.map(l => ({
              productId: Number(l.productId),
              quantity: l.quantity,
              unitCost: l.unitPrice,
              unitOfMeasure: l.unitOfMeasure
            }))
          };
          
          let resultId = receiptId;
          
          if (currentMode === 'ADD') {
            const res = await ApiService.InboundReceipt.create(payload as any);
            resultId = res.id;
            toast.success('L\u01b0u phi\u1ebfu nh\u1eadp th\u00e0nh c\u00f4ng');
          } else {
            // Edit API does not exist according to specs
          }

          if (resultId) {
            const freshData = await ApiService.InboundReceipt.getById(resultId);
            setReceiptId(freshData.id as string);
            setReceiptCode(freshData.receiptCode as string);
            setStatus((freshData as any).status || 'DRAFT');
            setCurrentMode('VIEW');
          }
        } catch (error: any) {
          toast.error(error.message || 'C\u00f3 l\u1ed7i x\u1ea3y ra');
        } finally {
          setIsLoading(false);
        }
      },
      handleCancel: () => {
        if (receiptId) {
          setCurrentMode('VIEW');
        } else {
          // close tab logic in parent
        }
      },
      handleConfirm: async () => {
        if (!receiptId) return;
        setIsLoading(true);
        try {
          await ApiService.InboundReceipt.confirm(receiptId);
          toast.success('X\u00e1c nh\u1eadn phi\u1ebfu nh\u1eadp th\u00e0nh c\u00f4ng');
          setStatus('CONFIRMED');
        } catch (error: any) {
          toast.error(error.message || 'C\u00f3 l\u1ed7i x\u1ea3y ra');
        } finally {
          setIsLoading(false);
        }
      },
      handleExit: () => {}
    }));

    const validateForm = () => {
      const errors: Record<string, string> = {};
      if (!supplierId) errors.partner = 'Vui l\u00f2ng ch\u1ecdn nh\u00e0 cung c\u1ea5p';
      
      if (lines.length === 0) {
        toast.error('Phi\u1ebfu nh\u1eadp ph\u1ea3i c\u00f3 \u00edt nh\u1ea5t 1 s\u1ea3n ph\u1ea9m.');
        errors.lines = 'Phi\u1ebfu nh\u1eadp ph\u1ea3i c\u00f3 \u00edt nh\u1ea5t 1 s\u1ea3n ph\u1ea9m.';
      }
      
      let hasLineError = false;
      lines.forEach((l, i) => {
        if (!l.productId || l.quantity <= 0) {
          errors[line__qty] = 'S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i > 0';
          hasLineError = true;
        }
      });
      
      if (hasLineError) {
        toast.error('Vui l\u00f2ng ki\u1ec3m tra l\u1ea1i th\u00f4ng tin nh\u1eadp.');
      }
      
      setFieldErrors(errors);
      return Object.keys(errors).length === 0;
    };

    const handleAddLine = () => {
      setLines([...lines, { id: line_, productId: '', productName: '', quantity: 1, unitPrice: 0, unitOfMeasure: 'CAI' }]);
    };

    const handleRemoveLine = (id: string) => {
      setLines(lines.filter(l => l.id !== id));
    };

    const handleUpdateLine = (id: string, field: keyof OrderItem, value: any) => {
      setLines(lines.map(l => l.id === id ? { ...l, [field]: value } : l));
    };

    const totalQty = lines.reduce((sum, l) => sum + (Number(l.quantity) || 0), 0);
    const totalAmount = lines.reduce((sum, l) => sum + ((Number(l.quantity) || 0) * (Number(l.unitPrice) || 0)), 0);

    return (
      <div className="h-full bg-erp-bg-content relative">
        <GenericDocumentForm
          mode={currentMode}
          docTitle="Phi\u1ebfu nh\u1eadp h\u00e0ng"
          docCode={receiptCode || 'AUTO-GENERATE'}
          status={status as any}
          info={[
            { key: 'branch', label: 'Kho nh\u1eadp', value: branch || 'CN Trung t\u00e2m' },
            { key: 'creator', label: 'Ng\u01b0\u1eddi l\u1eadp', value: creator }
          ]}
          createdDate={createdDate}
          partner={{
            label: 'Nh\u00e0 cung c\u1ea5p',
            displayName: supplierName,
            required: true,
            error: !!fieldErrors.partner,
            renderCombobox: () => (
              <SearchableCombobox
                data-testid="inbound-supplier-combo"
                value={supplierName}
                placeholder="T\u00ecm theo M\u00e3, T\u00ean, S\u0110T..."
                fetchData={async (q) => {
                  const data = await ApiService.Catalog.getSuppliers();
                  const term = q.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
                  return data.filter((s: any) => s.active !== false && (
                    s.name?.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().includes(term) ||
                    s.code?.toLowerCase().includes(term) ||
                    s.phone?.includes(term)
                  )) as any;
                }}
                columns={[
                  { header: 'M\u00e3 NCC', field: 'code', width: '90px' },
                  { header: 'T\u00ean NCC', field: 'name' },
                  { header: '\u0110i\u1ec7n tho\u1ea1i', field: 'phone', width: '110px' }
                ]}
                onSelect={(supplier: any) => {
                  setSupplierId(String(supplier.id));
                  setSupplierName(supplier.name);
                  setSupplierPhone(supplier.phone || '');
                  setSupplierAddress(supplier.address || '');
                  setFieldErrors(prev => ({ ...prev, partner: '' }));
                }}
                onCreateNew={user?.role === 'ADMIN' ? () => setShowSupplierSearch(true) : undefined}
                renderCreateNew={() => null}
              />
            ),
            fields: [
              { key: 'phone', label: '\u0110i\u1ec7n tho\u1ea1i', value: supplierPhone },
              { key: 'address', label: '\u0110\u1ecba ch\u1ec9', value: supplierAddress },
              { key: 'note', label: 'Ghi ch\u00fa', value: note, onChange: setNote, testId: 'inbound-note' }
            ]
          }}
          summary={[
            { key: 'totalQty', label: 'T\u1ed5ng s\u1ed1 l\u01b0\u1ee3ng', value: totalQty, testId: 'sum-total-qty' },
            { key: 'totalAmount', label: 'T\u1ed5ng ti\u1ec1n h\u00e0ng', value: totalAmount, bold: true, isPrimary: true, testId: 'sum-total' }
          ]}
          lines={{
            items: lines,
            testIdPrefix: 'inbound',
            priceLabel: 'Gi\u00e1 nh\u1eadp',
            onAdd: handleAddLine,
            onRemove: handleRemoveLine,
            onUpdate: handleUpdateLine,
            renderProductCombobox: (item, index) => (
              <SearchableCombobox
                data-testid={inbound-product-combo-}
                variant="cell"
                value={item.productName}
                placeholder="T\u00ecm s\u1ea3n ph\u1ea9m..."
                error={!!fieldErrors[line__qty]}
                fetchData={(query) => ApiService.Catalog.searchProducts(query) as any}
                columns={[
                  { header: 'M\u00e3 H\u00e0ng', field: 'code', width: '90px' },
                  { header: 'T\u00ean s\u1ea3n ph\u1ea9m', field: 'name' }
                ]}
                onSelect={(product: any) => {
                  handleUpdateLine(item.id, 'productId', String(product.id));
                  handleUpdateLine(item.id, 'productCode', product.code);
                  handleUpdateLine(item.id, 'productName', product.name);
                  handleUpdateLine(item.id, 'unitOfMeasure', product.baseUnit || 'CAI');
                  handleUpdateLine(item.id, 'unitPrice', product.price || 0);
                  setFieldErrors(prev => ({ ...prev, [line__qty]: '' }));
                  
                  setTimeout(() => {
                    try {
                      const input = document.querySelector<HTMLInputElement>([data-testid="inbound-line-quantity"][data-line-id=""]) || document.querySelector<HTMLInputElement>([data-testid="inbound-line-quantity"]);
                      if (input) input.focus();
                    } catch (e) {}
                  }, 50);
                }}
                onCreateNew={user?.role === 'ADMIN' ? () => { setActiveLineIndex(index); setShowProductSearch(true); } : undefined}
                renderCreateNew={() => null}
              />
            )
          }}
          errors={fieldErrors}
        />
        
        {/* Modals here if needed */}
      </div>
    );
  }
);
'''

with open('src/components/inventory/InboundReceiptForm.tsx', 'w', encoding='utf-8') as f:
    f.write(form_content)

module_content = '''import React, { useRef, useState } from 'react';
import { MdiModuleLayout, type SubViewType, type FormMode } from '../layout/MdiModuleLayout';
import { InboundReceiptForm, type InboundReceiptFormRef } from './InboundReceiptForm';
import { ApiService } from '../../api/ApiService';
import toast from 'react-hot-toast';

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
      toast.error('L\u1ed7i t\u1ea3i chi ti\u1ebft phi\u1ebfu nh\u1eadp: ' + err.message);
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
        <div className="p-4 h-full bg-white flex items-center justify-center">
          <p className="text-slate-500">Danh s\u00e1ch phi\u1ebfu nh\u1eadp (TBD)</p>
        </div>
      )}
    </MdiModuleLayout>
  );
};
'''

with open('src/components/inventory/InboundReceiptModule.tsx', 'w', encoding='utf-8') as f:
    f.write(module_content)
