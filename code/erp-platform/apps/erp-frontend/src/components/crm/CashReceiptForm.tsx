import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Info } from 'lucide-react';
import toast from 'react-hot-toast';
import { ApiService } from '../../api/ApiService';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { GenericDocumentForm } from '../common/document/GenericDocumentForm';
import { omitKey } from '../common/document/documentLines';
import { SearchableCombobox } from '../common/SearchableCombobox';
import { SearchModal } from '../common/SearchModal';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { notify } from '../../shared/notifications/notification';
import { submitErrorMessage, submitWithKey, useSubmitKey } from '../../shared/idempotency/submitKey';
import { formatCurrency, formatDateTime, toInputDate } from '../../shared/utils/format';
import type { FormMode, SummaryField } from '../../types/documents';
import type { CashReceiptDto, CashReceiptStatus, ReceiptCustomer } from '../../types/cashReceipt';
import { remainingAfterReceipt, validateReceipt } from './cashReceipt';

export interface CashReceiptFormProps {
  initialData?: CashReceiptDto | null;
  /** Mở từ màn Công nợ: chọn sẵn khách hàng. */
  initialCustomer?: ReceiptCustomer;
  /** canCancelReceipt: đang xem một phiếu đã ghi sổ (cho phép "Hủy phiếu thu"). */
  onStateChange?: (mode: FormMode, isLoading: boolean, canCancelReceipt: boolean) => void;
}

export interface CashReceiptFormRef {
  handleAdd: () => void;
  handleCancel: () => void;
  handleSubmit: () => void;
  handleExit: () => void;
  handleCancelReceipt: () => void;
}

interface CustomerLike {
  id?: string;
  customerCode?: string;
  name?: string;
  phone?: string;
  address?: string;
}

type DialogState = null | 'CANCEL' | 'EXIT';

const today = () => toInputDate(new Date());

