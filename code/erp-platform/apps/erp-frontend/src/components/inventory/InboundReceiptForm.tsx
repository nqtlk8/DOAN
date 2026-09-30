import { forwardRef, useEffect, useImperativeHandle, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { errorMessage } from '../../shared/errors/errorMessage';
import { ApiService } from '../../api/ApiService';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { GenericDocumentForm } from '../common/document/GenericDocumentForm';
import {
  focusLineCell,
  productToLinePatch,
  type ProductLike,
  sumLines,
  sumQuantity,
  useDocumentLines,
  validateLines,
  newLineId,
  omitKey,
} from '../common/document/documentLines';
import { SearchableCombobox } from '../common/SearchableCombobox';
import { SearchModal } from '../common/SearchModal';
import { QuickCreateSupplier } from '../common/quick-create/QuickCreateSupplier';
import { QuickCreateProduct } from '../common/quick-create/QuickCreateProduct';
import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';
import { formatCurrency, normalizeSearch, toInputDate } from '../../shared/utils/format';
import type { DocStatus, DocumentLine, FormMode, InboundReceiptDto, ProductDto } from '../../types/documents';

export type { FormMode };

interface SupplierLike {
  id?: string;
  code?: string;
  name?: string;
  phone?: string;
  address?: string;
  active?: boolean;
}

export interface InboundReceiptFormProps {
  mode?: FormMode;
  /** InboundReceiptResponseDto từ API (không có tên NCC / tên SP → tự tra cứu). */
  initialData?: InboundReceiptDto | null;
  onStateChange?: (mode: FormMode, isLoading: boolean, status?: DocStatus) => void;
}

export interface InboundReceiptFormRef {
  handleAdd: () => void;
  handleSubmit: () => Promise<void>;
  handleCancel: () => void;
  handleConfirm: () => Promise<void>;
  handleExit: () => void;
}

const supplierMatches = (s: SupplierLike, q: string) => {
  const term = normalizeSearch(q);
  if (!term) return true;
  return (
    normalizeSearch(s.name).includes(term) ||
    normalizeSearch(s.code).includes(term) ||
    (s.phone ?? '').includes(q.trim())
  );
};


/** Dòng hàng từ API (tùy loại phiếu có unitCost hoặc unitPrice, có/không có tên SP). */
type LooseLine = {
  productId?: number | string;
  productCode?: string;
  productName?: string;
  quantity?: number;
  unitCost?: number;
  unitPrice?: number;
  unitOfMeasure?: string;
};

const mapInitialLines = (data?: InboundReceiptDto | null): DocumentLine[] =>
  Array.isArray(data?.lines)
    ? (data.lines as LooseLine[]).map((l) => ({
        id: newLineId(),
        productId: l.productId != null ? String(l.productId) : '',
        productCode: l.productCode || '',
        productName: l.productName || '',
        unitOfMeasure: l.unitOfMeasure || 'CAI',
        quantity: Number(l.quantity) || 0,
        unitPrice: Number(l.unitCost ?? l.unitPrice ?? 0),
      }))
    : [];

type DialogState = null | 'CANCEL' | 'EXIT';

export const InboundReceiptForm = forwardRef<InboundReceiptFormRef, InboundReceiptFormProps>(
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

    const [receiptId, setReceiptId] = useState<string>(initialData?.id || '');
    const [receiptCode, setReceiptCode] = useState<string>(initialData?.receiptCode || '');
    const [status, setStatus] = useState<DocStatus | undefined>(initialData?.status as DocStatus | undefined);
    const createdDate = initialData?.createdAt || toInputDate(new Date());

    const [supplierId, setSupplierId] = useState<string>(initialData?.supplierId || '');
    const [supplier, setSupplier] = useState<SupplierLike | null>(null);
    const [note, setNote] = useState<string>(initialData?.note || '');
    const { lines, setLines, addLine, removeLine, updateLine, patchLine } = useDocumentLines(() =>
      mapInitialLines(initialData),
    );

    // Dialog thêm nhanh / tìm kiếm nâng cao
    const [quickSupplier, setQuickSupplier] = useState<string | null>(null);
    const [quickProduct, setQuickProduct] = useState<{ lineId: string; name: string } | null>(null);
    const [showSupplierSearch, setShowSupplierSearch] = useState(false);

    // Danh mục để tra tên (API phiếu nhập chỉ trả supplierId / productId).
    const { data: suppliers = [] } = useQuery<SupplierLike[]>({
      queryKey: ['suppliers'],
      queryFn: () => ApiService.Catalog.getSuppliers() as Promise<SupplierLike[]>,
    });
    const { data: products = [] } = useQuery<ProductDto[]>({
      queryKey: ['products'],
      queryFn: () => ApiService.Catalog.getProducts(),
      enabled: lines.some((l) => l.productId && !l.productName),
    });

    const currentSupplier = useMemo<SupplierLike | null>(
      () => supplier ?? suppliers.find((s) => String(s.id) === String(supplierId)) ?? null,
      [supplier, suppliers, supplierId],
    );

    const displayLines = useMemo(
      () =>
        lines.map((l) => {
          if (l.productName || !l.productId) return l;
          const p = products.find((x) => String(x.id) === l.productId);
          return p ? { ...l, productName: p.name ?? '', productCode: p.code ?? '' } : l;
        }),
      [lines, products],
    );

    useEffect(() => {
      onStateChange?.(mode, isLoading, status);
    }, [mode, isLoading, status, onStateChange]);

    const closeCurrentTab = () => {
      if (activeTabId) closeTab(activeTabId);
    };

    const resetForm = () => {
      setMode('ADD');
      setReceiptId('');
      setReceiptCode('');
      setStatus(undefined);
      setSupplierId('');
      setSupplier(null);
      setNote('');
      setLines([]);
      setFieldErrors({});
    };

    const selectSupplier = (s: SupplierLike) => {
      setSupplierId(String(s.id ?? ''));
      setSupplier(s);
      setFieldErrors((prev) => omitKey(prev, 'partner'));
    };

    const selectProduct = (lineId: string, p: ProductLike) => {
      patchLine(lineId, productToLinePatch(p));
      focusLineCell(lineId, 'quantity');
    };

    const validate = () => {
      const errors: Record<string, string> = {};
      if (!supplierId) errors.partner = 'Vui lòng chọn nhà cung cấp';
      if (lines.length === 0) {
        setFieldErrors(errors);
        toast.error('Phiếu nhập phải có ít nhất 1 sản phẩm.');
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
      if (mode !== 'ADD' || isLoading) return; // Backend chưa có API sửa phiếu nhập
      if (!validate()) return;
      setIsLoading(true);
      try {
        const res = await ApiService.InboundReceipt.create({
          supplierId,
          note,
          lines: lines.map((l) => ({
            productId: Number(l.productId),
            quantity: l.quantity,
            unitCost: l.unitPrice,
            unitOfMeasure: l.unitOfMeasure || 'CAI',
          })),
        });
        const newId = res?.id || '';
        // Đã tạo thành công → chuyển sang VIEW ngay, tránh người dùng bấm Lưu lần nữa tạo phiếu trùng.
        setReceiptId(newId);
        setStatus('DRAFT');
        setMode('VIEW');
        toast.success('Lưu phiếu nhập thành công');
        queryClient.invalidateQueries({ queryKey: ['inbound-receipts'] });
        if (newId) {
          try {
            const fresh = await ApiService.InboundReceipt.getById(newId);
            if (fresh?.receiptCode) setReceiptCode(fresh.receiptCode);
            if (fresh?.status) setStatus(fresh.status as DocStatus);
          } catch {
            /* Không lấy được số phiếu thì vẫn giữ phiếu đã lưu, không báo lỗi. */
          }
        }
      } catch (error) {
        toast.error(errorMessage(error, 'Lưu phiếu nhập thất bại'));
      } finally {
        setIsLoading(false);
      }
    };

    const handleConfirm = async () => {
      if (!receiptId || status !== 'DRAFT' || isLoading) return;
      setIsLoading(true);
      try {
        await ApiService.InboundReceipt.confirm(receiptId);
        setStatus('CONFIRMED');
        toast.success('Xác nhận phiếu nhập thành công');
        queryClient.invalidateQueries({ queryKey: ['inbound-receipts'] });
        queryClient.invalidateQueries({ queryKey: ['stocks'] });
      } catch (error) {
        toast.error(errorMessage(error, 'Xác nhận phiếu nhập thất bại'));
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
        if (!isView && (supplierId || lines.length > 0 || note)) setDialog('EXIT');
        else closeCurrentTab();
      },
    }));

    return (
      <div className="h-full bg-surface relative">
        <GenericDocumentForm
          mode={mode}
          docTitle="Phiếu nhập hàng"
          docCode={receiptCode || (receiptId ? '' : 'AUTO-GENERATE')}
          status={receiptId ? status : undefined}
          errors={fieldErrors}
          info={[
            { key: 'branch', label: 'Kho nhập', value: initialData?.branchId ? `Chi nhánh #${initialData.branchId}` : 'Chi nhánh đang đăng nhập' },
            { key: 'creator', label: 'Người lập', value: user?.username || '' },
          ]}
          createdDate={createdDate}
          partner={{
            label: 'Nhà cung cấp',
            required: true,
            displayName: currentSupplier?.name || '',
            onAdvancedSearch: () => setShowSupplierSearch(true),
            renderCombobox: (hasError) => (
              <SearchableCombobox<SupplierLike>
                data-testid="inbound-supplier-combo"
                value={currentSupplier?.name || ''}
                placeholder="Tìm theo mã, tên, SĐT nhà cung cấp…"
                error={hasError}
                fetchData={async (q) => {
                  const data = (await queryClient.fetchQuery({
                    queryKey: ['suppliers'],
                    queryFn: () => ApiService.Catalog.getSuppliers() as Promise<SupplierLike[]>,
                  })) as SupplierLike[];
                  return data.filter((s) => s.active !== false && supplierMatches(s, q));
                }}
                columns={[
                  { header: 'Mã NCC', field: 'code', width: '100px' },
                  { header: 'Tên nhà cung cấp', field: 'name' },
                  { header: 'Điện thoại', field: 'phone', width: '120px' },
                ]}
                onSelect={selectSupplier}
                onCreateNew={isAdmin ? (q) => setQuickSupplier(q) : undefined}
                createLabel="Thêm nhà cung cấp mới"
              />
            ),
            fields: [
              { key: 'phone', label: 'Điện thoại', value: currentSupplier?.phone || '' },
              { key: 'address', label: 'Địa chỉ', value: currentSupplier?.address || '' },
              { key: 'note', label: 'Ghi chú', value: note, onChange: setNote, testId: 'inbound-note' },
            ],
          }}
          summary={[
            { key: 'totalQty', label: 'Tổng số lượng', value: sumQuantity(lines), isQuantity: true, testId: 'sum-total-qty' },
            { key: 'totalAmount', label: 'Tổng tiền hàng', value: sumLines(lines), strong: true, tone: 'primary', testId: 'sum-total' },
          ]}
          lines={{
            items: displayLines,
            testIdPrefix: 'inbound',
            priceLabel: 'Giá nhập',
            onAdd: addLine,
            onRemove: removeLine,
            onUpdate: updateLine,
            renderProductCombobox: (line, _index, hasError) => (
              <SearchableCombobox
                data-testid={`inbound-product-combo-${line.id}`}
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

        <SearchModal<SupplierLike>
          isOpen={showSupplierSearch}
          onClose={() => setShowSupplierSearch(false)}
          title="Tìm nhà cung cấp"
          placeholder="Nhập mã, tên hoặc SĐT…"
          fetchData={async (q) => suppliers.filter((s) => s.active !== false && supplierMatches(s, q))}
          renderItem={(s) => (
            <div>
              <div className="font-medium text-ink">
                {s.name} {s.code ? <span className="text-ink-subtle">({s.code})</span> : null}
              </div>
              <div className="text-[12px] text-ink-muted">{[s.phone, s.address].filter(Boolean).join(' · ')}</div>
            </div>
          )}
          onSelect={selectSupplier}
        />

        <QuickCreateSupplier
          isOpen={quickSupplier !== null}
          initialName={quickSupplier ?? ''}
          onClose={() => setQuickSupplier(null)}
          onCreated={(s) => s && selectSupplier(s)}
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
          title={dialog === 'EXIT' ? 'Thoát phiếu nhập' : 'Hủy phiếu nhập'}
          message={
            dialog === 'EXIT'
              ? 'Dữ liệu chưa được lưu. Bạn có chắc chắn muốn thoát?'
              : 'Bạn có chắc chắn muốn hủy phiếu nhập này? Các thay đổi sẽ không được lưu.'
          }
          confirmLabel="Đồng ý"
          cancelLabel="Quay lại"
          onConfirm={() => {
            const d = dialog;
            setDialog(null);
            if (d === 'EXIT' || !receiptId) closeCurrentTab();
            else setMode('VIEW');
          }}
          onCancel={() => setDialog(null)}
        />
      </div>
    );
  },
);
InboundReceiptForm.displayName = 'InboundReceiptForm';
