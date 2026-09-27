import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import { ApiService } from '../../api/ApiService';
import { useAuth } from '../../context/AuthContext';
import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';
import { SearchableCombobox } from '../common/SearchableCombobox';
import toast from 'react-hot-toast';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { useTabs } from '../../context/TabContext';

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
    const { closeTab } = useTabs();
    const [showCancelConfirm, setShowCancelConfirm] = useState(false);
    
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
            toast.success('Lưu phiếu nhập thành công');
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
          toast.error(error.message || 'Có lỗi xảy ra');
        } finally {
          setIsLoading(false);
        }
      },
      handleCancel: () => {
        if (currentMode === 'ADD' || currentMode === 'EDIT') {
          setShowCancelConfirm(true);
        }
      },
      handleConfirm: async () => {
        if (!receiptId) return;
        setIsLoading(true);
        try {
          await ApiService.InboundReceipt.confirm(receiptId);
          toast.success('Xác nhận phiếu nhập thành công');
          setStatus('CONFIRMED');
        } catch (error: any) {
          toast.error(error.message || 'Có lỗi xảy ra');
        } finally {
          setIsLoading(false);
        }
      },
      handleExit: () => {}
    }));

    const validateForm = () => {
      const errors: Record<string, string> = {};
      if (!supplierId) errors.partner = 'Vui lòng chọn nhà cung cấp';
      
      if (lines.length === 0) {
        toast.error('Phiếu nhập phải có ít nhất 1 sản phẩm.');
        errors.lines = 'Phiếu nhập phải có ít nhất 1 sản phẩm.';
      }
      
      let hasLineError = false;
      lines.forEach((l, i) => {
        if (!l.productId || l.quantity <= 0) {
          errors[`line_${i}_qty`] = 'Số lượng phải > 0';
          hasLineError = true;
        }
      });
      
      if (hasLineError) {
        toast.error('Vui lòng kiểm tra lại thông tin nhập.');
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
          docTitle="Phiếu nhập hàng"
          docCode={receiptCode || 'AUTO-GENERATE'}
          status={status as any}
          info={[
            { key: 'branch', label: 'Kho nhập', value: branch || 'CN Trung tâm' },
            { key: 'creator', label: 'Người lập', value: creator }
          ]}
          createdDate={createdDate}
          partner={{
            label: 'Nhà cung cấp',
            displayName: supplierName,
            required: true,
            
            renderCombobox: (hasError) => (
              <SearchableCombobox
                error={hasError}
                data-testid="inbound-supplier-combo"
                value={supplierName}
                placeholder="Tìm theo Mã, Tên, SĐT..."
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
                  { header: 'Điện thoại', field: 'phone', width: '110px' }
                ]}
                onSelect={(supplier: any) => {
                  setSupplierId(String(supplier.id));
                  setSupplierName(supplier.name);
                  setSupplierPhone(supplier.phone || '');
                  setSupplierAddress(supplier.address || '');
                  setFieldErrors(prev => ({ ...prev, partner: '' }));
                }}
                onCreateNew={user?.role === 'ADMIN' ? () => setShowSupplierSearch(true) : undefined}
                
              />
            ),
            fields: [
              { key: 'phone', label: 'Điện thoại', value: supplierPhone },
              { key: 'address', label: 'Địa chỉ', value: supplierAddress },
              { key: 'note', label: 'Ghi chú', value: note, onChange: setNote, testId: 'inbound-note' }
            ]
          }}
          summary={[
            { key: 'totalQty', label: 'Tổng số lượng', value: totalQty, testId: 'sum-total-qty' },
            { key: 'totalAmount', label: 'Tổng tiền hàng', value: totalAmount, strong: true, tone: 'primary', testId: 'sum-total' }
          ]}
          lines={{
            items: lines,
            testIdPrefix: 'inbound',
            priceLabel: 'Giá nhập',
            onAdd: handleAddLine,
            onRemove: handleRemoveLine,
            onUpdate: handleUpdateLine,
            renderProductCombobox: (item, index) => (
              <SearchableCombobox
                data-testid={`inbound-product-combo-${item.id}`}
                variant="cell"
                value={item.productName}
                placeholder="Tìm sản phẩm..."
                error={!!fieldErrors[`line_${index}_qty`]}
                fetchData={(query) => ApiService.Catalog.searchProducts(query) as any}
                columns={[
                  { header: 'Mã Hàng', field: 'code', width: '90px' },
                  { header: 'Tên sản phẩm', field: 'name' }
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
                
              />
            )
          }}
          errors={fieldErrors}
        />
        <ConfirmDialog
          isOpen={showCancelConfirm}
          title="Xác nhận hủy"
          message="Bạn có chắc chắn muốn hủy phiếu nhập này? Các thay đổi sẽ không được lưu."
          confirmLabel="Đồng ý"
          cancelLabel="Đóng"
          onConfirm={() => {
            setShowCancelConfirm(false);
            if (receiptId) setCurrentMode('VIEW');
            else closeTab('inbound');
          }}
          onCancel={() => setShowCancelConfirm(false)}
        />
      </div>
    );
  }
);
