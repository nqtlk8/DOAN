import React from 'react';
import { Plus, Trash2, X, Search } from 'lucide-react';

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  /** Đơn vị tính (lấy từ baseUnit của sản phẩm). */
  unitOfMeasure?: string;
}

export type FormMode = 'VIEW' | 'ADD' | 'EDIT';

export interface GenericDocumentFormProps {
  mode: FormMode;
  title: string;
  error: string | null;
  orderCode: string;
  creator: string;
  branch: string;
  createdDate: string;
  setCreatedDate: (date: string) => void;
  note: string;
  setNote: (note: string) => void;
  partnerTitle: string;
  partnerCodeLabel: string;
  partnerPlaceholder: string;
  partnerName: string;
  onPartnerSearch: () => void;
  address: string;
  setAddress: (address: string) => void;
  phone: string;
  setPhone: (phone: string) => void;
  contactPerson: string;
  setContactPerson: (contactPerson: string) => void;
  oldDebt: number;
  setOldDebt: (val: number) => void;
  totalAmount: number;
  discount: number;
  setDiscount: (val: number) => void;
  tax: number;
  setTax: (val: number) => void;
  advancePayment: number;
  setAdvancePayment: (val: number) => void;
  invoiceRemaining: number;
  remainingBalance: number;
  items: OrderItem[];
  onAddItem: () => void;
  onRemoveItem: (id: string) => void;
  onUpdateItem: (id: string, field: keyof OrderItem, value: any) => void;
  onProductSearch: (itemId: string) => void;
  renderPartnerCombobox?: (hasError?: boolean) => React.ReactNode;
  renderProductCombobox?: (itemId: string, currentVal: string, hasError?: boolean) => React.ReactNode;
  errors?: Record<string, string>;
}

