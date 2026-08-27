const fs = require('fs');

const fileContent = `import React from 'react';
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

  // Internal Info
  creator: string;
  branch: string;
  createdDate: string;
  setCreatedDate: (date: string) => void;
  note: string;
  setNote: (note: string) => void;

  // Partner Info
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

  // Financial Info
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

  // Table
  items: OrderItem[];
  onAddItem: () => void;
  onRemoveItem: (id: string) => void;
  onUpdateItem: (id: string, field: keyof OrderItem, value: any) => void;
  onProductSearch: (itemId: string) => void;
}

export const GenericDocumentForm: React.FC<GenericDocumentFormProps> = ({
  mode,
  title,
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
}) => {
  const isView = mode === 'VIEW';

  const labelClass = "w-20 text-right text-[12px] text-slate-800 pr-2 whitespace-nowrap shrink-0";
  const inputClass = "flex-1 h-[24px] px-1.5 text-[12px] border border-[#a0a0a0] rounded-sm bg-white focus:outline-none focus:bg-[#FFFDE7] focus:border-blue-500 disabled:bg-[#f0f0f0] disabled:text-slate-600 disabled:border-[#d0d0d0]";
  const numInputClass = inputClass + " text-right font-semibold";

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-white text-[12px]">
      {/* Error Message */}
      {error && (
        <div className="px-4 py-2 bg-red-100 text-red-700 text-sm border-b border-red-200 flex items-center gap-2">
          <X size={14} /> {error}
        </div>
      )}

      {/* Classic Dense ERP Top Section */}
      <div className="bg-[#E3E9F0] px-2 py-2 border-b border-[#a0a0a0] shrink-0">
        <div className="grid grid-cols-[300px_minmax(400px,_1fr)_300px] gap-x-6 gap-y-0 items-start">
          
          {/* Column 1: Internal Info */}
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center">
              <label className={labelClass}>Ngày</label>
              <input
                type="date"
                disabled={isView}
                value={createdDate}
                onChange={(e) => setCreatedDate(e.target.value)}
                className={inputClass + " w-24 shrink-0"}
              />
              <label className="w-8 text-right text-[12px] text-slate-800 pr-2 shrink-0">Số</label>
              <input
                type="text"
                disabled
                value={orderCode}
                className={inputClass + " bg-[#e8e8e8]"}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Lấy giá</label>
              <input type="text" disabled value="Bán hàng theo khách" className={inputClass} />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Kho xuất</label>
              <input type="text" disabled value={branch} className={inputClass} />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Nhân viên</label>
              <input type="text" disabled value={creator} className={inputClass} />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Người lập</label>
              <input type="text" disabled value={creator} className={inputClass} />
            </div>
          </div>

          {/* Column 2: Customer Info */}
          <div className="flex flex-col gap-1.5 border-l border-[#c0c0c0] pl-6">
            <div className="flex items-center">
              <label className={labelClass}>{partnerTitle}</label>
              <div className="flex-1 flex h-[24px]">
                <input
                  type="text"
                  disabled={isView}
                  value={partnerName}
                  readOnly
                  placeholder={partnerPlaceholder}
                  className={inputClass + " rounded-r-none border-r-0"}
                />
                <button 
                  disabled={isView}
                  onClick={() => !isView && onPartnerSearch()}
                  className="px-2 bg-[#d0d0d0] border border-[#a0a0a0] rounded-r-sm hover:bg-[#c0c0c0] disabled:opacity-50"
                >
                  <Search size={14} />
                </button>
              </div>
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Họ tên</label>
              <input
                type="text"
                disabled={isView}
                value={contactPerson}
                onChange={(e) => setContactPerson(e.target.value)}
                className={inputClass}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Điện thoại</label>
              <input
                type="text"
                disabled={isView}
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className={inputClass}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Địa chỉ</label>
              <input
                type="text"
                disabled={isView}
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className={inputClass}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Ghi chú</label>
              <input
                type="text"
                disabled={isView}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className={inputClass}
              />
            </div>
          </div>

          {/* Column 3: Financial Info */}
          <div className="flex flex-col gap-1.5 border-l border-[#c0c0c0] pl-6">
            <div className="flex items-center">
              <label className={labelClass}>Nợ trước</label>
              <input
                type="number"
                disabled={isView}
                value={oldDebt}
                onChange={(e) => setOldDebt(Number(e.target.value) || 0)}
                className={numInputClass}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Tiền hàng</label>
              <input
                type="text"
                disabled
                value={totalAmount.toLocaleString()}
                className={numInputClass + " bg-[#e8e8e8]"}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Chiết khấu</label>
              <input
                type="number"
                disabled={isView}
                value={discount}
                onChange={(e) => setDiscount(Number(e.target.value) || 0)}
                className={numInputClass + " w-16 shrink-0"}
              />
              <label className="w-16 text-right text-[12px] text-slate-800 pr-2 shrink-0">Thuế VAT</label>
              <input
                type="number"
                disabled={isView}
                value={tax}
                onChange={(e) => setTax(Number(e.target.value) || 0)}
                className={numInputClass}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Trả trước</label>
              <input
                type="number"
                disabled={isView}
                value={advancePayment}
                onChange={(e) => setAdvancePayment(Number(e.target.value) || 0)}
                className={numInputClass}
              />
            </div>
            <div className="flex items-center">
              <label className={labelClass}>Còn lại</label>
              <input
                type="text"
                disabled
                value={remainingBalance.toLocaleString()}
                className={numInputClass + " bg-[#e8e8e8] font-bold"}
              />
            </div>
          </div>

        </div>
      </div>

      {/* DataGrid */}
      <div className="flex-1 overflow-auto bg-white">
        <table className="w-full text-left border-collapse whitespace-nowrap">
          <thead className="sticky top-0 bg-[#f0f0f0] z-10 border-b border-[#a0a0a0]">
            <tr>
              <th className="px-2 py-1.5 border-r border-[#d0d0d0] font-normal text-center w-10">STT</th>
              <th className="px-2 py-1.5 border-r border-[#d0d0d0] font-normal">Hàng hóa</th>
              <th className="px-2 py-1.5 border-r border-[#d0d0d0] font-normal text-right w-24 text-red-500">Số lượng</th>
              <th className="px-2 py-1.5 border-r border-[#d0d0d0] font-normal text-right w-32">Đơn giá</th>
              <th className="px-2 py-1.5 border-r border-[#d0d0d0] font-normal text-right w-32">Chiết khấu</th>
              <th className="px-2 py-1.5 border-r border-[#d0d0d0] font-normal text-right w-32">Thành tiền</th>
              <th className="px-2 py-1.5 border-r border-[#d0d0d0] font-normal text-center w-32">Ghi chú</th>
              {!isView && <th className="px-2 py-1.5 w-10"></th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-[#e0e0e0]">
            {items.map((item, index) => (
              <tr key={item.id} className="hover:bg-[#FFFDE7]">
                <td className="px-2 py-1 border-r border-[#e0e0e0] text-center">{index + 1}</td>
                <td className="px-2 py-1 border-r border-[#e0e0e0]">
                  <input
                    type="text"
                    disabled={isView}
                    placeholder="Chọn hàng hóa..."
                    readOnly
                    onClick={() => {
                      if (!isView) onProductSearch(item.id);
                    }}
                    className="w-full h-full bg-transparent outline-none cursor-pointer uppercase"
                    value={item.productName}
                  />
                </td>
                <td className="px-2 py-1 border-r border-[#e0e0e0]">
                  <input
                    type="number"
                    disabled={isView}
                    min="1"
                    className="w-full h-full bg-transparent outline-none text-right text-red-600 font-semibold"
                    value={item.quantity}
                    onChange={(e) => onUpdateItem(item.id, 'quantity', parseInt(e.target.value) || 0)}
                  />
                </td>
                <td className="px-2 py-1 border-r border-[#e0e0e0]">
                  <input
                    type="number"
                    disabled={isView}
                    min="0"
                    className="w-full h-full bg-transparent outline-none text-right"
                    value={item.unitPrice}
                    onChange={(e) => onUpdateItem(item.id, 'unitPrice', parseFloat(e.target.value) || 0)}
                  />
                </td>
                <td className="px-2 py-1 border-r border-[#e0e0e0]">
                  <input
                    type="number"
                    disabled={isView}
                    className="w-full h-full bg-transparent outline-none text-right"
                    value={0}
                  />
                </td>
                <td className="px-2 py-1 border-r border-[#e0e0e0] text-right font-semibold">
                  {(item.quantity * item.unitPrice).toLocaleString()}
                </td>
                <td className="px-2 py-1 border-r border-[#e0e0e0]">
                  <input type="text" disabled={isView} className="w-full h-full bg-transparent outline-none" />
                </td>
                {!isView && (
                  <td className="px-2 py-1 text-center">
                    <button
                      type="button"
                      onClick={() => onRemoveItem(item.id)}
                      className="text-red-500 hover:text-red-700"
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {!isView && (
              <tr>
                <td colSpan={8} className="px-2 py-1">
                  <button
                    type="button"
                    onClick={onAddItem}
                    className="text-blue-600 hover:underline flex items-center gap-1"
                  >
                    <Plus size={14} /> Dòng mới
                  </button>
                </td>
              </tr>
            )}
          </tbody>
          <tfoot className="sticky bottom-0 bg-[#E3E9F0] border-t border-[#a0a0a0] font-semibold">
            <tr>
              <td colSpan={2} className="px-2 py-1.5 border-r border-[#d0d0d0] text-right">TỔNG CỘNG</td>
              <td className="px-2 py-1.5 border-r border-[#d0d0d0] text-right text-red-600">
                {items.reduce((sum, item) => sum + item.quantity, 0).toLocaleString()}
              </td>
              <td colSpan={2} className="px-2 py-1.5 border-r border-[#d0d0d0]"></td>
              <td className="px-2 py-1.5 border-r border-[#d0d0d0] text-right text-blue-700">
                {totalAmount.toLocaleString()}
              </td>
              <td colSpan={isView ? 1 : 2}></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
};
`

fs.writeFileSync('src/components/common/document/GenericDocumentForm.tsx', fileContent, 'utf8');
console.log('Successfully applied classic ERP layout matching the image!');
