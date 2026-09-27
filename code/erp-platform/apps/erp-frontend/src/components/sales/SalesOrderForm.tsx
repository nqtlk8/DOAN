import { useState, useEffect, useMemo, forwardRef, useImperativeHandle } from 'react';
import { Plus, Trash2, X, Printer } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { PrintInvoice } from '../common/PrintInvoice';
import { useTabs } from '../../context/TabContext';
import { useAuth } from '../../context/AuthContext';
import { SearchModal } from '../common/SearchModal';
import type { Customer, Product } from '../../types/catalog';

import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';
import { QuickCreateCustomer } from '../common/quick-create/QuickCreateCustomer';
import { QuickCreateProduct } from '../common/quick-create/QuickCreateProduct';
import { formatCurrency } from '../../shared/utils/format';
import { SearchableCombobox } from '../common/SearchableCombobox';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import toast from 'react-hot-toast';
import { useSalesInvoice } from '../../hooks/useSalesInvoice';

export type FormMode = 'VIEW' | 'ADD' | 'EDIT';

export interface SalesOrderFormProps {
  mode?: FormMode;
  initialData?: any;
  onStateChange?: (mode: FormMode, isLoading: boolean) => void;
}

export interface SalesOrderFormRef {
  handleAdd: () => void;
  handleEdit: () => void;
  handleCancel: () => void;
  handleDelete: () => void;
  handleSubmit: () => void;
  handleConfirm: () => void;
  handlePrint: () => void;
  handleExit: () => void;
}

/**
 * Chuẩn hoá dữ liệu đầu vào của form.
 * Khi xem lại đơn, SalesModule truyền SalesInvoiceResponseDto (invoiceCode, lines, previousDebt...)
 * trong khi form đọc dạng cũ (orderCode, products[{id,name,qty,price}]) -> map về dạng cũ (BUG-7).
 */
const normalizeInitialData = (data: any) => {
  if (!data || !Array.isArray(data.lines)) return data;
  return {
    ...data,
    orderCode: data.invoiceCode,
    customer: data.customerName,
    createdDate: data.createdAt ? String(data.createdAt).slice(0, 10) : undefined,
    oldDebt: Number(data.previousDebt ?? 0),
    advancePayment: Number(data.advancePayment ?? 0),
    products: data.lines.map((l: any) => ({
      id: l.productId,
      name: l.productName,
      qty: Number(l.quantity),
      price: Number(l.unitPrice),
      unitOfMeasure: l.unitOfMeasure,
    })),
  };
};

