import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import { Plus, Trash2, X, Printer } from 'lucide-react';
import { ApiService } from '../../api/ApiService';
import { PrintInvoice } from '../common/PrintInvoice';
import { useTabs } from '../../context/TabContext';
import { useAuth } from '../../context/AuthContext';
import { SearchModal } from '../common/SearchModal';
import type { Customer, Product } from '../../types/catalog';

import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';
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
  handlePrint: () => void;
  handleExit: () => void;
}

export const SalesOrderForm = forwardRef<SalesOrderFormRef, SalesOrderFormProps>(
  ({ mode: initialMode = 'VIEW', initialData, onStateChange }, ref) => {
    const { user } = useAuth();
    const { closeTab, activeTabId } = useTabs();

    const [mode, setMode] = useState<FormMode>(initialMode);
    const isView = mode === 'VIEW';

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
        productId: p.id,
        productName: p.name,
        quantity: p.qty,
        unitPrice: p.price,
      })) || [],
    );

    const [advancePayment, setAdvancePayment] = useState<number>(0);
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
      handlePrint: () => setShowPrintModal(true),
      handleExit,
    }));

    const handleAdd = () => {
      // Reset state for new order
      setMode('ADD');
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
    const remainingBalance = oldDebt + finalAmount - advancePayment;

    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

    const { createMutation } = useSalesInvoice();

    const handleSubmit = async () => {
      setIsLoading(true);
      setError(null);
      setFieldErrors({});
      try {
        const newErrors: Record<string, string> = {};
        if (!customerId) {
          newErrors.partner = 'Vui lòng chọn khách hàng';
        }
        if (items.length === 0) {
          throw new Error('Đơn hàng phải có ít nhất 1 sản phẩm.');
        }

        // check items
        items.forEach((item, index) => {
          if (!item.productId) newErrors[`item_${index}_product`] = 'Chọn sản phẩm';
          if (item.quantity <= 0) newErrors[`item_${index}_quantity`] = 'Số lượng > 0';
        });

        if (Object.keys(newErrors).length > 0) {
          setFieldErrors(newErrors);
          throw new Error('Vui lòng kiểm tra lại thông tin nhập.');
        }

        const payload = {
          customerId: customerId,
          paymentMethod,
          advancePayment,
          lines: items.map((i) => ({
            productId: Number(i.productId),
            productName: i.productName,
            quantity: i.quantity,
            unitPrice: i.unitPrice,
            unitOfMeasure: 'CAI',
          })),
        };

        if (mode === 'ADD') {
          const response = await createMutation.mutateAsync(payload);
          if (response && response.invoiceCode) {
            setOrderCode(response.invoiceCode);
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

    const handlePrint = () => {
      window.print();
    };

    return (
      <div className="flex flex-col h-full bg-white relative">
        {/* Scrollable Content */}
        <GenericDocumentForm
          mode={mode}
          title={
            mode === 'ADD'
              ? 'Thêm mới Đơn hàng'
              : mode === 'EDIT'
                ? `Sửa Đơn: ${orderCode}`
                : `Đơn Bán Hàng: ${orderCode}`
          }
          error={error}
          errors={fieldErrors}
          orderCode={orderCode}
          creator={creator}
          branch={branch}
          createdDate={createdDate}
          setCreatedDate={setCreatedDate}
          note={note}
          setNote={setNote}
          partnerTitle="Khách hàng / Đối tác"
          partnerCodeLabel="Mã KH / Tên KH"
          partnerPlaceholder="Nhấn để chọn khách hàng..."
          partnerName={customerName}
          onPartnerSearch={() => setShowCustomerSearch(true)}
          onProductSearch={() => setShowProductSearch(true)}
          address={address}
          setAddress={setAddress}
          phone={phone}
          setPhone={setPhone}
          contactPerson={contactPerson}
          setContactPerson={setContactPerson}
          oldDebt={oldDebt}
          setOldDebt={setOldDebt}
          totalAmount={totalAmount}
          discount={discount}
          setDiscount={setDiscount}
          tax={tax}
          setTax={setTax}
          advancePayment={advancePayment}
          setAdvancePayment={setAdvancePayment}
          remainingBalance={remainingBalance}
          items={items}
          onAddItem={addItem}
          onRemoveItem={removeItem}
          onUpdateItem={updateItem}
          renderPartnerCombobox={() => (
            <SearchableCombobox
              data-testid="sales-customer-combo"
              value={customerName}
              placeholder="Nhấn để chọn khách hàng..."
              disabled={mode === 'VIEW'}
              fetchData={ApiService.Catalog.searchCustomers}
              columns={[
                { header: 'Mã', field: 'customerCode', width: '20%' },
                { header: 'Tên KH', field: 'name', width: '50%' },
                { header: 'Điện thoại', field: 'phoneNumber', width: '30%' }
              ]}
              onSelect={async (customer) => {
                setCustomerCode(customer.customerCode || customer.customerId || '');
                setCustomerId(customer.id);
                setCustomerName(customer.name);
                setAddress(customer.address || '');
                setPhone(customer.phoneNumber || '');
                setContactPerson(customer.contactPerson || '');
                try {
                  const debt = await ApiService.Debt.getBalance(customer.id);
                  setOldDebt(debt || 0);
                } catch (e) {
                  console.error('Failed to fetch debt', e);
                  setOldDebt(0);
                }
              }}
            />
          )}
          renderProductCombobox={(itemId, currentVal) => (
            <SearchableCombobox
              data-testid={`sales-product-combo-${itemId}`}
              value={currentVal}
              placeholder="Nhấn để chọn..."
              disabled={mode === 'VIEW'}
              fetchData={ApiService.Catalog.searchProducts}
              columns={[
                { header: 'Mã', field: 'productCode', width: '20%' },
                { header: 'Tên', field: 'name', width: '50%' },
                { header: 'Giá', field: 'price', width: '30%', format: (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0) }
              ]}
              onSelect={(product) => {
                updateItem(itemId, 'productId', product.productId);
                updateItem(itemId, 'productName', product.name);
                updateItem(itemId, 'unitPrice', product.price);
              }}
            />
          )}
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
          fetchData={(query) => ApiService.Catalog.searchCustomers(query)}
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
          fetchData={(query) => ApiService.Catalog.searchProducts(query)}
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