export const CashReceiptForm = forwardRef<CashReceiptFormRef, CashReceiptFormProps>(
  ({ initialData, initialCustomer, onStateChange }, ref) => {
    const { user } = useAuth();
    const { closeTab, activeTabId } = useTabs();
    const queryClient = useQueryClient();

    const [mode, setMode] = useState<FormMode>(initialData ? 'VIEW' : 'ADD');
    const isView = mode === 'VIEW';

    // Phiếu đã lưu (chế độ xem)
    const [receiptId, setReceiptId] = useState<string | null>(initialData?.id ?? null);
    const [receiptCode, setReceiptCode] = useState<string>(initialData?.receiptCode ?? 'AUTO-GENERATE');
    const [status, setStatus] = useState<CashReceiptStatus | undefined>(initialData?.status);
    const [createdAt, setCreatedAt] = useState<string | null>(initialData?.createdAt ?? null);
    const [cancelledAt, setCancelledAt] = useState<string | null>(initialData?.cancelledAt ?? null);
    const [cancelReason, setCancelReason] = useState<string | null>(initialData?.cancelReason ?? null);
    const [debtAfter, setDebtAfter] = useState<number>(Number(initialData?.debtAfter ?? 0));

    // Nội dung phiếu
    const [receiptDate, setReceiptDate] = useState<string>(initialData?.receiptDate ?? today());
    const [note, setNote] = useState<string>(initialData?.note ?? '');
    const [customerId, setCustomerId] = useState<string>(initialData?.customerId ?? '');
    const customerIdRef = useRef<string>(initialData?.customerId ?? '');
    const [customerCode, setCustomerCode] = useState<string>(initialData?.customerCode ?? '');
    const [customerName, setCustomerName] = useState<string>(initialData?.customerName ?? '');
    const [phone, setPhone] = useState<string>(initialData?.customerPhone ?? '');
    const [address, setAddress] = useState<string>(initialData?.customerAddress ?? '');
    // ADD: nợ hiện tại của khách tại chi nhánh. VIEW: nợ trước khi thu (snapshot trên phiếu).
    const [debt, setDebt] = useState<number>(Number(initialData?.debtBefore ?? 0));
    const [debtLoading, setDebtLoading] = useState(false);
    const [amount, setAmount] = useState<number>(Number(initialData?.amount ?? 0));

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [dialog, setDialog] = useState<DialogState>(null);
    // Một key cho một lần lập phiếu (chống tạo trùng khi bấm Lưu lại sau lỗi mạng); đổi khi "Thêm mới".
    const submitKey = useSubmitKey();
    const [showCustomerSearch, setShowCustomerSearch] = useState(false);
    const [showCancelReceipt, setShowCancelReceipt] = useState(false);
    const [cancelReasonInput, setCancelReasonInput] = useState('');

    const canCancelReceipt = isView && !!receiptId && status === 'POSTED';
    useEffect(() => {
      onStateChange?.(mode, isLoading, canCancelReceipt);
    }, [mode, isLoading, canCancelReceipt, onStateChange]);

    const remaining = isView ? debtAfter : remainingAfterReceipt(debt, amount);

    const closeCurrentTab = () => {
      if (activeTabId) closeTab(activeTabId);
    };

    const invalidateDebtViews = (custId: string) => {
      void queryClient.invalidateQueries({ queryKey: ['debts'] });
      void queryClient.invalidateQueries({ queryKey: ['debt-movements', custId] });
      void queryClient.invalidateQueries({ queryKey: ['cashReceipts'] });
    };

    const applyReceipt = (r: CashReceiptDto) => {
      setReceiptId(r.id);
      setReceiptCode(r.receiptCode);
      setStatus(r.status);
      setCreatedAt(r.createdAt ?? null);
      setCancelledAt(r.cancelledAt ?? null);
      setCancelReason(r.cancelReason ?? null);
      setReceiptDate(r.receiptDate);
      setNote(r.note ?? '');
      setCustomerId(r.customerId);
      customerIdRef.current = r.customerId;
      setCustomerCode(r.customerCode ?? '');
      setCustomerName(r.customerName ?? '');
      setPhone(r.customerPhone ?? '');
      setAddress(r.customerAddress ?? '');
      setDebt(Number(r.debtBefore) || 0);
      setAmount(Number(r.amount) || 0);
      setDebtAfter(Number(r.debtAfter) || 0);
    };

    const selectCustomer = useCallback(async (c: CustomerLike) => {
      const id = c.id || '';
      setCustomerId(id);
      customerIdRef.current = id;
      setCustomerCode(c.customerCode || '');
      setCustomerName(c.name || '');
      setPhone(c.phone || '');
      setAddress(c.address || '');
      setFieldErrors((prev) => omitKey(omitKey(prev, 'partner'), 'amount'));
      setDebt(0);
      setAmount(0);
      if (!id) return;
      setDebtLoading(true);
      try {
        const balance = await ApiService.Debt.getBalance(id);
        if (customerIdRef.current !== id) return; // đã đổi khách trong lúc chờ
        setDebt(Number(balance) || 0);
      } catch (e) {
        console.error('Failed to fetch debt', e);
        toast.error('Không lấy được số nợ hiện tại của khách hàng.');
      } finally {
        if (customerIdRef.current === id) setDebtLoading(false);
      }
    }, []);

    // Mở từ màn Công nợ ("Thu tiền"): chọn sẵn khách một lần.
    const initialCustomerApplied = useRef(false);
    useEffect(() => {
      if (initialData || !initialCustomer || initialCustomerApplied.current) return;
      initialCustomerApplied.current = true;
      void selectCustomer({
        id: initialCustomer.id,
        customerCode: initialCustomer.customerCode ?? undefined,
        name: initialCustomer.name ?? undefined,
        phone: initialCustomer.phone ?? undefined,
        address: initialCustomer.address ?? undefined,
      });
    }, [initialData, initialCustomer, selectCustomer]);

    const handleAdd = () => {
      submitKey.renew();
      setMode('ADD');
      setReceiptId(null);
      setReceiptCode('AUTO-GENERATE');
      setStatus(undefined);
      setCreatedAt(null);
      setCancelledAt(null);
      setCancelReason(null);
      setDebtAfter(0);
      setReceiptDate(today());
      setNote('');
      setCustomerId('');
      customerIdRef.current = '';
      setCustomerCode('');
      setCustomerName('');
      setPhone('');
      setAddress('');
      setDebt(0);
      setAmount(0);
      setError(null);
      setFieldErrors({});
    };

    const handleSubmit = async () => {
      if (mode !== 'ADD' || isLoading) return;
      setError(null);
      const errors = validateReceipt({ customerId, debt, amount, receiptDate, today: today() });
      setFieldErrors(errors);
      if (Object.keys(errors).length > 0) {
        toast.error('Vui lòng kiểm tra lại thông tin nhập.');
        return;
      }
      setIsLoading(true);
      try {
        const payload = {
          customerId,
          amount,
          receiptDate,
          note: note.trim() || undefined,
        };
        const created = await submitWithKey(() => ApiService.CashReceipt.create(payload, submitKey.key));
        try {
          applyReceipt(await ApiService.CashReceipt.getById(created.id));
        } catch {
          // Đã lưu thành công; chỉ không tải lại được snapshot -> hiển thị theo số liệu trên màn hình.
          setReceiptId(created.id);
          setReceiptCode(created.receiptCode);
          setStatus('POSTED');
          setDebtAfter(remainingAfterReceipt(debt, amount));
        }
        setMode('VIEW');
        notify.success(`Đã lập phiếu thu ${created.receiptCode}`);
        invalidateDebtViews(customerId);
      } catch (err) {
        console.error(err);
        const msg = submitErrorMessage(err, 'Lỗi khi lưu phiếu thu');
        setError(msg);
        notify.error(msg);
        // Số nợ có thể đã đổi (người khác vừa thu) -> tải lại để người dùng thấy số mới.
        if (customerIdRef.current) {
          ApiService.Debt.getBalance(customerIdRef.current)
            .then((b) => setDebt(Number(b) || 0))
            .catch(() => undefined);
        }
      } finally {
        setIsLoading(false);
      }
    };

    const handleConfirmCancelReceipt = async () => {
      if (!receiptId || isLoading) return;
      setIsLoading(true);
      try {
        const r = await submitWithKey(() => ApiService.CashReceipt.cancel(receiptId, cancelReasonInput));
        applyReceipt(r);
        setShowCancelReceipt(false);
        setCancelReasonInput('');
        notify.success(`Đã hủy phiếu thu ${r.receiptCode}. Công nợ đã được cộng lại ${formatCurrency(r.amount)}.`);
        invalidateDebtViews(r.customerId);
      } catch (err) {
        console.error(err);
        notify.error(submitErrorMessage(err, 'Lỗi khi hủy phiếu thu', 'cancel'));
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
      handleExit: () => {
        if (!isView && customerId) setDialog('EXIT');
        else closeCurrentTab();
      },
      handleCancelReceipt: () => {
        if (canCancelReceipt) setShowCancelReceipt(true);
      },
    }));

    const summary: SummaryField[] = [
      {
        key: 'debt',
        label: isView ? 'Nợ trước thu' : 'Số nợ hiện tại',
        value: debt,
        tone: 'danger',
        testId: 'receipt-debt',
      },
      {
        key: 'amount',
        label: 'Số tiền thu',
        value: amount,
        onChange: isView ? undefined : setAmount,
        tone: 'primary',
        strong: isView,
        testId: 'receipt-amount',
        action: {
          label: 'Thu hết',
          onClick: () => {
            setAmount(debt);
            setFieldErrors((prev) => omitKey(prev, 'amount'));
          },
          disabled: debt <= 0 || debtLoading,
          testId: 'receipt-pay-all',
        },
      },
      { key: 'remaining', label: 'Còn lại', value: remaining, tone: 'danger', strong: true, testId: 'receipt-remaining' },
    ];

    const fieldErrorList = ['amount', 'date'].map((k) => fieldErrors[k]).filter(Boolean);

    return (
      <div className="flex flex-col h-full bg-surface relative">
        <GenericDocumentForm
          mode={mode}
          docTitle="Phiếu thu"
          docCode={receiptCode}
          status={status}
          error={error}
          errors={fieldErrors}
          testIdPrefix="receipt"
          info={[
            { key: 'type', label: 'Loại phiếu', value: 'Thu nợ khách hàng' },
            { key: 'creator', label: 'Nhân viên', value: user?.username || '' },
          ]}
          createdDate={receiptDate}
          createdDateLabel="Ngày thu"
          createdDateMax={today()}
          onCreatedDateChange={isView ? undefined : (v) => {
            setReceiptDate(v);
            setFieldErrors((prev) => omitKey(prev, 'date'));
          }}
          partner={{
            label: 'Khách hàng',
            required: true,
            displayName: `${customerName}${customerCode ? ` (${customerCode})` : ''}`,
            onAdvancedSearch: () => setShowCustomerSearch(true),
            renderCombobox: (hasError) => (
              <SearchableCombobox<CustomerLike>
                data-testid="receipt-customer-combo"
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
                onSelect={(c) => void selectCustomer(c)}
              />
            ),
            fields: [
              { key: 'code', label: 'Mã KH', value: customerCode || '—', testId: 'receipt-customer-code' },
              { key: 'phone', label: 'Điện thoại', value: phone || '—', testId: 'receipt-customer-phone' },
              { key: 'address', label: 'Địa chỉ', value: address || '—', testId: 'receipt-customer-address' },
              {
                key: 'note',
                label: 'Diễn giải',
                value: note,
                onChange: isView ? undefined : setNote,
                placeholder: 'Ghi chú (nếu cần)',
                testId: 'receipt-note',
              },
            ],
          }}
          summary={summary}
        >
          <div className="p-4 flex flex-col gap-3 max-w-3xl">
            {!isView && fieldErrorList.length > 0 && (
              <div role="alert" data-testid="receipt-field-errors" className="px-3 py-2 rounded-[4px] bg-danger-soft text-danger text-[13px]">
                {fieldErrorList.map((m) => (
                  <div key={m}>{m}</div>
                ))}
              </div>
            )}
            {!isView && debtLoading && <div className="text-[13px] text-ink-muted">Đang tải số nợ hiện tại…</div>}
            {isView && status === 'CANCELLED' && (
              <div data-testid="receipt-cancelled-info" className="px-3 py-2 rounded-[4px] bg-danger-soft text-danger text-[13px]">
                Phiếu đã hủy{cancelledAt ? ` lúc ${formatDateTime(cancelledAt)}` : ''}. Công nợ của khách đã được cộng lại{' '}
                {formatCurrency(amount)}.{cancelReason ? ` Lý do: ${cancelReason}` : ''}
              </div>
            )}
            {isView && createdAt && (
              <div className="text-[13px] text-ink-muted">Lập lúc {formatDateTime(createdAt)}</div>
            )}
            <div className="flex gap-2 text-[13px] text-ink-muted">
              <Info size={16} className="shrink-0 mt-0.5 text-primary" aria-hidden />
              <span>
                Phiếu thu trừ vào <strong>tổng nợ của khách tại chi nhánh</strong>, không gắn với một hóa đơn cụ thể. Phiếu đã
                lưu không sửa được; nếu nhập sai, hãy <strong>hủy phiếu</strong> (F8) rồi lập phiếu mới.
              </span>
            </div>
          </div>
        </GenericDocumentForm>

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
          onSelect={(c) => void selectCustomer(c)}
        />

        {showCancelReceipt && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Hủy phiếu thu"
            className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4"
            onKeyDown={(e) => {
              if (e.key === 'Escape' && !isLoading) setShowCancelReceipt(false);
            }}
          >
            <div className="card shadow-xl w-full max-w-md p-4 flex flex-col gap-3">
              <h2 className="text-[15px] font-semibold text-ink">Hủy phiếu thu {receiptCode}?</h2>
              <p className="text-[13px] text-ink-muted">
                Công nợ của khách <strong className="text-ink">{customerName}</strong> sẽ được cộng lại{' '}
                <strong className="text-danger">{formatCurrency(amount)}</strong>. Phiếu vẫn được giữ lại với trạng thái "Đã hủy".
              </p>
              <label className="erp-label" htmlFor="cancel-receipt-reason">
                Lý do hủy (không bắt buộc)
              </label>
              <textarea
                id="cancel-receipt-reason"
                data-testid="receipt-cancel-reason"
                className="erp-input min-h-[72px] py-1"
                value={cancelReasonInput}
                onChange={(e) => setCancelReasonInput(e.target.value)}
                autoFocus
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={isLoading}
                  onClick={() => setShowCancelReceipt(false)}
                  data-testid="receipt-cancel-dismiss"
                >
                  Không hủy
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  disabled={isLoading}
                  onClick={() => void handleConfirmCancelReceipt()}
                  data-testid="receipt-cancel-confirm"
                >
                  Hủy phiếu thu
                </button>
              </div>
            </div>
          </div>
        )}

        <ConfirmDialog
          isOpen={dialog !== null}
          title={dialog === 'EXIT' ? 'Xác nhận thoát' : 'Hủy thay đổi'}
          message={
            dialog === 'EXIT'
              ? 'Phiếu thu chưa được lưu. Bạn có chắc chắn muốn thoát không?'
              : 'Bạn có chắc chắn muốn bỏ phiếu thu đang nhập không?'
          }
          onConfirm={() => {
            const d = dialog;
            setDialog(null);
            if (d === 'EXIT') closeCurrentTab();
            else if (receiptId) setMode('VIEW');
            else handleAdd();
          }}
          onCancel={() => setDialog(null)}
        />
      </div>
    );
  },
);
CashReceiptForm.displayName = 'CashReceiptForm';