export const SalesOrderForm = forwardRef<SalesOrderFormRef, SalesOrderFormProps>(
  ({ mode: initialMode = 'VIEW', initialData: rawInitialData, onStateChange }, ref) => {
    const initialData = useMemo(() => normalizeInitialData(rawInitialData), [rawInitialData]);
    const { user } = useAuth();
    const { closeTab, activeTabId } = useTabs();

    const [mode, setMode] = useState<FormMode>(initialMode);
    const isView = mode === 'VIEW';
    const [currentInvoiceId, setCurrentInvoiceId] = useState<string | null>(initialData?.id || null);
    const [invoiceStatus, setInvoiceStatus] = useState<string>(initialData?.status || 'DRAFT');

    // Column 1: Internal
    const creator = initialData?.creator || user?.username || 'Admin';
    const branch = initialData?.branch || 'CN Trung Tâm';
    const [createdDate, setCreatedDate] = useState(initialData?.createdDate || new Date().toISOString().slice(0, 10));
    const [orderCode, setOrderCode] = useState(initialData?.orderCode || 'AUTO-GENERATE');
    const [note, setNote] = useState(initialData?.note || '');

    // Column 2: Customer
    const [customerCode, setCustomerCode] = useState(initialData?.customerCode || '');
    const [customerId, setCustomerId] = useState(initialData?.customerId || '');
    const [customerName, setCustomerName] = useState(initialData?.customerName || initialData?.customer || '');
    const [address, setAddress] = useState(initialData?.address || '');
    const [phone, setPhone] = useState(initialData?.phone || '');
    const [contactPerson, setContactPerson] = useState(initialData?.contactPerson || '');

    // Column 3: Financial
    const [paymentMethod] = useState(initialData?.paymentMethod || 'CASH');

    const [items, setItems] = useState<OrderItem[]>(
      initialData?.products?.map((p: any) => ({
        id: Date.now().toString() + Math.random(),
        productId: p.id != null ? String(p.id) : '',
        productName: p.name,
        quantity: p.qty,
        unitPrice: p.price,
        unitOfMeasure: p.unitOfMeasure,
      })) || [],
    );

    const [advancePayment, setAdvancePayment] = useState<number>(initialData?.advancePayment || 0);
    const [oldDebt, setOldDebt] = useState<number>(initialData?.oldDebt || 0);
    const [discount, setDiscount] = useState<number>(0);
    const [tax, setTax] = useState<number>(0);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [showPrintModal, setShowPrintModal] = useState(false);
    const [showCustomerSearch, setShowCustomerSearch] = useState(false);
    const [showProductSearch, setShowProductSearch] = useState(false);
    const [activeItemRowId, setActiveItemRowId] = useState<string | null>(null);

    useEffect(() => {
      if (onStateChange) {
        onStateChange(mode, isLoading);
      }
    }, [mode, isLoading, onStateChange]);

    useImperativeHandle(ref, () => ({
      handleAdd,
      handleEdit,
      handleCancel,
      handleDelete,
      handleSubmit,
      handleConfirm,
      handlePrint: () => setShowPrintModal(true),
      handleExit,
    }));

    const handleAdd = () => {
      // Reset state for new order
      setMode('ADD');
      // Bỏ liên kết với đơn đang xem trước đó
      setCurrentInvoiceId(null);
      setInvoiceStatus('DRAFT');
      setNote('');
      setAdvancePayment(0);
      setOldDebt(0);
      setFieldErrors({});
      setOrderCode('AUTO-GENERATE');
      setCustomerCode('');
      setCustomerId('');
      setCustomerName('');
      setAddress('');
      setPhone('');
      setContactPerson('');
      setItems([]);
    };

    const handleEdit = () => {
      setMode('EDIT');
    };

    const [confirmState, setConfirmState] = useState<{isOpen: boolean, type: 'CANCEL' | 'DELETE' | 'EXIT' | null}>({ isOpen: false, type: null });

    const handleCancel = () => {
      setConfirmState({ isOpen: true, type: 'CANCEL' });
    };

    const handleDelete = () => {
      setConfirmState({ isOpen: true, type: 'DELETE' });
    };

    const handleExit = () => {
      if (!isView) {
        setConfirmState({ isOpen: true, type: 'EXIT' });
      } else {
        if (activeTabId) closeTab(activeTabId);
      }
    };

    const addItem = () => {
      if (isView) return;
      const newItem: OrderItem = {
        id: Date.now().toString(),
        productId: '',
        productName: '',
        quantity: 1,
        unitPrice: 0,
      };
      setItems([...items, newItem]);
    };

    const removeItem = (id: string) => {
      if (isView) return;
      setItems(items.filter((item) => item.id !== id));
    };

    const updateItem = (id: string, field: keyof OrderItem, value: any) => {
      if (isView) return;
      setItems((prevItems) => prevItems.map((item) => (item.id === id ? { ...item, [field]: value } : item)));
    };

    const totalAmount = items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
    const finalAmount = totalAmount - discount + tax;
    const invoiceRemaining = Math.max(0, finalAmount - advancePayment);
    const remainingBalance = oldDebt + finalAmount - advancePayment;

    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

    const { createMutation, confirmMutation } = useSalesInvoice();

    const handleSubmit = async () => {
      setError(null);
      setFieldErrors({});

      const newErrors: Record<string, string> = {};
      if (!customerId) {
        newErrors.partner = 'Vui lòng chọn khách hàng';
      }
      if (items.length === 0) {
        toast.error('Đơn hàng phải có ít nhất 1 sản phẩm.');
        return;
      }

      // check items
      items.forEach((item, index) => {
        if (!item.productId) newErrors[`item_${index}_product`] = 'Chọn sản phẩm';
        if (item.quantity <= 0) newErrors[`item_${index}_quantity`] = 'Số lượng > 0';
      });

      if (Object.keys(newErrors).length > 0) {
        setFieldErrors(newErrors);
        toast.error('Vui lòng kiểm tra lại thông tin nhập.');
        return;
      }

      setIsLoading(true);
      try {
        const payload = {
          customerId: customerId,
          paymentMethod,
          note,
          advancePayment,
          lines: items.map((i) => ({
            productId: Number(i.productId),
            productName: i.productName,
            quantity: i.quantity,
            unitPrice: Number(i.unitPrice) || 0,
            unitOfMeasure: i.unitOfMeasure || 'CAI',
          })),
        };

        if (mode === 'ADD') {
          // Backend tạo và xác nhận đơn trong cùng 1 giao dịch (bỏ bước Draft riêng):
          // bấm "Lưu" là trừ tồn kho + cộng công nợ ngay, không cần bấm Xác nhận thêm
          // lần nữa. Nếu có lỗi (vd. tồn kho/công nợ), toàn bộ giao dịch rollback ở
          // backend nên sẽ không có đơn "mồ côi" ở trạng thái Draft.
          const response = await createMutation.mutateAsync(payload);
          if (response && response.invoiceCode) {
            setOrderCode(response.invoiceCode);
            if (response.id) {
              setCurrentInvoiceId(response.id);
            }
            setInvoiceStatus('CONFIRMED');
          }
        }

        setMode('VIEW');
      } catch (err: any) {
        console.error(err);
        toast.error(err.message || 'Lỗi khi lưu đơn hàng');
      } finally {
        setIsLoading(false);
      }
    };

    const handleConfirm = async () => {
      if (!currentInvoiceId) {
        toast.error('Chưa có ID hóa đơn, vui lòng lưu trước khi xác nhận.');
        return;
      }
      if (invoiceStatus === 'CONFIRMED') {
        // Đơn tạo qua nút "Lưu" đã được xác nhận ngay từ đầu, không còn ở trạng thái Draft.
        toast.error('Đơn hàng đã được xác nhận (đã trừ tồn kho và cộng công nợ).');
        return;
      }
      setIsLoading(true);
      try {
        await confirmMutation.mutateAsync(currentInvoiceId);
        setInvoiceStatus('CONFIRMED');
      } catch (err: any) {
        console.error(err);
        toast.error(err.message || 'Lỗi khi xác nhận đơn hàng');
      } finally {
        setIsLoading(false);
      }
    };

    const handlePrint = () => {
      window.print();
    };

    return (
      <div className="flex flex-col h-full bg-white relative">
        {/* Scrollable Content */}
        <GenericDocumentForm
          mode={mode}
          docTitle="Phiếu bán hàng"
          docCode={orderCode || 'AUTO-GENERATE'}
          status={initialData?.status}
          error={error}
          errors={fieldErrors}
          info={[
            { key: 'branch', label: 'Kho xuất', value: branch || 'CN Trung tâm' },
            { key: 'priceList', label: 'Bảng giá', value: 'Bán hàng theo khách' },
            { key: 'creator', label: 'Nhân viên', value: user?.username || '' }
          ]}
          createdDate={createdDate ?? ''}
          onCreatedDateChange={setCreatedDate}
          partner={{
            label: 'Khách hàng',
            required: true,
            displayName: `${customerName} ${customerCode ? `(${customerCode})` : ''}`,
            onAdvancedSearch: () => setShowCustomerSearch(true),
            renderCombobox: (hasError) => (
              <SearchableCombobox
                data-testid="sales-customer-combo"
                value={customerName}
                placeholder="Nhập mã, tên hoặc SĐT khách hàng..."
                error={hasError}
                fetchData={ApiService.Catalog.searchCustomers as any}
                columns={[
                  { header: 'MÃ KH', field: 'customerCode', width: '90px' },
                  { header: 'TÊN KH', field: 'name', width: '1fr' },
                  { header: 'ĐIỆN THOẠI', field: 'phone', width: '110px' },
                  { header: 'ĐỊA CHỈ', field: 'address', width: '30%' }
                ]}
                onSelect={async (customer) => {
                  setCustomerCode(customer.customerCode || customer.customerId || '');
                  setCustomerId(customer.id);
                  setCustomerName(customer.name);
                  setAddress(customer.address || '');
                  setPhone(customer.phone || '');
                  setContactPerson(customer.contactPerson || '');
                  try {
                    const debt = await ApiService.Debt.getBalance(customer.id);
                    setOldDebt(debt || 0);
                  } catch (e) {
                    console.error('Failed to fetch debt', e);
                    setOldDebt(0);
                  }
                }}
                onCreateNew={user?.role === 'ADMIN' ? () => setShowCustomerSearch(true) : undefined}
                
              />
            ),
            fields: [
              { key: 'contact', label: 'Người liên hệ', value: contactPerson, onChange: setContactPerson },
              { key: 'phone', label: 'Điện thoại', value: phone, onChange: setPhone },
              { key: 'address', label: 'Địa chỉ', value: address, onChange: setAddress },
              { key: 'note', label: 'Ghi chú', value: note, onChange: setNote }
            ]
          }}
          summary={[
            { key: 'oldDebt', label: 'Nợ trước', value: oldDebt, onChange: setOldDebt, testId: 'sum-old-debt' },
            { key: 'total', label: 'Tiền hàng', value: totalAmount, testId: 'sum-total' },
            { key: 'discount', label: 'Chiết khấu', value: discount, onChange: setDiscount, testId: 'sum-discount' },
            { key: 'tax', label: 'VAT', value: tax, onChange: setTax, testId: 'sum-tax' },
            { key: 'advance', label: 'Trả trước', value: advancePayment, onChange: setAdvancePayment, testId: 'sum-advance' },
            { key: 'invoiceRemaining', label: 'Cần của đơn', value: invoiceRemaining, tone: 'primary', strong: true, testId: 'sum-invoice-remaining' },
            { key: 'newDebt', label: 'Nợ tổng mới', value: remainingBalance, tone: 'danger', strong: true, testId: 'sum-new-debt' },
          ]}
          lines={{
            testIdPrefix: 'sales',
            items: items,
            priceLabel: 'Đơn giá',
            onAdd: () => setItems([...items, { id: Date.now().toString() + Math.random(), productId: '', productName: '', quantity: 1, unitPrice: 0 }]),
            onRemove: (id) => setItems(items.filter(i => i.id !== id)),
            onUpdate: (id, field, value) => updateItem(id, field as any, value),
            renderProductCombobox: (line, index, hasError) => (
              <SearchableCombobox
                data-testid={`sales-product-combo-${line.id}`}
                value={line.productName}
                placeholder="Nhấn để chọn..."
                error={hasError}
                fetchData={ApiService.Catalog.searchProducts as any}
                columns={[
                  { header: 'MÃ', field: 'code', width: '90px' },
                  { header: 'TÊN', field: 'name', width: '1fr' },
                  { header: 'ĐVT', field: 'baseUnit', width: '70px' },
                  { header: 'GIÁ', field: 'price', width: '120px', format: (val) => formatCurrency(val || 0) }
                ]}
                onSelect={(product) => {
                  updateItem(line.id, 'productId', String(product.id));
                  updateItem(line.id, 'productCode', product.code || '');
                  updateItem(line.id, 'productName', product.name);
                  updateItem(line.id, 'unitPrice', Number(product.price ?? 0));
                  updateItem(line.id, 'unitOfMeasure', product.baseUnit || 'CAI');
                  
                  setTimeout(() => {
                    try {
                      const inputs = document.querySelectorAll(`[data-testid="sales-line-quantity"]`);
                      const input = inputs[index] as HTMLInputElement;
                      if (input) input.focus();
                    } catch (e) {}
                  }, 50);
                }}
                onCreateNew={user?.role === 'ADMIN' ? () => setShowProductSearch(true) : undefined}
                
              />
            )
          }}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onConfirm={handleConfirm}
          onPrint={handlePrint}
          onSave={handleSubmit}
          onCancel={handleCancel}
          onExit={handleExit}
          hideConfirm={initialData?.status === 'CONFIRMED'}
          isLoading={createMutation.isPending || confirmMutation.isPending}
        />

        {/* Print Preview Modal */}
        {showPrintModal && (
          <div className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
              <div className="p-4 border-b flex justify-between items-center bg-slate-50">
                <h2 className="text-xl font-semibold text-slate-800">Preview In Phiếu</h2>
                <div className="flex gap-2">
                  <button
                    onClick={() => setShowPrintModal(false)}
                    className="px-4 py-2 border border-slate-300 bg-white text-slate-700 rounded-lg hover:bg-slate-100 flex items-center gap-2"
                  >
                    <X size={18} /> Đóng
                  </button>
                  <button
                    onClick={handlePrint}
                    className="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 flex items-center gap-2"
                  >
                    <Printer size={18} /> In ngay
                  </button>
                </div>
              </div>
              <div className="p-8 overflow-y-auto bg-slate-200 flex-1 flex justify-center">
                <div className="bg-white shadow-sm" style={{ width: '148mm', minHeight: '210mm' }}>
                  <PrintInvoice
                    customerName={customerName}
                    items={items}
                    totalAmount={totalAmount}
                    advancePayment={advancePayment}
                    invoiceRemaining={invoiceRemaining}
                    remainingBalance={remainingBalance}
                    oldDebt={oldDebt}
                    mode="sales"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        <PrintInvoice
          className="hidden print:block fixed inset-0 z-[9999] bg-white w-full h-full"
          customerName={customerName}
          items={items}
          totalAmount={totalAmount}
          advancePayment={advancePayment}
          invoiceRemaining={invoiceRemaining}
          remainingBalance={remainingBalance}
          oldDebt={oldDebt}
          mode="sales"
        />

        {/* Search Modals */}
        <SearchModal<Customer>
          isOpen={showCustomerSearch}
          onClose={() => setShowCustomerSearch(false)}
          title="Tìm kiếm Khách hàng"
          placeholder="Nhập tên hoặc mã KH..."
          fetchData={(query) => ApiService.Catalog.searchCustomers(query) as any}
          renderItem={(c) => (
            <div>
              <div className="font-medium text-slate-900">
                {c.name} {c.code ? `(${c.code})` : ''}
              </div>
              <div className="text-sm text-slate-500">
                {c.phone || ''} - {c.address || ''}
              </div>
            </div>
          )}
          onSelect={(c) => {
            setCustomerCode(c.id);
            setCustomerName(c.name);
            if (c.address) setAddress(c.address);
            if (c.phone) setPhone(c.phone);
          }}
        />

        <SearchModal<Product>
          isOpen={showProductSearch}
          onClose={() => {
            setShowProductSearch(false);
            setActiveItemRowId(null);
          }}
          title="Tìm kiếm Sản phẩm"
          placeholder="Nhập tên hoặc mã SP..."
          fetchData={(query) => ApiService.Catalog.searchProducts(query) as any}
          renderItem={(p) => (
            <div className="flex justify-between items-center">
              <div>
                <div className="font-medium text-slate-900">
                  {p.name} {p.code ? `(${p.code})` : ''}
                </div>
              </div>
              <div className="text-sm font-semibold text-teal-600">
                {(p.price ?? 0).toLocaleString()} ₫
              </div>
            </div>
          )}
          onSelect={(p) => {
            if (activeItemRowId) {
              setItems(
                items.map((item) =>
                  item.id === activeItemRowId
                    ? {
                        ...item,
                        productId: String(p.id),
                        productName: p.name,
                        unitPrice: p.price ?? 0,
                      }
                    : item,
                ),
              );
            }
          }}
        />
        <ConfirmDialog
          isOpen={confirmState.isOpen}
          title={
            confirmState.type === 'DELETE' ? 'Xóa chứng từ' :
            confirmState.type === 'EXIT' ? 'Xác nhận thoát' :
            'Hủy thay đổi'
          }
          message={
            confirmState.type === 'DELETE' ? 'Bạn có chắc chắn muốn xóa chứng từ này không? Hành động này không thể hoàn tác.' :
            confirmState.type === 'EXIT' ? 'Dữ liệu chưa được lưu. Bạn có chắc chắn muốn thoát và mất các thay đổi không?' :
            'Bạn có chắc chắn muốn hủy các thay đổi chưa lưu không?'
          }
          onConfirm={() => {
            if (confirmState.type === 'DELETE') {
              if (activeTabId) closeTab(activeTabId);
            } else if (confirmState.type === 'EXIT') {
              if (activeTabId) closeTab(activeTabId);
            } else if (confirmState.type === 'CANCEL') {
              if (initialMode === 'ADD' && mode === 'ADD') {
                if (activeTabId) closeTab(activeTabId);
              } else {
                setMode('VIEW');
              }
            }
            setConfirmState({ isOpen: false, type: null });
          }}
          onCancel={() => setConfirmState({ isOpen: false, type: null })}
        />
      </div>
    );
  },
);
