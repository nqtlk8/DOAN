import { forwardRef, useEffect, useImperativeHandle, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Printer, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../shared/errors/errorMessage';
import { ApiService } from '../../api/ApiService';
import { PrintInvoice } from '../common/PrintInvoice';
import { useTabs } from '../../context/TabContext';
import { useAuth } from '../../context/AuthContext';
import { SearchModal } from '../common/SearchModal';
import { GenericDocumentForm, type OrderItem } from '../common/document/GenericDocumentForm';
import {
  focusLineCell,
  newLineId,
  productToLinePatch,
  type ProductLike,
  sumLines,
  useDocumentLines,
  validateLines,
  omitKey,
} from '../common/document/documentLines';
import { QuickCreateCustomer } from '../common/quick-create/QuickCreateCustomer';
import { QuickCreateProduct } from '../common/quick-create/QuickCreateProduct';
import { SearchableCombobox } from '../common/SearchableCombobox';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { formatCurrency, toInputDate } from '../../shared/utils/format';
import { useSalesInvoice } from '../../hooks/useSalesInvoice';
import type { DocStatus, FormMode, ProductDto, SalesInvoiceDto } from '../../types/documents';
import type { components } from '@erp/api-contract';

export type { FormMode };

export interface SalesOrderFormProps {
  mode?: FormMode;
  initialData?: SalesInvoiceDto | null;
  onStateChange?: (mode: FormMode, isLoading: boolean, status?: DocStatus) => void;
}

export interface SalesOrderFormRef {
  handleAdd: () => void;
  handleCancel: () => void;
  handleSubmit: () => void;
  handleConfirm: () => void;
  handlePrint: () => void;
  handleExit: () => void;
}

interface CustomerLike {
  id?: string;
  customerCode?: string;
  name?: string;
  phone?: string;
  address?: string;
  contactPerson?: string;
}

/**
 * Chuẩn hoá dữ liệu đầu vào của form.
 * Khi xem lại đơn, SalesModule truyền SalesInvoiceResponseDto (invoiceCode, lines, previousDebt...)
 * trong khi form đọc dạng cũ (orderCode, products[{id,name,qty,price}]) -> map về dạng cũ (BUG-7).
 */
interface NormalizedInvoice {
  id?: string;
  status?: DocStatus;
  orderCode?: string;
  customerId?: string;
  customerName?: string;
  customerCode?: string;
  createdDate?: string;
  note?: string;
  oldDebt: number;
  advancePayment: number;
  paymentMethod?: 'CASH' | 'CREDIT' | 'MIXED';
  address?: string;
  phone?: string;
  contactPerson?: string;
  branch?: string;
  creator?: string;
  products: { id?: number | string; code?: string; name?: string; qty: number; price: number; unitOfMeasure?: string }[];
}

type LooseLine = {
  productId?: number | string;
  productCode?: string;
  productName?: string;
  quantity?: number;
  unitCost?: number;
  unitPrice?: number;
  unitOfMeasure?: string;
};

const normalizeInitialData = (data?: SalesInvoiceDto | null): NormalizedInvoice | null => {
  if (!data) return null;
  return {
    id: data.id,
    status: data.status,
    orderCode: data.invoiceCode,
    customerId: data.customerId,
    customerName: data.customerName,
    note: data.note,
    paymentMethod: data.paymentMethod,
    createdDate: data.createdAt ? String(data.createdAt).slice(0, 10) : undefined,
    oldDebt: Number(data.previousDebt ?? 0),
    advancePayment: Number(data.advancePayment ?? 0),
    products: ((data.lines ?? []) as LooseLine[]).map((l) => ({
      id: l.productId,
      code: l.productCode,
      name: l.productName,
      qty: Number(l.quantity),
      price: Number(l.unitPrice ?? l.unitCost ?? 0),
      unitOfMeasure: l.unitOfMeasure,
    })),
  };
};

type DialogState = null | 'CANCEL' | 'EXIT';

export const SalesOrderForm = forwardRef<SalesOrderFormRef, SalesOrderFormProps>(
  ({ mode: initialMode = 'VIEW', initialData: rawInitialData, onStateChange }, ref) => {
    const initialData = useMemo(() => normalizeInitialData(rawInitialData), [rawInitialData]);
    const { user } = useAuth();
    const isAdmin = user?.role === 'ADMIN';
    const { closeTab, activeTabId } = useTabs();

    const [mode, setMode] = useState<FormMode>(initialMode);
    const isView = mode === 'VIEW';
    const [currentInvoiceId, setCurrentInvoiceId] = useState<string | null>(initialData?.id || null);
    const [invoiceStatus, setInvoiceStatus] = useState<DocStatus>(initialData?.status || 'DRAFT');

    // Khối trái: thông tin nội bộ
    const branch = initialData?.branch || 'CN Trung Tâm';
    // Ngày chứng từ do backend ghi nhận (API tạo hóa đơn không nhận ngày) → chỉ hiển thị.
    const createdDate: string = initialData?.createdDate || toInputDate(new Date());
    const [orderCode, setOrderCode] = useState<string>(initialData?.orderCode || 'AUTO-GENERATE');
    const [note, setNote] = useState<string>(initialData?.note || '');

    // Khối giữa: khách hàng
    const [customerCode, setCustomerCode] = useState<string>(initialData?.customerCode || '');
    const [customerId, setCustomerId] = useState<string>(initialData?.customerId || '');
    const [customerName, setCustomerName] = useState<string>(initialData?.customerName || '');
    const [address, setAddress] = useState<string>(initialData?.address || '');
    const [phone, setPhone] = useState<string>(initialData?.phone || '');
    const [contactPerson, setContactPerson] = useState<string>(initialData?.contactPerson || '');

    // Khối phải: tài chính
    const [paymentMethod] = useState<'CASH' | 'CREDIT' | 'MIXED'>(initialData?.paymentMethod || 'CASH');
    const [advancePayment, setAdvancePayment] = useState<number>(initialData?.advancePayment || 0);
    const [oldDebt, setOldDebt] = useState<number>(initialData?.oldDebt || 0);
    const [discount, setDiscount] = useState<number>(0);
    const [tax, setTax] = useState<number>(0);

    const { lines: items, setLines: setItems, addLine, removeLine, updateLine, patchLine } = useDocumentLines(() =>
      (initialData?.products ?? []).map((p) => ({
        id: newLineId(),
        productId: p.id != null ? String(p.id) : '',
        productCode: p.code || '',
        productName: p.name || '',
        quantity: Number(p.qty) || 0,
        unitPrice: Number(p.price) || 0,
        unitOfMeasure: p.unitOfMeasure,
      })),
    );

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [dialog, setDialog] = useState<DialogState>(null);
    const [showPrintModal, setShowPrintModal] = useState(false);
    const [showCustomerSearch, setShowCustomerSearch] = useState(false);
    const [quickCustomer, setQuickCustomer] = useState<string | null>(null);
    const [quickProduct, setQuickProduct] = useState<{ lineId: string; name: string } | null>(null);

    // Xem lại hóa đơn cũ mà API không trả tên/mã SP → tra từ danh mục.
    const { data: products = [] } = useQuery<ProductDto[]>({
      queryKey: ['products'],
      queryFn: () => ApiService.Catalog.getProducts(),
      enabled: items.some((l) => l.productId && !l.productName),
    });
    const displayItems = useMemo(
      () =>
        items.map((l) => {
          if (l.productName || !l.productId) return l;
          const p = products.find((x) => String(x.id) === l.productId);
          return p ? { ...l, productName: p.name ?? '', productCode: p.code ?? '' } : l;
        }),
      [items, products],
    );

    const { createMutation, confirmMutation } = useSalesInvoice();

    const statusForUi: DocStatus | undefined = currentInvoiceId ? invoiceStatus : undefined;
    useEffect(() => {
      onStateChange?.(mode, isLoading, statusForUi);
    }, [mode, isLoading, statusForUi, onStateChange]);

    const totalAmount = sumLines(items);
    const finalAmount = totalAmount - discount + tax;
    const invoiceRemaining = Math.max(0, finalAmount - advancePayment);
    const remainingBalance = oldDebt + finalAmount - advancePayment;

    const closeCurrentTab = () => {
      if (activeTabId) closeTab(activeTabId);
    };

    const handleAdd = () => {
      setMode('ADD');
      setCurrentInvoiceId(null);
      setInvoiceStatus('DRAFT');
      setNote('');
      setAdvancePayment(0);
      setOldDebt(0);
      setDiscount(0);
      setTax(0);
      setFieldErrors({});
      setError(null);
      setOrderCode('AUTO-GENERATE');
      setCustomerCode('');
      setCustomerId('');
      setCustomerName('');
      setAddress('');
      setPhone('');
      setContactPerson('');
      setItems([]);
    };

    const selectCustomer = async (c: CustomerLike) => {
      setCustomerCode(c.customerCode || '');
      setCustomerId(c.id || '');
      setCustomerName(c.name || '');
      setAddress(c.address || '');
      setPhone(c.phone || '');
      setContactPerson(c.contactPerson || '');
      setFieldErrors((prev) => omitKey(prev, 'partner'));
      if (!c.id) return;
      try {
        const debt = await ApiService.Debt.getBalance(c.id);
        setOldDebt(Number(debt) || 0);
      } catch (e) {
        console.error('Failed to fetch debt', e);
        setOldDebt(0);
      }
    };

    const selectProduct = (lineId: string, p: ProductLike) => {
      patchLine(lineId, productToLinePatch(p));
      focusLineCell(lineId, 'quantity');
    };

    const handleSubmit = async () => {
      if (mode !== 'ADD' || isLoading) return; // Backend chưa có API sửa hóa đơn
      setError(null);

      const newErrors: Record<string, string> = {};
      if (!customerId) newErrors.partner = 'Vui lòng chọn khách hàng';
      if (items.length === 0) {
        setFieldErrors(newErrors);
        toast.error('Đơn hàng phải có ít nhất 1 sản phẩm.');
        return;
      }
      Object.assign(newErrors, validateLines(items));
      setFieldErrors(newErrors);
      if (Object.keys(newErrors).length > 0) {
        toast.error('Vui lòng kiểm tra lại thông tin nhập.');
        return;
      }

      setIsLoading(true);
      try {
        const payload = {
          customerId,
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
        // Backend tạo và xác nhận đơn trong cùng 1 giao dịch: bấm "Lưu" là trừ tồn kho + cộng công nợ ngay.
        const response = await createMutation.mutateAsync(payload as components['schemas']['SalesInvoiceCreateDto']);
        if (response?.invoiceCode) setOrderCode(response.invoiceCode);
        if (response?.id) setCurrentInvoiceId(response.id);
        setInvoiceStatus('CONFIRMED');
        setMode('VIEW');
      } catch (err) {
        console.error(err);
        toast.error(errorMessage(err, 'Lỗi khi lưu đơn hàng'));
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
        toast.error('Đơn hàng đã được xác nhận (đã trừ tồn kho và cộng công nợ).');
        return;
      }
      setIsLoading(true);
      try {
        await confirmMutation.mutateAsync(currentInvoiceId);
        setInvoiceStatus('CONFIRMED');
      } catch (err) {
        console.error(err);
        toast.error(errorMessage(err, 'Lỗi khi xác nhận đơn hàng'));
      } finally {
        setIsLoading(false);
      }
    };

    useImperativeHandle(ref, () => ({
      handleAdd,
      handleCancel: () => {
        if (!isView) setDialog('CANCEL');
      },
      handleSubmit,
      handleConfirm,
      handlePrint: () => setShowPrintModal(true),
      handleExit: () => {
        if (!isView) setDialog('EXIT');
        else closeCurrentTab();
      },
    }));

    const printItems: OrderItem[] = displayItems;

    return (
      <div className="flex flex-col h-full bg-surface relative">
        <GenericDocumentForm
          mode={mode}
          docTitle="Phiếu bán hàng"
          docCode={orderCode || 'AUTO-GENERATE'}
          status={statusForUi}
          error={error}
          errors={fieldErrors}
          info={[
            { key: 'branch', label: 'Kho xuất', value: branch },
            { key: 'priceList', label: 'Lấy giá', value: 'Bán hàng theo khách' },
            { key: 'creator', label: 'Nhân viên', value: initialData?.creator || user?.username || '' },
          ]}
          createdDate={createdDate}
          partner={{
            label: 'Khách hàng',
            required: true,
            displayName: `${customerName}${customerCode ? ` (${customerCode})` : ''}`,
            onAdvancedSearch: () => setShowCustomerSearch(true),
            renderCombobox: (hasError) => (
              <SearchableCombobox<CustomerLike>
                data-testid="sales-customer-combo"
                value={customerName}
                placeholder="Nhập mã, tên hoặc SĐT khách hàng…"
                error={hasError}
                fetchData={(q) => ApiService.Catalog.searchCustomers(q) as Promise<CustomerLike[]>}
                columns={[
                  { header: 'Mã KH', field: 'customerCode', width: '90px' },
                  { header: 'Tên khách hàng', field: 'name' },
                  { header: 'Điện thoại', field: 'phone', width: '110px' },
                  { header: 'Địa chỉ', field: 'address', width: '30%' },
                ]}
                onSelect={selectCustomer}
                onCreateNew={isAdmin ? (q) => setQuickCustomer(q) : undefined}
                createLabel="Thêm khách hàng mới"
              />
            ),
            fields: [
              { key: 'contact', label: 'Người liên hệ', value: contactPerson, onChange: setContactPerson },
              { key: 'phone', label: 'Điện thoại', value: phone, onChange: setPhone },
              { key: 'address', label: 'Địa chỉ', value: address, onChange: setAddress },
              { key: 'note', label: 'Ghi chú', value: note, onChange: setNote, testId: 'sales-note' },
            ],
          }}
          summary={[
            { key: 'oldDebt', label: 'Nợ trước', value: oldDebt, onChange: setOldDebt, allowNegative: true, testId: 'sum-old-debt' },
            { key: 'total', label: 'Tiền hàng', value: totalAmount, testId: 'sum-total' },
            { key: 'discount', label: 'Chiết khấu', value: discount, onChange: setDiscount, testId: 'sum-discount' },
            { key: 'tax', label: 'VAT', value: tax, onChange: setTax, testId: 'sum-tax' },
            { key: 'advance', label: 'Trả trước', value: advancePayment, onChange: setAdvancePayment, testId: 'sum-advance' },
            { key: 'invoiceRemaining', label: 'Còn của đơn', value: invoiceRemaining, tone: 'primary', strong: true, testId: 'sum-invoice-remaining' },
            { key: 'newDebt', label: 'Nợ tổng mới', value: remainingBalance, tone: 'danger', strong: true, testId: 'sum-new-debt' },
          ]}
          lines={{
            testIdPrefix: 'sales',
            items: displayItems,
            priceLabel: 'Đơn giá',
            onAdd: addLine,
            onRemove: removeLine,
            onUpdate: updateLine,
            renderProductCombobox: (line, _index, hasError) => (
              <SearchableCombobox
                data-testid={`sales-product-combo-${line.id}`}
                variant="cell"
                value={line.productName}
                placeholder="Nhấn để chọn..."
                error={hasError}
                fetchData={(q) => ApiService.Catalog.searchProducts(q)}
                columns={[
                  { header: 'Mã', field: 'code', width: '100px' },
                  { header: 'Tên hàng', field: 'name' },
                  { header: 'ĐVT', field: 'baseUnit', width: '70px' },
                  { header: 'Giá bán', field: 'price', width: '120px', align: 'right', format: (v) => formatCurrency(v ?? 0) },
                ]}
                onSelect={(p) => selectProduct(line.id, p)}
                onCreateNew={isAdmin ? (q) => setQuickProduct({ lineId: line.id, name: q }) : undefined}
                createLabel="Thêm sản phẩm mới"
              />
            ),
          }}
        />

        {/* Xem trước phiếu in */}
        {showPrintModal && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Xem trước phiếu in"
            className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4"
          >
            <div className="card shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
              <div className="h-12 px-4 border-b border-line flex justify-between items-center bg-slate-50">
                <h2 className="text-[15px] font-semibold text-ink">Xem trước phiếu in</h2>
                <div className="flex gap-2">
                  <button type="button" onClick={() => setShowPrintModal(false)} className="btn btn-secondary">
                    <X size={14} /> Đóng
                  </button>
                  <button type="button" onClick={() => window.print()} className="btn btn-primary">
                    <Printer size={14} /> In ngay
                  </button>
                </div>
              </div>
              <div className="p-8 overflow-y-auto bg-slate-200 flex-1 flex justify-center">
                <div className="bg-surface shadow-sm" style={{ width: '148mm', minHeight: '210mm' }}>
                  <PrintInvoice
                    customerName={customerName}
                    items={printItems}
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
          className="hidden print:block fixed inset-0 z-[9999] bg-surface w-full h-full"
          customerName={customerName}
          items={printItems}
          totalAmount={totalAmount}
          advancePayment={advancePayment}
          invoiceRemaining={invoiceRemaining}
          remainingBalance={remainingBalance}
          oldDebt={oldDebt}
          mode="sales"
        />

        <SearchModal<CustomerLike>
          isOpen={showCustomerSearch}
          onClose={() => setShowCustomerSearch(false)}
          title="Tìm khách hàng"
          placeholder="Nhập mã, tên hoặc SĐT…"
          fetchData={(q) => ApiService.Catalog.searchCustomers(q) as Promise<CustomerLike[]>}
          renderItem={(c) => (
            <div>
              <div className="font-medium text-ink">
                {c.name} {c.customerCode ? <span className="text-ink-subtle">({c.customerCode})</span> : null}
              </div>
              <div className="text-[12px] text-ink-muted">{[c.phone, c.address].filter(Boolean).join(' · ')}</div>
            </div>
          )}
          onSelect={selectCustomer}
        />

        <QuickCreateCustomer
          isOpen={quickCustomer !== null}
          initialName={quickCustomer ?? ''}
          onClose={() => setQuickCustomer(null)}
          onCreated={(c) => c && selectCustomer(c)}
        />
        <QuickCreateProduct
          isOpen={quickProduct !== null}
          initialName={quickProduct?.name ?? ''}
          onClose={() => setQuickProduct(null)}
          onCreated={(p) => {
            if (p && quickProduct) selectProduct(quickProduct.lineId, p);
          }}
        />

        <ConfirmDialog
          isOpen={dialog !== null}
          title={dialog === 'EXIT' ? 'Xác nhận thoát' : 'Hủy thay đổi'}
          message={
            dialog === 'EXIT'
              ? 'Dữ liệu chưa được lưu. Bạn có chắc chắn muốn thoát và mất các thay đổi không?'
              : 'Bạn có chắc chắn muốn hủy các thay đổi chưa lưu không?'
          }
          onConfirm={() => {
            const d = dialog;
            setDialog(null);
            if (d === 'EXIT' || (initialMode === 'ADD' && !currentInvoiceId)) closeCurrentTab();
            else setMode('VIEW');
          }}
          onCancel={() => setDialog(null)}
        />
      </div>
    );
  },
);
SalesOrderForm.displayName = 'SalesOrderForm';
