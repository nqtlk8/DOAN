import React from 'react';
import { Plus, Trash2, X } from 'lucide-react';

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
  mode, title, error, orderCode,
  creator, branch, createdDate, setCreatedDate, note, setNote,
  partnerTitle, partnerCodeLabel, partnerPlaceholder, partnerName, onPartnerSearch, address, setAddress, phone, setPhone, contactPerson, setContactPerson,
  oldDebt, setOldDebt, totalAmount, discount, setDiscount, tax, setTax, advancePayment, setAdvancePayment, remainingBalance,
  items, onAddItem, onRemoveItem, onUpdateItem, onProductSearch
}) => {
  const isView = mode === 'VIEW';

  return (
    <div className="flex-1 overflow-y-auto">
      {/* Header Title */}
      <div className="px-6 py-4 flex items-center justify-between bg-blue-50/50 border-b border-blue-100">
        <h2 className="text-xl font-bold text-blue-800 uppercase tracking-wide">
          {title}
        </h2>
        <div className="flex gap-2">
          <span className={`px-3 py-1 text-xs font-semibold rounded-full border ${
            isView ? 'bg-slate-100 text-slate-600 border-slate-200' : 
            mode === 'ADD' ? 'bg-green-100 text-green-700 border-green-200' : 'bg-orange-100 text-orange-700 border-orange-200'
          }`}>
            TRẠNG THÁI FORM: {mode}
          </span>
        </div>
      </div>

      {error && (
        <div className="mx-6 mt-4 p-4 bg-red-50 text-red-600 rounded-xl text-sm border border-red-100 flex items-center gap-2">
          <X size={16} /> {error}
        </div>
      )}

      <div className="p-6">
        {/* 3-Column Header Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8 border border-slate-200 p-6 rounded-2xl bg-slate-50/30 shadow-sm">
          
          {/* Column 1: Internal Info */}
          <div className="space-y-4">
            <h3 className="text-sm text-slate-900 font-semibold uppercase tracking-wider border-b border-slate-200 pb-2 mb-4">1. Thông tin nội bộ</h3>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Người lập phiếu</label>
              <input type="text" disabled value={creator} className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-900" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Chi nhánh</label>
              <input type="text" disabled value={branch} className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-900" />
            </div>
            <div className="flex gap-2">
              <div className="flex-1">
                <label className="block text-xs font-medium text-slate-500 mb-1">Ngày lập</label>
                <input type="date" disabled={isView} value={createdDate} onChange={e => setCreatedDate(e.target.value)} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500" />
              </div>
              <div className="flex-1">
                <label className="block text-xs font-medium text-slate-500 mb-1">Mã chứng từ</label>
                <input type="text" disabled value={orderCode} className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-900 font-semibold" />
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Ghi chú</label>
              <textarea disabled={isView} value={note} onChange={e => setNote(e.target.value)} rows={2} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500"></textarea>
            </div>
          </div>

          {/* Column 2: Customer/Partner Info */}
          <div className="space-y-4 border-l border-slate-200 pl-8">
            <h3 className="text-sm text-slate-900 font-semibold uppercase tracking-wider border-b border-slate-200 pb-2 mb-4">2. {partnerTitle}</h3>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">{partnerCodeLabel}</label>
              <input 
                type="text" 
                disabled={isView} 
                value={partnerName} 
                readOnly
                onClick={() => !isView && onPartnerSearch()}
                placeholder={partnerPlaceholder} 
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500 cursor-pointer" 
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Địa chỉ</label>
              <input type="text" disabled={isView} value={address} onChange={e => setAddress(e.target.value)} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500" />
            </div>
            <div className="flex gap-2">
              <div className="flex-1">
                <label className="block text-xs font-medium text-slate-500 mb-1">Điện thoại</label>
                <input type="text" disabled={isView} value={phone} onChange={e => setPhone(e.target.value)} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500" />
              </div>
              <div className="flex-1">
                <label className="block text-xs font-medium text-slate-500 mb-1">Người liên hệ</label>
                <input type="text" disabled={isView} value={contactPerson} onChange={e => setContactPerson(e.target.value)} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:ring-2 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-500" />
              </div>
            </div>
          </div>

          {/* Column 3: Financial Info */}
          <div className="space-y-4 border-l border-slate-200 pl-8">
            <h3 className="text-sm text-slate-900 font-semibold uppercase tracking-wider border-b border-slate-200 pb-2 mb-4">3. Tài chính / Thanh toán</h3>
            <div className="flex justify-between items-center">
              <label className="text-xs font-medium text-slate-500">Nợ trước</label>
              <input type="number" disabled={isView} value={oldDebt} onChange={e => setOldDebt(Number(e.target.value) || 0)} className="w-1/2 px-2 py-1 bg-white border border-slate-200 rounded text-sm text-slate-900 text-right focus:ring-1 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-700" />
            </div>
            <div className="flex justify-between items-center">
              <label className="text-xs font-medium text-slate-500">Tiền hàng</label>
              <div className="w-1/2 px-2 py-1 bg-slate-100 border border-slate-200 rounded text-sm text-right font-semibold text-slate-700">
                {totalAmount.toLocaleString()}
              </div>
            </div>
            <div className="flex gap-2">
              <div className="flex-1 flex justify-between items-center">
                <label className="text-xs font-medium text-slate-500">Chiết khấu</label>
                <input type="number" disabled={isView} value={discount} onChange={e => setDiscount(Number(e.target.value) || 0)} className="w-1/2 px-2 py-1 bg-white border border-slate-200 rounded text-sm text-slate-900 text-right focus:ring-1 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-700" />
              </div>
              <div className="flex-1 flex justify-between items-center">
                <label className="text-xs font-medium text-slate-500">Thuế VAT</label>
                <input type="number" disabled={isView} value={tax} onChange={e => setTax(Number(e.target.value) || 0)} className="w-1/2 px-2 py-1 bg-white border border-slate-200 rounded text-sm text-slate-900 text-right focus:ring-1 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-700" />
              </div>
            </div>
            <div className="flex justify-between items-center">
              <label className="text-xs font-medium text-slate-500">Trả trước</label>
              <input type="number" disabled={isView} value={advancePayment} onChange={e => setAdvancePayment(Number(e.target.value) || 0)} className="w-1/2 px-2 py-1 bg-white border border-slate-200 rounded text-sm text-slate-900 text-right focus:ring-1 focus:ring-blue-500 disabled:bg-slate-100 disabled:text-slate-700" />
            </div>
            <div className="flex justify-between items-center pt-2 border-t border-slate-200">
              <label className="text-xs font-medium text-slate-500">Còn lại</label>
              <div className="w-1/2 px-2 py-1 bg-slate-100 border border-slate-200 rounded text-sm text-right font-bold text-red-600">
                {remainingBalance.toLocaleString()}
              </div>
            </div>
          </div>

        </div>

        {/* DataGrid */}
        <div>
          <div className="flex justify-between items-center mb-3">
            <h3 className="text-md font-semibold text-slate-800">Chi tiết hàng hóa</h3>
            {!isView && (
              <button
                type="button"
                onClick={onAddItem}
                className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
              >
                <Plus size={16} /> Thêm dòng (Insert)
              </button>
            )}
          </div>

          <div className="border border-slate-200 rounded-xl overflow-x-auto shadow-sm">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-100 border-b border-slate-200">
                  <th className="px-4 py-2 text-xs font-semibold text-slate-600 uppercase w-12 text-center">STT</th>
                  <th className="px-4 py-2 text-xs font-semibold text-slate-600 uppercase">Sản phẩm</th>
                  <th className="px-4 py-2 text-xs font-semibold text-slate-600 uppercase w-32">Số lượng</th>
                  <th className="px-4 py-2 text-xs font-semibold text-slate-600 uppercase w-40">Đơn giá</th>
                  <th className="px-4 py-2 text-xs font-semibold text-slate-600 uppercase w-40 text-right">Thành tiền</th>
                  {!isView && <th className="px-4 py-2 text-xs font-semibold text-slate-600 uppercase w-12"></th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {items.map((item, index) => (
                  <tr key={item.id} className="hover:bg-blue-50/30 transition-colors">
                    <td className="px-4 py-2 text-sm text-slate-500 text-center">{index + 1}</td>
                    <td className="px-4 py-2">
                      <input
                        type="text"
                        disabled={isView}
                        placeholder="Nhấn chọn sản phẩm..."
                        readOnly
                        onClick={() => {
                          if (!isView) {
                            onProductSearch(item.id);
                          }
                        }}
                        className="w-full px-2 py-1 bg-transparent border-b border-transparent text-slate-900 focus:border-blue-500 focus:outline-none text-sm disabled:text-slate-800 cursor-pointer"
                        value={item.productName}
                      />
                    </td>
                    <td className="px-4 py-2">
                      <input
                        type="number"
                        disabled={isView}
                        min="1"
                        className="w-full px-2 py-1 bg-transparent border-b border-transparent focus:border-blue-500 focus:outline-none text-sm text-slate-900 text-right disabled:text-slate-800"
                        value={item.quantity}
                        onChange={(e) => onUpdateItem(item.id, 'quantity', parseInt(e.target.value) || 0)}
                      />
                    </td>
                    <td className="px-4 py-2">
                      <input
                        type="number"
                        disabled={isView}
                        min="0"
                        className="w-full px-2 py-1 bg-transparent border-b border-transparent focus:border-blue-500 focus:outline-none text-sm text-slate-900 text-right disabled:text-slate-800"
                        value={item.unitPrice}
                        onChange={(e) => onUpdateItem(item.id, 'unitPrice', parseFloat(e.target.value) || 0)}
                      />
                    </td>
                    <td className="px-4 py-2 text-sm font-medium text-slate-800 text-right">
                      {(item.quantity * item.unitPrice).toLocaleString()}
                    </td>
                    {!isView && (
                      <td className="px-4 py-2 text-center">
                        <button
                          type="button"
                          onClick={() => onRemoveItem(item.id)}
                          className="p-1 text-slate-400 hover:text-red-500 rounded transition-colors"
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr>
                    <td colSpan={isView ? 5 : 6} className="px-4 py-8 text-center text-slate-500 text-sm">
                      Chưa có dữ liệu. {isView ? '' : 'Nhấn F5 hoặc "Thêm dòng" để bắt đầu.'}
                    </td>
                  </tr>
                )}
              </tbody>
              <tfoot className="bg-slate-50/50 border-t border-slate-200">
                <tr>
                  <td colSpan={3}></td>
                  <td className="px-4 py-3 text-sm font-semibold text-slate-600 text-right">TỔNG TIỀN:</td>
                  <td className="px-4 py-3 text-lg font-bold text-blue-600 text-right">{totalAmount.toLocaleString()}</td>
                  {!isView && <td></td>}
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
