import { forwardRef, useEffect, useImperativeHandle, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { errorMessage } from '../../shared/errors/errorMessage';
import { ApiService } from '../../api/ApiService';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { GenericDocumentForm } from '../common/document/GenericDocumentForm';
import {
  focusLineCell,
  newLineId,
  productToLinePatch,
  type ProductLike,
  sumLines,
  sumQuantity,
  useDocumentLines,
  validateLines,
  omitKey,
} from '../common/document/documentLines';
import { SearchableCombobox } from '../common/SearchableCombobox';
import { SearchModal } from '../common/SearchModal';
import { QuickCreateCustomer } from '../common/quick-create/QuickCreateCustomer';
import { QuickCreateProduct } from '../common/quick-create/QuickCreateProduct';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { formatCurrency, toInputDate } from '../../shared/utils/format';
import type { DocStatus, DocumentLine, FormMode, GoodsReturnResponse } from '../../types/documents';

export type { FormMode };

interface CustomerLike {
  id?: string;
  customerCode?: string;
  name?: string;
  phone?: string;
  address?: string;
}

export interface GoodsReturnFormProps {
  mode?: FormMode;
  initialData?: GoodsReturnResponse | null;
  onStateChange?: (mode: FormMode, isLoading: boolean, status?: DocStatus) => void;
}

export interface GoodsReturnFormRef {
  handleAdd: () => void;
  handleSubmit: () => Promise<void>;
  handleCancel: () => void;
  handleConfirm: () => Promise<void>;
  handleExit: () => void;
}

const mapInitialLines = (data?: GoodsReturnResponse | null): DocumentLine[] =>
  (data?.lines ?? []).map((l) => ({
    id: newLineId(),
    productId: l.productId != null ? String(l.productId) : '',
    productCode: l.productCode || '',
    productName: l.productName || '',
    unitOfMeasure: l.unitOfMeasure || 'CAI',
    quantity: Number(l.quantity) || 0,
    unitPrice: Number(l.unitPrice) || 0,
  }));

type DialogState = null | 'CANCEL' | 'EXIT';

export const GoodsReturnForm = forwardRef<GoodsReturnFormRef, GoodsReturnFormProps>(
  ({ mode: initialMode = 'ADD', initialData, onStateChange }, ref) => {
    const { user } = useAuth();
    const isAdmin = user?.role === 'ADMIN';
    const { closeTab, activeTabId } = useTabs();
    const queryClient = useQueryClient();

    const [mode, setMode] = useState<FormMode>(initialMode);
    const isView = mode === 'VIEW';
    const [isLoading, setIsLoading] = useState(false);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [dialog, setDialog] = useState<DialogState>(null);

    const [returnId, setReturnId] = useState<string>(initialData?.id || '');
    const [returnCode, setReturnCode] = useState<string>(initialData?.returnCode || '');
    const [status, setStatus] = useState<DocStatus | undefined>(initialData?.status);
    const createdDate = initialData?.createdAt || toInputDate(new Date());

    const [customer, setCustomer] = useState<CustomerLike | null>(
      initialData?.customerId ? { id: initialData.customerId, name: initialData.customerName } : null,
    );
    const [reason, setReason] = useState<string>(initialData?.reason || '');
    const [note, setNote] = useState<string>('');
    const { lines, setLines, addLine, removeLine, updateLine, patchLine } = useDocumentLines(() =>
      mapInitialLines(initialData),
    );

    const [quickCustomer, setQuickCustomer] = useState<string | null>(null);
    const [quickProduct, setQuickProduct] = useState<{ lineId: string; name: string } | null>(null);
    const [showCustomerSearch, setShowCustomerSearch] = useState(false);

    useEffect(() => {
      onStateChange?.(mode, isLoading, status);
    }, [mode, isLoading, status, onStateChange]);

    const closeCurrentTab = () => {
      if (activeTabId) closeTab(activeTabId);
    };

    const resetForm = () => {
      setMode('ADD');
      setReturnId('');
      setReturnCode('');
      setStatus(undefined);
      setCustomer(null);
      setReason('');
      setNote('');
      setLines([]);
      setFieldErrors({});
    };

    const selectCustomer = (c: CustomerLike) => {
      setCustomer(c);
      setFieldErrors((prev) => omitKey(prev, 'partner'));
    };

    const selectProduct = (lineId: string, p: ProductLike) => {
      patchLine(lineId, productToLinePatch(p));
      focusLineCell(lineId, 'quantity');
    };

    const validate = () => {
      const errors: Record<string, string> = {};
      if (!customer?.id) errors.partner = 'Vui lòng chọn khách hàng';
      if (lines.length === 0) {
        setFieldErrors(errors);
        toast.error('Phiếu trả phải có ít nhất 1 sản phẩm.');
        return false;
      }
      Object.assign(errors, validateLines(lines));
      setFieldErrors(errors);
      if (Object.keys(errors).length > 0) {
        toast.error('Vui lòng kiểm tra lại thông tin nhập.');
        return false;
      }
      return true;
    };

    const handleSubmit = async () => {
      if (mode !== 'ADD' || isLoading) return; // Backend chưa có API sửa phiếu trả
      if (!validate()) return;
      setIsLoading(true);
      try {
        const res = await ApiService.GoodsReturn.create({
          customerId: String(customer!.id),
          reason: reason || undefined,
          note: note || undefined,
          lines: lines.map((l) => ({
            productId: Number(l.productId),
            quantity: l.quantity,
            unitPrice: l.unitPrice,
            unitOfMeasure: l.unitOfMeasure || 'CAI',
          })),
        });
        const newId = typeof res === 'string' ? res : '';
        setReturnId(newId);
        setStatus('DRAFT');
        setMode('VIEW');
        toast.success('Lưu phiếu trả thành công');
        queryClient.invalidateQueries({ queryKey: ['goods-returns'] });
        if (newId) {
          try {
            const fresh = await ApiService.GoodsReturn.getById(newId);
            if (fresh?.returnCode) setReturnCode(fresh.returnCode);
            if (fresh?.status) setStatus(fresh.status);
          } catch {
            /* Không lấy được số phiếu thì vẫn giữ phiếu đã lưu. */
          }
        }
      } catch (error) {
        toast.error(errorMessage(error, 'Lưu phiếu trả thất bại'));
      } finally {
        setIsLoading(false);
      }
    };

    const handleConfirm = async () => {
      if (!returnId || status !== 'DRAFT' || isLoading) return;
      setIsLoading(true);
      try {
        await ApiService.GoodsReturn.confirm(returnId);
        setStatus('CONFIRMED');
        toast.success('Xác nhận trả hàng thành công');
        queryClient.invalidateQueries({ queryKey: ['goods-returns'] });
        queryClient.invalidateQueries({ queryKey: ['stocks'] });
      } catch (error) {
        toast.error(errorMessage(error, 'Xác nhận trả hàng thất bại'));
      } finally {
        setIsLoading(false);
      }
    };

    useImperativeHandle(ref, () => ({
      handleAdd: resetForm,
      handleSubmit,
      handleCancel: () => {
        if (!isView) setDialog('CANCEL');
      },
      handleConfirm,
      handleExit: () => {
        if (!isView && (customer || lines.length > 0 || reason || note)) setDialog('EXIT');
        else closeCurrentTab();
      },
    }));

    return (
      <div className="h-full bg-surface relative">
        <GenericDocumentForm
          mode={mode}
          docTitle="Phiếu nhập lại hàng bán"
          docCode={returnCode || (returnId ? '' : 'AUTO-GENERATE')}
          status={returnId ? status : undefined}
          errors={fieldErrors}
          info={[
            { key: 'branch', label: 'Kho nhập', value: initialData?.branchId ? `Chi nhánh #${initialData.branchId}` : 'Chi nhánh đang đăng nhập' },
            { key: 'creator', label: 'Người lập', value: user?.username || '' },
          ]}
          createdDate={createdDate}
          partner={{
            label: 'Khách hàng',
            required: true,
            displayName: customer?.name || '',
            onAdvancedSearch: () => setShowCustomerSearch(true),
            renderCombobox: (hasError) => (
              <SearchableCombobox<CustomerLike>
                data-testid="return-customer-combo"
                value={customer?.name || ''}
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
              { key: 'phone', label: 'Điện thoại', value: customer?.phone || '' },
              { key: 'address', label: 'Địa chỉ', value: customer?.address || '' },
              { key: 'reason', label: 'Lý do trả', value: reason, onChange: setReason, testId: 'return-reason' },
              { key: 'note', label: 'Ghi chú', value: note, onChange: setNote, testId: 'return-note' },
            ],
          }}
          summary={[
            { key: 'totalQty', label: 'Tổng số lượng', value: sumQuantity(lines), isQuantity: true, testId: 'sum-total-qty' },
            { key: 'totalAmount', label: 'Tổng tiền trả', value: sumLines(lines), strong: true, tone: 'primary', testId: 'sum-total' },
          ]}
          lines={{
            items: lines,
            testIdPrefix: 'return',
            priceLabel: 'Đơn giá',
            onAdd: addLine,
            onRemove: removeLine,
            onUpdate: updateLine,
            renderProductCombobox: (line, _index, hasError) => (
              <SearchableCombobox
                data-testid={`return-product-combo-${line.id}`}
                variant="cell"
                value={line.productName}
                placeholder="Tìm mã hoặc tên hàng…"
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
          title={dialog === 'EXIT' ? 'Thoát phiếu trả hàng' : 'Hủy phiếu trả hàng'}
          message={
            dialog === 'EXIT'
              ? 'Dữ liệu chưa được lưu. Bạn có chắc chắn muốn thoát?'
              : 'Bạn có chắc chắn muốn hủy phiếu trả này? Các thay đổi sẽ không được lưu.'
          }
          confirmLabel="Đồng ý"
          cancelLabel="Quay lại"
          onConfirm={() => {
            const d = dialog;
            setDialog(null);
            if (d === 'EXIT' || !returnId) closeCurrentTab();
            else setMode('VIEW');
          }}
          onCancel={() => setDialog(null)}
        />
      </div>
    );
  },
);
GoodsReturnForm.displayName = 'GoodsReturnForm';
