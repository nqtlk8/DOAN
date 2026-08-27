import React from 'react';
import { Plus, Trash2, X, Search } from 'lucide-react';

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
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
  remainingBalance: number;
  items: OrderItem[];
  onAddItem: () => void;
  onRemoveItem: (id: string) => void;
  onUpdateItem: (id: string, field: keyof OrderItem, value: any) => void;
  onProductSearch: (itemId: string) => void;
  renderPartnerCombobox?: () => React.ReactNode;
  renderProductCombobox?: (itemId: string, currentVal: string) => React.ReactNode;
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
  remainingBalance,
  items,
  onAddItem,
  onRemoveItem,
  onUpdateItem,
  onProductSearch,
  renderPartnerCombobox,
  renderProductCombobox,
}) => {
  const isView = mode === 'VIEW';

  const rowHeight = 'h-[34px]';
  const inputHeight = 'h-[28px]';
  const borderBot = 'border-b border-[#94a3b8]';

  // Nền trắng cho toàn bộ form.
  // - Input thường: nền trắng.
  // - Input readonly/disabled: nền xám nhạt #f1f5f9, chữ đen đậm.
  const baseInput = `${inputHeight} w-full px-[10px] text-[13px] text-slate-900 outline-none border border-transparent focus:bg-[#fefce8] focus:border-[#64748b] disabled:bg-[#f1f5f9] disabled:text-[#0f172a] disabled:font-semibold disabled:border-[#cbd5e1]`;

  // Khối tài chính luôn có viền
  const rightInput = `${inputHeight} w-full px-[10px] text-[13px] text-slate-900 outline-none border border-[#94a3b8] focus:bg-[#fefce8] disabled:bg-[#f1f5f9] disabled:text-[#0f172a] disabled:font-semibold text-right`;
  const labelClass =
    'text-[13px] text-[#0f172a] whitespace-nowrap px-[10px] text-right font-medium flex items-center justify-end';
  const leftLabelClass =
    'text-[13px] text-slate-700 whitespace-nowrap px-[10px] text-left font-medium flex items-center justify-start';

  return (
    <div className="font-sans flex-1 flex flex-col overflow-hidden bg-[#e2e8f0]">
      {error && (
        <div className="px-4 py-2 bg-red-100 text-red-700 text-sm border-b border-red-200 flex items-center gap-2">
          <X size={14} /> {error}
        </div>
      )}

      {/* HEADER BỐ CỤC DẠNG BẢNG 3 CỘT (Grid Cha Duy Nhất, Nền Trắng) */}
      <div className="w-full max-w-[1600px] min-w-[1024px] mx-auto overflow-x-auto bg-white border-b border-[#94a3b8] shrink-0">
        <div className="grid grid-cols-[minmax(350px,1.2fr)_minmax(450px,2fr)_minmax(280px,1fr)] w-full">
          {/* KHU VỰC 2: KHỐI TRÁI */}
          <div className="border-r border-[#94a3b8] flex flex-col">
            {/* Dòng 1: Ngày / Số */}
            <div
              className={`grid items-center ${rowHeight} ${borderBot}`}
              style={{ gridTemplateColumns: '70px 130px 40px 1fr' }}
            >
              <div className={labelClass}>Ngày</div>
              <div className="pr-1 flex items-center h-full">
                <input
                  type="date"
                  disabled={isView}
                  value={createdDate}
                  onChange={(e) => setCreatedDate(e.target.value)}
                  className={`${baseInput} border-[#cbd5e1]`}
                />
              </div>
              <div className={labelClass}>Số</div>
              <div className="pr-1 flex items-center h-full">
                <input type="text" disabled value={orderCode} className={`${baseInput} border-[#cbd5e1]`} />
              </div>
            </div>

            {/* Dòng 2: Lấy giá */}
            <div className={`grid items-center ${rowHeight} ${borderBot}`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <div className={leftLabelClass}>Lấy giá</div>
              <div className="pr-2 flex items-center h-full">
                <input type="text" disabled value="Bán hàng theo khách" className={`${baseInput}`} />
              </div>
            </div>

            {/* Dòng 3: Kho xuất */}
            <div className={`grid items-center ${rowHeight} ${borderBot}`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <div className={leftLabelClass}>Kho xuất</div>
              <div className="pr-2 flex items-center h-full">
                <input type="text" disabled value={branch} className={`${baseInput}`} />
              </div>
            </div>

            {/* Dòng 4: Nhân viên */}
            <div className={`grid items-center ${rowHeight} ${borderBot}`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <div className={leftLabelClass}>Nhân viên</div>
              <div className="pr-2 flex items-center h-full">
                <input type="text" disabled value={creator} className={`${baseInput}`} />
              </div>
            </div>

            {/* Dòng 5: Người lập */}
            <div className={`grid items-center ${rowHeight}`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <div className={leftLabelClass}>Người lập</div>
              <div className="pr-2 flex items-center h-full">
                <input type="text" disabled value={creator} className={`${baseInput}`} />
              </div>
            </div>
          </div>

          {/* KHU VỰC 3: KHỐI GIỮA (Khách hàng) */}
          <div className="border-r border-[#94a3b8] flex flex-col">
            {/* Dòng 1: Khách hàng */}
            <div
              className={`grid items-center ${rowHeight} ${borderBot}`}
              style={{ gridTemplateColumns: renderPartnerCombobox && !isView ? '110px 1fr' : '110px 1fr 32px' }}
            >
              <div className={labelClass}>{partnerCodeLabel.split('/')[0]}</div>
              {renderPartnerCombobox && !isView ? (
                <div className="flex items-center h-[28px] pr-2">
                  {renderPartnerCombobox()}
                </div>
              ) : (
                <>
                  <div className="flex items-center h-full pr-1">
                    <input
                      type="text"
                      disabled={isView}
                      value={partnerName}
                      readOnly
                      placeholder={partnerPlaceholder}
                      className={`${baseInput} border-[#cbd5e1] ${!isView ? 'cursor-pointer' : ''}`}
                      onClick={() => !isView && onPartnerSearch()}
                    />
                  </div>
                  <div className="flex items-center h-full pr-2">
                    <button
                      disabled={isView}
                      onClick={() => !isView && onPartnerSearch()}
                      className="w-full h-[28px] flex items-center justify-center bg-[#f1f5f9] border border-[#94a3b8] hover:bg-[#cbd5e1] disabled:opacity-50"
                    >
                      <Search size={14} />
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Dòng 2: Họ tên */}
            <div className={`grid items-center ${rowHeight} ${borderBot}`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <div className={labelClass}>Họ tên</div>
              <div className="pr-2 flex items-center h-full">
                <input
                  type="text"
                  disabled={isView}
                  value={contactPerson}
                  onChange={(e) => setContactPerson(e.target.value)}
                  className={`${baseInput}`}
                />
              </div>
            </div>

            {/* Dòng 3: Điện thoại */}
            <div className={`grid items-center ${rowHeight} ${borderBot}`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <div className={labelClass}>Điện thoại</div>
              <div className="pr-2 flex items-center h-full">
                <input
                  type="text"
                  disabled={isView}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className={`${baseInput}`}
                />
              </div>
            </div>

            {/* Dòng 4: Địa chỉ */}
            <div className={`grid items-center ${rowHeight} ${borderBot}`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <div className={labelClass}>Địa chỉ</div>
              <div className="pr-2 flex items-center h-full">
                <input
                  type="text"
                  disabled={isView}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className={`${baseInput}`}
                />
              </div>
            </div>

            {/* Dòng 5: Ghi chú */}
            <div className={`grid items-center ${rowHeight}`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <div className={labelClass}>Ghi chú</div>
              <div className="pr-2 flex items-center h-full">
                <input
                  type="text"
                  disabled={isView}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className={`${baseInput}`}
                />
              </div>
            </div>
          </div>

          {/* KHU VỰC 4: KHỐI TÀI CHÍNH (Phải) */}
          <div className="flex flex-col pr-2">
            {/* Dòng 1: Nợ trước */}
            <div className={`grid items-center ${rowHeight}`} style={{ gridTemplateColumns: '100px 1fr' }}>
              <div className={labelClass}>Nợ trước</div>
              <div className="flex items-center">
                <input
                  type="number"
                  disabled={isView}
                  value={oldDebt}
                  onChange={(e) => setOldDebt(Number(e.target.value) || 0)}
                  className={rightInput}
                />
              </div>
            </div>

            {/* Dòng 2: Tiền hàng */}
            <div className={`grid items-center ${rowHeight}`} style={{ gridTemplateColumns: '100px 1fr' }}>
              <div className={labelClass}>Tiền hàng</div>
              <div className="flex items-center">
                <input type="text" disabled value={totalAmount.toLocaleString()} className={rightInput} />
              </div>
            </div>

            {/* Dòng 3: Chiết khấu / VAT */}
            <div className={`grid items-center ${rowHeight}`} style={{ gridTemplateColumns: '100px 1fr 70px 1fr' }}>
              <div className={labelClass}>Chiết khấu</div>
              <div className="flex items-center">
                <input
                  type="number"
                  disabled={isView}
                  value={discount}
                  onChange={(e) => setDiscount(Number(e.target.value) || 0)}
                  className={rightInput}
                />
              </div>
              <div className={labelClass}>Thuế VAT</div>
              <div className="flex items-center">
                <input
                  type="number"
                  disabled={isView}
                  value={tax}
                  onChange={(e) => setTax(Number(e.target.value) || 0)}
                  className={rightInput}
                />
              </div>
            </div>

            {/* Dòng 4: Trả trước */}
            <div className={`grid items-center ${rowHeight}`} style={{ gridTemplateColumns: '100px 1fr' }}>
              <div className={labelClass}>Trả trước</div>
              <div className="flex items-center">
                <input
                  type="number"
                  disabled={isView}
                  value={advancePayment}
                  onChange={(e) => setAdvancePayment(Number(e.target.value) || 0)}
                  className={rightInput}
                />
              </div>
            </div>

            {/* Dòng 5: Còn lại */}
            <div className={`grid items-center ${rowHeight}`} style={{ gridTemplateColumns: '100px 1fr' }}>
              <div className={labelClass}>Còn lại</div>
              <div className="flex items-center">
                <input
                  type="text"
                  disabled
                  value={remainingBalance.toLocaleString()}
                  className={`${rightInput} font-bold text-[14px]`}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* KHU VỰC 5: BẢNG DANH SÁCH HÀNG HÓA */}
      <div className="font-sans flex-1 w-full max-w-[1600px] min-w-[1024px] mx-auto flex flex-col bg-white p-[10px] pb-0">
        <div className="font-sans flex-1 overflow-auto border border-b-0 border-[#64748b]">
          <table className="w-full border-collapse table-fixed">
            <thead className="sticky top-0 bg-[#f1f5f9] z-10 shadow-sm">
              <tr>
                <th
                  className="border-b border-r border-[#64748b] px-[10px] py-[6px] font-semibold text-[#0f172a] text-center"
                  style={{ width: '5%' }}
                >
                  STT
                </th>
                <th
                  className="border-b border-r border-[#64748b] px-[10px] py-[6px] font-semibold text-[#0f172a] text-center"
                  style={{ width: '30%' }}
                >
                  Hàng hóa
                </th>
                <th
                  className="border-b border-r border-[#64748b] px-[10px] py-[6px] font-semibold text-[#0f172a] text-center"
                  style={{ width: '12%' }}
                >
                  Số lượng
                </th>
                <th
                  className="border-b border-r border-[#64748b] px-[10px] py-[6px] font-semibold text-[#0f172a] text-center"
                  style={{ width: '13%' }}
                >
                  Đơn giá
                </th>
                <th
                  className="border-b border-r border-[#64748b] px-[10px] py-[6px] font-semibold text-[#0f172a] text-center"
                  style={{ width: '10%' }}
                >
                  Chiết khấu
                </th>
                <th
                  className="border-b border-r border-[#64748b] px-[10px] py-[6px] font-semibold text-[#0f172a] text-center"
                  style={{ width: '15%' }}
                >
                  Thành tiền
                </th>
                <th
                  className="border-b border-r border-[#64748b] px-[10px] py-[6px] font-semibold text-[#0f172a] text-center"
                  style={{ width: '15%' }}
                >
                  Ghi chú
                </th>
                {!isView && (
                  <th className="border-b border-[#64748b] px-[10px] py-[6px]" style={{ width: '40px' }}></th>
                )}
              </tr>
            </thead>
            <tbody>
              {items.map((item, index) => (
                <tr key={item.id} className="hover:bg-[#e0f2fe]">
                  <td className="border-b border-r border-[#64748b] px-[10px] py-[4px] text-center">{index + 1}</td>
                  <td className="border-b border-r border-[#64748b] px-0 py-0">
                    {renderProductCombobox && !isView ? (
                      renderProductCombobox(item.id, item.productName)
                    ) : (
                      <input
                        type="text"
                        disabled={isView}
                        placeholder="Bấm chọn..."
                        readOnly
                        onClick={() => {
                          if (!isView) onProductSearch(item.id);
                        }}
                        className="w-full h-[28px] px-[10px] bg-transparent outline-none cursor-pointer uppercase text-left text-slate-900"
                        value={item.productName}
                      />
                    )}
                  </td>
                  <td className="border-b border-r border-[#64748b] px-0 py-0">
                    <input
                      type="number"
                      disabled={isView}
                      min="1"
                      className="w-full h-[28px] px-[10px] bg-transparent outline-none text-right text-red-600 font-bold"
                      value={item.quantity}
                      onChange={(e) => onUpdateItem(item.id, 'quantity', parseInt(e.target.value) || 0)}
                    />
                  </td>
                  <td className="border-b border-r border-[#64748b] px-0 py-0">
                    <input
                      type="number"
                      disabled={isView}
                      min="0"
                      className="w-full h-[28px] px-[10px] bg-transparent outline-none text-right text-slate-900"
                      value={item.unitPrice}
                      onChange={(e) => onUpdateItem(item.id, 'unitPrice', parseFloat(e.target.value) || 0)}
                    />
                  </td>
                  <td className="border-b border-r border-[#64748b] px-0 py-0">
                    <input
                      type="number"
                      disabled={isView}
                      className="w-full h-[28px] px-[10px] bg-transparent outline-none text-right text-slate-900"
                      value={0}
                    />
                  </td>
                  <td className="border-b border-r border-[#64748b] px-[10px] py-[4px] text-right font-bold text-slate-900">
                    {(item.quantity * item.unitPrice).toLocaleString()}
                  </td>
                  <td className="border-b border-r border-[#64748b] px-0 py-0">
                    <input
                      type="text"
                      disabled={isView}
                      className="w-full h-[28px] px-[10px] bg-transparent outline-none text-left text-slate-900"
                    />
                  </td>
                  {!isView && (
                    <td className="border-b border-[#64748b] text-center">
                      <button
                        type="button"
                        onClick={() => onRemoveItem(item.id)}
                        className="text-red-600 hover:text-red-800 p-1"
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {!isView && items.length === 0 && (
                <tr>
                  <td colSpan={8} className="border-b border-[#64748b] px-[10px] py-8 text-center text-[#94a3b8]">
                    Chưa có dữ liệu. Hãy bấm "Dòng mới" bên dưới.
                  </td>
                </tr>
              )}
              {!isView && (
                <tr>
                  <td colSpan={8} className="border-b border-[#64748b] px-[10px] py-[4px]">
                    <button
                      type="button"
                      onClick={onAddItem}
                      className="text-blue-700 hover:underline flex items-center gap-1 font-medium"
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
        <table className="w-full border-collapse border border-[#64748b] border-t-[2px] table-fixed shrink-0 bg-[#f1f5f9]">
          <tfoot>
            <tr>
              <td
                colSpan={2}
                style={{ width: '35%' }}
                className="border-r border-[#64748b] px-[10px] py-[6px] text-center font-bold text-[#0f172a]"
              >
                TỔNG CỘNG
              </td>
              <td
                style={{ width: '12%' }}
                className="border-r border-[#64748b] px-[10px] py-[6px] text-right font-bold text-red-600"
              >
                {items.reduce((sum, item) => sum + item.quantity, 0).toLocaleString()}
              </td>
              <td colSpan={2} style={{ width: '23%' }} className="border-r border-[#64748b]"></td>
              <td
                style={{ width: '15%' }}
                className="border-r border-[#64748b] px-[10px] py-[6px] text-right font-bold text-slate-900 text-[14px]"
              >
                {totalAmount.toLocaleString()}
              </td>
              <td colSpan={isView ? 1 : 2} style={{ width: isView ? '15%' : 'calc(15% + 40px)' }} className=""></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
};