export const GenericDocumentForm: React.FC<GenericDocumentFormProps> = ({
  mode,
  error,
  orderCode,
  creator,
  branch,
  createdDate,
  setCreatedDate,
  note,
  setNote,
  partnerTitle,
  partnerCodeLabel,
  partnerPlaceholder,
  partnerName,
  onPartnerSearch,
  address,
  setAddress,
  phone,
  setPhone,
  contactPerson,
  setContactPerson,
  oldDebt,
  setOldDebt,
  totalAmount,
  discount,
  setDiscount,
  tax,
  setTax,
  advancePayment,
  setAdvancePayment,
  invoiceRemaining,
  remainingBalance,
  items,
  onAddItem,
  onRemoveItem,
  onUpdateItem,
  onProductSearch,
  renderPartnerCombobox,
  renderProductCombobox,
  errors = {},
}) => {
  const isView = mode === 'VIEW';
  const [activeItemRowId, setActiveItemRowId] = React.useState<string | null>(null);

  return (
    <div className="font-sans flex-1 flex flex-col overflow-hidden bg-erp-bg-content">
      {error && (
        <div data-testid="sales-error-msg" className="px-4 py-2 bg-red-100 text-red-700 text-sm border-b border-red-200 flex items-center gap-2">
          <X size={14} /> {error}
        </div>
      )}

      {/* HEADER FORM KHÔNG VIỀN (Giao diện Kế toán Desktop) */}
      <div className="w-full mx-auto overflow-x-auto bg-erp-bg-content shrink-0 border-b border-erp-btn-border p-2">
        <div className="grid grid-cols-[minmax(300px,1.2fr)_minmax(400px,2fr)_minmax(280px,1fr)] gap-6 w-full">
          
          {/* KHỐI TRÁI */}
          <div className="grid items-center gap-1" style={{ gridTemplateColumns: '80px minmax(0, 1fr)' }}>
            <div className="text-right text-erp-label pr-2">Ngày</div>
            <div className="flex gap-2">
              <input
                type="date"
                disabled={isView}
                value={createdDate}
                onChange={(e) => setCreatedDate(e.target.value)}
                className="h-erp-input-height px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none flex-1 min-w-0"
              />
              <div className="text-right text-erp-label pr-2 flex items-center justify-end">Số</div>
              <input 
                type="text" 
                disabled 
                value={orderCode} 
                className="h-erp-input-height px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled text-erp-text-disabled outline-none rounded-none w-[100px] shrink-0" 
              />
            </div>

            <div className="text-right text-erp-label pr-2">Lấy giá</div>
            <input 
              type="text" 
              disabled 
              value="Bán hàng theo khách" 
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled text-erp-text-disabled outline-none rounded-none" 
            />

            <div className="text-right text-erp-label pr-2">Kho xuất</div>
            <input 
              type="text" 
              disabled 
              value={branch} 
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled text-erp-text-disabled outline-none rounded-none" 
            />

            <div className="text-right text-erp-label pr-2">Nhân viên</div>
            <input 
              type="text" 
              disabled 
              value={creator} 
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled text-erp-text-disabled outline-none rounded-none" 
            />

            <div className="text-right text-erp-label pr-2">Người lập</div>
            <input 
              type="text" 
              disabled 
              value={creator} 
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled text-erp-text-disabled outline-none rounded-none" 
            />
          </div>

          {/* KHỐI GIỮA (Khách hàng) */}
          <div className="grid items-center gap-1" style={{ gridTemplateColumns: '90px minmax(0, 1fr)' }}>
            <div className="text-right text-erp-label pr-2">{partnerCodeLabel.split('/')[0]}</div>
            {renderPartnerCombobox && !isView ? (
              <div className="h-erp-input-height w-full">
                {renderPartnerCombobox(!!errors['partner'])}
              </div>
            ) : (
              <div className="flex gap-1">
                <input
                  type="text"
                  disabled={isView}
                  value={partnerName}
                  readOnly
                  placeholder={partnerPlaceholder}
                  className={`h-erp-input-height flex-1 min-w-0 px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none ${!isView ? 'cursor-pointer' : ''} ${errors['partner'] ? 'border-erp-border-input-error bg-red-50' : ''}`}
                  onClick={() => !isView && onPartnerSearch()}
                />
                {!isView && (
                  <button
                    onClick={onPartnerSearch}
                    className="w-[28px] h-erp-input-height flex items-center justify-center bg-erp-btn-bg border border-erp-btn-border hover:bg-erp-btn-hover-bg shrink-0"
                  >
                    <Search size={14} />
                  </button>
                )}
              </div>
            )}

            <div className="text-right text-erp-label pr-2">Họ tên</div>
            <input
              type="text"
              disabled={isView}
              value={contactPerson}
              onChange={(e) => setContactPerson(e.target.value)}
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none"
            />

            <div className="text-right text-erp-label pr-2">Điện thoại</div>
            <input
              type="text"
              disabled={isView}
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none"
            />

            <div className="text-right text-erp-label pr-2">Địa chỉ</div>
            <input
              type="text"
              disabled={isView}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none"
            />

            <div className="text-right text-erp-label pr-2">Ghi chú</div>
            <input
              type="text"
              disabled={isView}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none"
            />
          </div>

          {/* KHỐI TÀI CHÍNH (Phải) */}
          <div className="grid items-center gap-1" style={{ gridTemplateColumns: '90px minmax(0, 1fr)' }}>
            <div className="text-right text-erp-label pr-2">Nợ trước</div>
            <input
              type="number"
              disabled={isView}
              value={oldDebt}
              onChange={(e) => setOldDebt(Number(e.target.value) || 0)}
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none text-right"
            />

            <div className="text-right text-erp-label pr-2">Tiền hàng</div>
            <input 
              type="text" 
              disabled 
              value={totalAmount.toLocaleString()} 
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled outline-none rounded-none text-right font-bold text-erp-text-accent-red" 
            />

            <div className="text-right text-erp-label pr-2">Chiết khấu</div>
            <div className="flex gap-2">
              <input
                type="number"
                disabled={isView}
                value={discount}
                onChange={(e) => setDiscount(Number(e.target.value) || 0)}
                className="h-erp-input-height flex-1 min-w-0 px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none text-right"
              />
              <div className="text-right text-erp-label pr-1 flex items-center justify-end">VAT</div>
              <input
                type="number"
                disabled={isView}
                value={tax}
                onChange={(e) => setTax(Number(e.target.value) || 0)}
                className="h-erp-input-height w-[70px] shrink-0 px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none text-right"
              />
            </div>

            <div className="text-right text-erp-label pr-2">Trả trước</div>
            <input
              type="number"
              disabled={isView}
              value={advancePayment}
              onChange={(e) => setAdvancePayment(Number(e.target.value) || 0)}
              className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-input border border-erp-border-input focus:border-erp-border-input-focus outline-none disabled:bg-erp-bg-disabled disabled:text-erp-text-disabled disabled:border-erp-border-disabled rounded-none text-right"
            />

              <div className="text-right text-erp-label pr-2">Còn của đơn</div>
              <input
                type="text"
                disabled
                value={invoiceRemaining.toLocaleString()}
                className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled outline-none rounded-none text-right font-bold text-erp-text-accent-red"
              />

              <div className="text-right text-erp-label pr-2">Nợ tổng mới</div>
              <input
                type="text"
                disabled
                value={remainingBalance.toLocaleString()}
                className="h-erp-input-height w-full px-1 text-erp-base bg-erp-bg-disabled border border-erp-border-disabled outline-none rounded-none text-right font-bold text-erp-text-accent-red"
              />
          </div>
        </div>
      </div>

      {/* KHU VỰC 5: BẢNG DANH SÁCH HÀNG HÓA */}
      <div className="font-sans flex-1 w-full max-w-[1600px] min-w-[1024px] mx-auto flex flex-col bg-white">
        <div className="flex-1 overflow-auto bg-white border-b border-erp-border-table relative">
          <table className="w-full border-collapse table-fixed" style={{ borderCollapse: 'collapse' }}>
            <thead className="sticky top-0 bg-erp-bg-table-header z-10 shadow-[0_1px_0_var(--color-erp-border-table)]">
              <tr className="h-erp-row-height">
                <th className="border border-erp-border-table px-1 font-bold text-erp-label text-center text-erp-text-primary" style={{ width: '40px' }}>STT</th>
                <th className="border border-erp-border-table px-1 font-bold text-erp-label text-center text-erp-text-primary" style={{ width: '30%' }}>Hàng hóa</th>
                <th className="border border-erp-border-table px-1 font-bold text-erp-label text-center text-erp-text-primary" style={{ width: '10%' }}>Số lượng</th>
                <th className="border border-erp-border-table px-1 font-bold text-erp-label text-center text-erp-text-primary" style={{ width: '13%' }}>Đơn giá</th>
                <th className="border border-erp-border-table px-1 font-bold text-erp-label text-center text-erp-text-primary" style={{ width: '10%' }}>Chiết khấu</th>
                <th className="border border-erp-border-table px-1 font-bold text-erp-label text-center text-erp-text-primary" style={{ width: '15%' }}>Thành tiền</th>
                <th className="border border-erp-border-table px-1 font-bold text-erp-label text-center text-erp-text-primary" style={{ width: '15%' }}>Ghi chú</th>
                {!isView && <th className="border border-erp-border-table px-1" style={{ width: '30px' }}></th>}
              </tr>
            </thead>
            <tbody>
              {items.map((item, index) => {
                const isSelected = activeItemRowId === item.id;
                return (
                  <tr
                    key={item.id}
                    data-testid="sales-line-row"
                    className={`h-erp-row-height border-b border-erp-border-table cursor-default ${
                      isSelected ? 'bg-erp-row-selected-bg text-erp-row-selected-text' : 'hover:bg-erp-row-hover-bg text-erp-text-primary'
                    }`}
                    onClick={() => setActiveItemRowId(item.id)}
                  >
                    <td className="border-r border-l border-erp-border-table px-1 text-center text-erp-base">{index + 1}</td>
                    <td className="border-r border-erp-border-table px-0 relative">
                      {renderProductCombobox && !isView ? (
                        <div className="h-full w-full" onClick={(e) => e.stopPropagation()}>
                          {renderProductCombobox(item.id, item.productName, !!errors[`item_${index}_product`])}
                        </div>
                      ) : (
                        <input
                          type="text"
                          disabled={isView}
                          placeholder="Bấm chọn..."
                          readOnly
                          onClick={(e) => {
                            e.stopPropagation();
                            setActiveItemRowId(item.id);
                            if (!isView) onProductSearch(item.id);
                          }}
                          className={`w-full h-full px-1 bg-transparent outline-none cursor-pointer uppercase text-left text-erp-base ${isSelected ? 'text-erp-row-selected-text placeholder:text-erp-row-selected-text' : 'text-erp-text-primary'}`}
                          value={item.productName}
                        />
                      )}
                    </td>
                    <td className="border-r border-erp-border-table px-0">
                      <input
                        data-testid="sales-line-quantity"
                        type="number"
                        disabled={isView}
                        min="1"
                        className={`w-full h-full px-1 bg-transparent outline-none text-right font-bold text-erp-base ${
                          errors[`item_${index}_quantity`] ? 'ring-1 ring-inset ring-red-500 bg-red-50 text-red-600' : isSelected ? 'text-erp-row-selected-text' : 'text-erp-text-accent-red'
                        }`}
                        value={item.quantity}
                        onChange={(e) => onUpdateItem(item.id, 'quantity', parseInt(e.target.value) || 0)}
                        onClick={(e) => { e.stopPropagation(); setActiveItemRowId(item.id); }}
                      />
                    </td>
                    <td className="border-r border-erp-border-table px-0">
                      <input
                        data-testid="sales-line-price"
                        type="number"
                        disabled={isView}
                        min="0"
                        className={`w-full h-full px-1 bg-transparent outline-none text-right text-erp-base ${isSelected ? 'text-erp-row-selected-text' : 'text-erp-text-primary'}`}
                        value={item.unitPrice}
                        onChange={(e) => onUpdateItem(item.id, 'unitPrice', parseFloat(e.target.value) || 0)}
                        onClick={(e) => { e.stopPropagation(); setActiveItemRowId(item.id); }}
                      />
                    </td>
                    <td className="border-r border-erp-border-table px-0">
                      <input
                        type="number"
                        disabled={isView}
                        className={`w-full h-full px-1 bg-transparent outline-none text-right text-erp-base ${isSelected ? 'text-erp-row-selected-text' : 'text-erp-text-primary'}`}
                        value={0}
                        readOnly
                        onClick={(e) => { e.stopPropagation(); setActiveItemRowId(item.id); }}
                      />
                    </td>
                    <td data-testid="sales-line-total" className={`border-r border-erp-border-table px-1 text-right font-bold text-erp-base ${isSelected ? 'text-erp-row-selected-text' : 'text-erp-text-accent-red'}`}>
                      {(item.quantity * item.unitPrice).toLocaleString()}
                    </td>
                    <td className="border-r border-erp-border-table px-0">
                      <input
                        type="text"
                        disabled={isView}
                        className={`w-full h-full px-1 bg-transparent outline-none text-left text-erp-base ${isSelected ? 'text-erp-row-selected-text' : 'text-erp-text-primary'}`}
                        onClick={(e) => { e.stopPropagation(); setActiveItemRowId(item.id); }}
                      />
                    </td>
                    {!isView && (
                      <td className="border-r border-erp-border-table text-center bg-white">
                        <button
                          type="button"
                          onClick={(e) => { e.stopPropagation(); onRemoveItem(item.id); }}
                          className="text-erp-text-accent-red hover:text-red-800 flex items-center justify-center w-full h-full"
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
              {!isView && items.length === 0 && (
                <tr>
                  <td colSpan={8} className="border-b border-erp-border-table px-1 py-4 text-center text-slate-500 text-erp-base">
                    Chưa có dữ liệu. Hãy bấm "Dòng mới" bên dưới.
                  </td>
                </tr>
              )}
              {!isView && (
                <tr className="h-erp-row-height bg-white">
                  <td colSpan={8} className="border-b border-erp-border-table px-2">
                    <button
                      type="button"
                      data-testid="sales-add-line"
                      onClick={onAddItem}
                      className="text-blue-700 hover:underline flex items-center gap-1 font-medium text-erp-base"
                    >
                      <Plus size={14} /> Dòng mới
                    </button>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* TỔNG CỘNG (Luôn ở dưới cùng) */}
        <div className="shrink-0 bg-erp-bg-table-header border-t-2 border-erp-border-table">
          <table className="w-full border-collapse table-fixed">
            <tbody>
              <tr className="h-[28px]">
                <td colSpan={2} style={{ width: 'calc(40px + 30%)' }} className="border-r border-erp-border-table px-2 text-center font-bold text-erp-text-primary text-erp-base">
                  TỔNG CỘNG
                </td>
                <td style={{ width: '10%' }} className="border-r border-erp-border-table px-1 text-right font-bold text-erp-text-accent-red text-erp-base">
                  {items.reduce((sum, item) => sum + item.quantity, 0).toLocaleString()}
                </td>
                <td colSpan={2} style={{ width: '23%' }} className="border-r border-erp-border-table"></td>
                <td style={{ width: '15%' }} className="border-r border-erp-border-table px-1 text-right font-bold text-erp-text-accent-red text-[13px]">
                  {totalAmount.toLocaleString()}
                </td>
                <td colSpan={isView ? 1 : 2} style={{ width: isView ? '15%' : 'calc(15% + 30px)' }}></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
