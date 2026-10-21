import { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Printer, TriangleAlert, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { notify } from '../../shared/notifications/notification';
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
import {
  hasErrorCode,
  INVALID_STATUS,
  OutcomeUnknownError,
  submitErrorMessage,
  useSubmitKey,
} from '../../shared/idempotency/submitKey';
import type { DocStatus, DocumentLine, FormMode, ProductDto, SalesInvoiceDto } from '../../types/documents';
import {
  applyResolvedPrice,
  isLinePriceEdited,
  linePriceInfo,
  listPricePatch,
  SAVE_PRICE_NOTE,
} from './customerPricing';
import type { components } from '@erp/api-contract';

export type { FormMode };

export interface SalesOrderFormProps {
  mode?: FormMode;
  initialData?: SalesInvoiceDto | null;
  onStateChange?: (mode: FormMode, isLoading: boolean, status?: DocStatus) => void;
}

export interface SalesOrderFormRef {
  handleAdd: () => void;
  /** Sửa hóa đơn đang ở trạng thái nháp (F3). */
  handleEdit: () => void;
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
  address?: string;
  phone?: string;
  contactPerson?: string;
  branch?: string;
  creator?: string;
  products: { id?: number | string; code?: string; name?: string; qty: number; price: number; unitOfMeasure?: string }[];
}

const normalizeInitialData = (data?: SalesInvoiceDto | null): NormalizedInvoice | null => {
  if (!data) return null;
  return {
    id: data.id,
    status: data.status,
    orderCode: data.invoiceCode,
    customerId: data.customerId,
    customerName: data.customerName,
    note: data.note,
    // createdAt có độ lệch múi giờ: quy đổi sang ngày địa phương thay vì cắt chuỗi (ngày UTC).
    createdDate: data.createdAt ? toInputDate(new Date(String(data.createdAt))) : undefined,
    oldDebt: Number(data.previousDebt ?? 0),
    advancePayment: Number(data.advancePayment ?? 0),
    products: (data.lines ?? []).map((l) => ({
      id: l.productId,
      name: l.productName,
      qty: Number(l.quantity),
      price: Number(l.unitPrice ?? 0),
      unitOfMeasure: l.unitOfMeasure,
    })),
  };
};

type DialogState = null | 'CANCEL' | 'EXIT';

/** Nội dung phiếu lúc bấm "Sửa": bấm Hủy thì trả form về đúng nội dung nháp đã lưu. */
interface SalesSnapshot {
  customerId: string;
  customerCode: string;
  customerName: string;
  address: string;
  phone: string;
  contactPerson: string;
  note: string;
  advancePayment: number;
  oldDebt: number;
  discount: number;
  tax: number;
  items: DocumentLine[];
}


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
    // Khách đang chọn — dùng để bỏ qua kết quả tra giá đã cũ khi nhân viên đổi khách liên tục.
    const customerIdRef = useRef<string>(initialData?.customerId || '');
    const [customerName, setCustomerName] = useState<string>(initialData?.customerName || '');
    const [address, setAddress] = useState<string>(initialData?.address || '');
    const [phone, setPhone] = useState<string>(initialData?.phone || '');
    const [contactPerson, setContactPerson] = useState<string>(initialData?.contactPerson || '');

    // Khối phải: tài chính
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

    // Xem lại hóa đơn cũ mà API không trả mã SP (hoặc tên) → tra từ danh mục.
    const { data: products = [] } = useQuery<ProductDto[]>({
      queryKey: ['products'],
      queryFn: () => ApiService.Catalog.getProducts(),
      enabled: items.some((l) => l.productId && (!l.productName || !l.productCode)),
    });
    const displayItems = useMemo(
      () =>
        items.map((l) => {
          // Hóa đơn mở lại từ danh sách chỉ có tên SP (API không trả mã) → bổ sung mã/tên từ danh mục.
          if (!l.productId || (l.productName && l.productCode)) return l;
          const p = products.find((x) => String(x.id) === l.productId);
          return p ? { ...l, productName: l.productName || p.name || '', productCode: l.productCode || p.code || '' } : l;
        }),
      [items, products],
    );

    const itemsRef = useRef(items);
    useEffect(() => {
      itemsRef.current = items;
    }, [items]);

    /** Tra giá theo khách cho các sản phẩm rồi cập nhật dòng (chỉ dòng onlyLineId nếu truyền). */
    const refreshCustomerPrices = async (custId: string, productIds: string[], onlyLineId?: string) => {
      const ids = Array.from(new Set(productIds.filter(Boolean)));
      if (!custId || ids.length === 0) return;
      try {
        const resolved = await ApiService.CustomerPrice.resolve(custId, ids);
        if (customerIdRef.current !== custId) return; // đã đổi khách trong lúc chờ
        const byProduct = new Map(resolved.map((r) => [String(r.productId), r]));
        setItems((prev) =>
          prev.map((l) => {
            if (onlyLineId && l.id !== onlyLineId) return l;
            const r = byProduct.get(l.productId);
            return r ? applyResolvedPrice(l, r) : l;
          }),
        );
      } catch (e) {
        console.error('Failed to resolve customer prices', e);
        toast.error('Không lấy được giá theo khách hàng — đang dùng giá niêm yết.');
      }
    };

    const hasCustomer = !!customerId;
    const editedPriceCount = isView ? 0 : displayItems.filter((l) => isLinePriceEdited(l, hasCustomer)).length;

    const { createDraftMutation, updateDraftMutation, confirmMutation } = useSalesInvoice();
    const editSnapshotRef = useRef<SalesSnapshot | null>(null);
    // Một key cho một lần lập phiếu: mọi lần bấm Lưu (kể cả gửi lại sau khi mạng lỗi) gửi cùng key,
    // nên backend chỉ tạo đúng một hóa đơn. Chỉ đổi key khi bắt đầu phiếu mới (handleAdd).
    const submitKey = useSubmitKey();

    const statusForUi: DocStatus | undefined = currentInvoiceId ? invoiceStatus : undefined;
    useEffect(() => {
      onStateChange?.(mode, isLoading, statusForUi);
    }, [mode, isLoading, statusForUi, onStateChange]);

    // Hóa đơn nháp chưa chụp nợ cũ (chỉ chụp khi xác nhận) -> hiển thị công nợ hiện tại của khách.
    useEffect(() => {
      if (initialData?.status !== 'DRAFT' || !initialData.customerId) return;
      let cancelled = false;
      ApiService.Debt.getBalance(initialData.customerId)
        .then((debt) => {
          if (!cancelled) setOldDebt(Number(debt) || 0);
        })
        .catch(() => undefined);
      return () => {
        cancelled = true;
      };
    }, [initialData]);

    const totalAmount = sumLines(items);
    const finalAmount = totalAmount - discount + tax;
    const invoiceRemaining = Math.max(0, finalAmount - advancePayment);
    const remainingBalance = oldDebt + finalAmount - advancePayment;

    const closeCurrentTab = () => {
      if (activeTabId) closeTab(activeTabId);
    };

    const handleAdd = () => {
      submitKey.renew();
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
      customerIdRef.current = '';
      setCustomerName('');
      setAddress('');
      setPhone('');
      setContactPerson('');
      setItems([]);
    };

    /** Bắt đầu sửa hóa đơn nháp. Hóa đơn đã xác nhận (đã trừ kho, ghi công nợ) thì không sửa được. */
    const handleEdit = () => {
      if (mode !== 'VIEW' || !currentInvoiceId || invoiceStatus !== 'DRAFT' || isLoading) return;
      editSnapshotRef.current = {
        customerId,
        customerCode,
        customerName,
        address,
        phone,
        contactPerson,
        note,
        advancePayment,
        oldDebt,
        discount,
        tax,
        items,
      };
      submitKey.renew(); // key mới cho lần sửa này
      setError(null);
      setFieldErrors({});
      setMode('EDIT');
    };

    /** Bỏ các thay đổi chưa lưu, trả về nội dung nháp lúc bấm "Sửa". */
    const restoreSnapshot = () => {
      const s = editSnapshotRef.current;
      editSnapshotRef.current = null;
      if (!s) return;
      setCustomerId(s.customerId);
      customerIdRef.current = s.customerId;
      setCustomerCode(s.customerCode);
      setCustomerName(s.customerName);
      setAddress(s.address);
      setPhone(s.phone);
      setContactPerson(s.contactPerson);
      setNote(s.note);
      setAdvancePayment(s.advancePayment);
      setOldDebt(s.oldDebt);
      setDiscount(s.discount);
      setTax(s.tax);
      setItems(s.items);
      setFieldErrors({});
      setError(null);
    };

    const selectCustomer = async (c: CustomerLike) => {
      setCustomerCode(c.customerCode || '');
      setCustomerId(c.id || '');
      customerIdRef.current = c.id || '';
      setCustomerName(c.name || '');
      setAddress(c.address || '');
      setPhone(c.phone || '');
      setContactPerson(c.contactPerson || '');
      setFieldErrors((prev) => omitKey(prev, 'partner'));
      if (!c.id) return;
      // Đổi khách -> áp lại giá theo khách mới cho các dòng chưa sửa giá tay.
      void refreshCustomerPrices(
        c.id,
        itemsRef.current.map((l) => l.productId),
      );
      try {
        const debt = await ApiService.Debt.getBalance(c.id);
        setOldDebt(Number(debt) || 0);
      } catch (e) {
        console.error('Failed to fetch debt', e);
        setOldDebt(0);
      }
    };

    const selectProduct = (lineId: string, p: ProductLike) => {
      const patch = productToLinePatch(p);
      // Tạm dùng giá niêm yết; nếu đã chọn khách thì tra giá riêng của khách ngay sau đó.
      patchLine(lineId, { ...patch, ...listPricePatch(p) });
      focusLineCell(lineId, 'quantity');
      if (customerIdRef.current && patch.productId) {
        void refreshCustomerPrices(customerIdRef.current, [patch.productId], lineId);
      }
    };

    const handleSubmit = async () => {
      if ((mode !== 'ADD' && mode !== 'EDIT') || isLoading) return;
      const isEdit = mode === 'EDIT';
      if (isEdit && !currentInvoiceId) return;
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
        if (isEdit && currentInvoiceId) {
          // Sửa nháp: ghi đè nội dung phiếu, vẫn chưa trừ kho / ghi công nợ.
          await updateDraftMutation.mutateAsync({
            id: currentInvoiceId,
            payload: payload as components['schemas']['SalesInvoiceCreateDto'],
            editSession: submitKey.key,
          });
          editSnapshotRef.current = null;
          setInvoiceStatus('DRAFT');
          setMode('VIEW');
          notify.success('Đã lưu thay đổi phiếu bán hàng (nháp). Bấm "Xác nhận" (F9) để xuất kho và ghi công nợ.');
          try {
            const fresh = await ApiService.SalesInvoice.getById(currentInvoiceId);
            if (fresh?.invoiceCode) setOrderCode(fresh.invoiceCode);
            if (fresh?.status) setInvoiceStatus(fresh.status as DocStatus);
          } catch {
            /* Đã lưu; không tải lại được thì giữ nội dung đang hiển thị. */
          }
          return;
        }
        // Bước 1: lưu nháp (chưa trừ kho, chưa ghi công nợ). Bước 2 là nút "Xác nhận" (F9).
        const response = await createDraftMutation.mutateAsync({
          payload: payload as components['schemas']['SalesInvoiceCreateDto'],
          idempotencyKey: submitKey.key,
        });
        if (response?.invoiceCode) setOrderCode(response.invoiceCode);
        if (response?.id) setCurrentInvoiceId(response.id);
        setInvoiceStatus('DRAFT');
        setMode('VIEW');
        notify.success('Đã lưu phiếu bán hàng (nháp). Bấm "Xác nhận" (F9) để xuất kho và ghi công nợ.');
      } catch (err) {
        console.error(err);
        const msg = submitErrorMessage(err, 'Lỗi khi lưu đơn hàng');
        if (isEdit && currentInvoiceId && hasErrorCode(err, INVALID_STATUS)) {
          // Phiếu đã được xác nhận ở nơi khác: bỏ chỉnh sửa, hiển thị trạng thái mới nhất.
          restoreSnapshot();
          setMode('VIEW');
          try {
            const fresh = await ApiService.SalesInvoice.getById(currentInvoiceId);
            if (fresh?.status) setInvoiceStatus(fresh.status as DocStatus);
          } catch {
            /* giữ trạng thái đang hiển thị */
          }
        }
        // Chưa rõ kết quả: giữ thông báo trên form để người dùng biết bấm Lưu lại là an toàn.
        if (err instanceof OutcomeUnknownError) setError(msg);
        notify.error(msg);
      } finally {
        setIsLoading(false);
      }
    };

    const handleConfirm = async () => {
      if (!currentInvoiceId || invoiceStatus !== 'DRAFT' || isLoading) return;
      setError(null);
      setIsLoading(true);
      try {
        await confirmMutation.mutateAsync(currentInvoiceId);
        setInvoiceStatus('CONFIRMED');
        notify.success('Đã xác nhận phiếu bán hàng: đã xuất kho và ghi công nợ.');
        try {
          // Nợ cũ được chụp lúc xác nhận -> hiển thị đúng số đã chụp trên hóa đơn.
          const fresh = await ApiService.SalesInvoice.getById(currentInvoiceId);
          if (fresh?.previousDebt != null) setOldDebt(Number(fresh.previousDebt));
        } catch {
          /* Đã xác nhận; không tải lại được thì giữ số đang hiển thị. */
        }
      } catch (err) {
        console.error(err);
        const msg = submitErrorMessage(err, 'Xác nhận phiếu bán hàng thất bại', 'confirm');
        if (err instanceof OutcomeUnknownError) setError(msg);
        notify.error(msg);
      } finally {
        setIsLoading(false);
      }
    };


    useImperativeHandle(ref, () => ({
      handleAdd,
      handleEdit,
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
            { key: 'priceList', label: 'Lấy giá', value: 'Giá riêng KH → Niêm yết' },
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
            priceInfo: isView ? undefined : (l: DocumentLine) => linePriceInfo(l, hasCustomer),
            footerNote:
              editedPriceCount > 0 ? (
                <span className="inline-flex items-center gap-1.5">
                  <TriangleAlert size={14} className="text-warning shrink-0" aria-hidden />
                  <span>
                    <strong>{editedPriceCount} dòng</strong> đã sửa giá. {SAVE_PRICE_NOTE}
                  </span>
                </span>
              ) : undefined,
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
              : mode === 'EDIT'
                ? 'Bỏ các thay đổi chưa lưu? Phiếu nháp sẽ giữ nguyên nội dung đã lưu trước đó.'
                : 'Bạn có chắc chắn muốn hủy các thay đổi chưa lưu không?'
          }
          onConfirm={() => {
            const d = dialog;
            setDialog(null);
            if (d === 'CANCEL' && mode === 'EDIT') {
              restoreSnapshot();
              setMode('VIEW');
            } else if (d === 'EXIT' || (initialMode === 'ADD' && !currentInvoiceId)) closeCurrentTab();
            else setMode('VIEW');
          }}
          onCancel={() => setDialog(null)}
        />
      </div>
    );
  },
);
SalesOrderForm.displayName = 'SalesOrderForm';
