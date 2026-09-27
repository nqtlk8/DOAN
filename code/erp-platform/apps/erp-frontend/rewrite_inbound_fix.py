import os

form_content = '''import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import { ApiService } from '../../api/ApiService';
import { useAuth } from '../../context/AuthContext';
import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';
import { SearchableCombobox } from '../common/SearchableCombobox';
import toast from 'react-hot-toast';

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
    const [branch] = useState(initialData?.branch || 'CN Trung tâm');
    const creator = initialData?.creator || user?.username || 'Admin';

    const [supplierId, setSupplierId] = useState<string>(initialData?.supplierId || '');
    const [supplierName, setSupplierName] = useState<string>(initialData?.supplierName || '');
    const [supplierPhone, setSupplierPhone] = useState<string>(initialData?.supplierPhone || '');
    const [supplierAddress, setSupplierAddress] = useState<string>(initialData?.supplierAddress || '');
    const [note, setNote] = useState<string>(initialData?.note || '');

    const [lines, setLines] = useState<OrderItem[]>(() => {
      if (initialData?.lines && Array.isArray(initialData.lines)) {
        return initialData.lines.map((l: any, i: number) => ({
          id: `line_${i}`,
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
            toast.success('Luu phi?u nh?p thành công');
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
          toast.error(error.message || 'Có l?i x?y ra');
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
          toast.success('Xác nh?n phi?u nh?p thành công');
          setStatus('CONFIRMED');
        } catch (error: any) {
          toast.error(error.message || 'Có l?i x?y ra');
        } finally {
          setIsLoading(false);
        }
      },
      handleExit: () => {}
    }));

    const validateForm = () => {
      const errors: Record<string, string> = {};
      if (!supplierId) errors.partner = 'Vui lòng ch?n nhà cung c?p';
      
      if (lines.length === 0) {
        toast.error('Phi?u nh?p ph?i có ít nh?t 1 s?n ph?m.');
        errors.lines = 'Phi?u nh?p ph?i có ít nh?t 1 s?n ph?m.';
      }
      
      let hasLineError = false;
      lines.forEach((l, i) => {
        if (!l.productId || l.quantity <= 0) {
          errors[`line_${i}_qty`] = 'S? lu?ng ph?i > 0';
          hasLineError = true;
        }
      });
      
      if (hasLineError) {
        toast.error('Vui lòng ki?m tra l?i thông tin nh?p.');
      }
      
      setFieldErrors(errors);
      return Object.keys(errors).length === 0;
    };

    const handleAddLine = () => {
      setLines([...lines, { id: `line_${Date.now()}`, productId: '', productName: '', quantity: 1, unitPrice: 0, unitOfMeasure: 'CAI' }]);
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
          docTitle="Phi?u nh?p hàng"
          docCode={receiptCode || 'AUTO-GENERATE'}
          status={status as any}
          info={[
            { key: 'branch', label: 'Kho nh?p', value: branch || 'CN Trung tâm' },
            { key: 'creator', label: 'Ngu?i l?p', value: creator }
          ]}
          createdDate={createdDate}
          partner={{
            label: 'Nhà cung c?p',
            displayName: supplierName,
            required: true,
            error: !!fieldErrors.partner,
            renderCombobox: () => (
              <SearchableCombobox
                data-testid="inbound-supplier-combo"
                value={supplierName}
                placeholder="Tìm theo Mã, Tên, SÐT..."
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
                  { header: 'Mã NCC', field: 'code', width: '90px' },
                  { header: 'Tên NCC', field: 'name' },
                  { header: 'Ði?n tho?i', field: 'phone', width: '110px' }
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
              { key: 'phone', label: 'Ði?n tho?i', value: supplierPhone },
              { key: 'address', label: 'Ð?a ch?', value: supplierAddress },
              { key: 'note', label: 'Ghi chú', value: note, onChange: setNote, testId: 'inbound-note' }
            ]
          }}
          summary={[
            { key: 'totalQty', label: 'T?ng s? lu?ng', value: totalQty, testId: 'sum-total-qty' },
            { key: 'totalAmount', label: 'T?ng ti?n hàng', value: totalAmount, bold: true, isPrimary: true, testId: 'sum-total' }
          ]}
          lines={{
            items: lines,
            testIdPrefix: 'inbound',
            priceLabel: 'Giá nh?p',
            onAdd: handleAddLine,
            onRemove: handleRemoveLine,
            onUpdate: handleUpdateLine,
            renderProductCombobox: (item, index) => (
              <SearchableCombobox
                data-testid={`inbound-product-combo-${item.id}`}
                variant="cell"
                value={item.productName}
                placeholder="Tìm s?n ph?m..."
                error={!!fieldErrors[`line_${index}_qty`]}
                fetchData={(query) => ApiService.Catalog.searchProducts(query) as any}
                columns={[
                  { header: 'Mã Hàng', field: 'code', width: '90px' },
                  { header: 'Tên s?n ph?m', field: 'name' }
                ]}
                onSelect={(product: any) => {
                  handleUpdateLine(item.id, 'productId', String(product.id));
                  handleUpdateLine(item.id, 'productCode', product.code);
                  handleUpdateLine(item.id, 'productName', product.name);
                  handleUpdateLine(item.id, 'unitOfMeasure', product.baseUnit || 'CAI');
                  handleUpdateLine(item.id, 'unitPrice', product.price || 0);
                  setFieldErrors(prev => ({ ...prev, [`line_${index}_qty`]: '' }));
                  
                  setTimeout(() => {
                    try {
                      const input = document.querySelector<HTMLInputElement>(`[data-testid="inbound-line-quantity"][data-line-id="${item.id}"]`) || document.querySelector<HTMLInputElement>(`[data-testid="inbound-line-quantity"]`);
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
      </div>
    );
  }
);
'''

with open('src/components/inventory/InboundReceiptForm.tsx', 'w', encoding='utf-8') as f:
    f.write(form_content)
