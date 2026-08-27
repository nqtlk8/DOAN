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

  const rowHeight = "h-[34px]";
  const inputHeight = "h-[28px]";
  const borderBot = "border-b border-[#ccc]";
  
  // Shared base input style - no border for left/mid columns, border for right column
  const baseInput = \`\${inputHeight} w-full px-2 text-[13px] text-black outline-none focus:bg-[#FFFDE7] disabled:bg-transparent\`;
  const rightInput = \`\${inputHeight} w-full px-2 text-[13px] text-black outline-none border border-[#999] focus:bg-[#FFFDE7] disabled:bg-[#f0f0f0] text-right pr-[10px]\`;

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-white text-[13px]">
      {error && (
        <div className="px-4 py-2 bg-red-100 text-red-700 text-sm border-b border-red-200 flex items-center gap-2">
          <X size={14} /> {error}
        </div>
      )}

      {/* HEADER BỐ CỤC DẠNG BẢNG 3 CỘT (Grid Cha Duy Nhất) */}
      <div className="bg-[#E6ECF5] border-b border-t border-[#999] shrink-0">
        <div 
          className="grid"
          style={{ gridTemplateColumns: '530px 1fr 280px' }}
        >
          {/* KHU VỰC 2: KHỐI TRÁI */}
          <div className="border-r border-[#999] flex flex-col">
            {/* Dòng 1: Ngày / Số */}
            <div className={\`flex items-center \${rowHeight} \${borderBot}\`}>
              <div className="grid items-center h-full w-full" style={{ gridTemplateColumns: '50px 140px 40px 1fr' }}>
                <label className="text-right pr-2 text-[#333]">Ngày</label>
                <div className="px-1 flex items-center h-full">
                  <input type="date" disabled={isView} value={createdDate} onChange={e => setCreatedDate(e.target.value)} className={\`\${baseInput} border border-[#999] bg-white\`} />
                </div>
                <label className="text-right pr-2 text-[#333]">Số</label>
                <div className="px-1 flex items-center h-full">
                  <input type="text" disabled value={orderCode} className={\`\${baseInput} border border-[#999] bg-[#E8E8E8] font-bold\`} />
                </div>
              </div>
            </div>
            
            {/* Dòng 2: Lấy giá */}
            <div className={\`grid items-center \${rowHeight} \${borderBot}\`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <label className="text-left pl-2 font-medium text-[#444]">Lấy giá</label>
              <div className="pl-2 h-full flex items-center">
                <input type="text" disabled value="Bán hàng theo khách" className={\`\${baseInput} bg-[#E8E8E8] border border-[#ccc]\`} />
              </div>
            </div>

            {/* Dòng 3: Kho xuất */}
            <div className={\`grid items-center \${rowHeight} \${borderBot}\`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <label className="text-left pl-2 font-medium text-[#444]">Kho xuất</label>
              <div className="pl-2 h-full flex items-center">
                <input type="text" disabled value={branch} className={\`\${baseInput} bg-[#E8E8E8] border border-[#ccc]\`} />
              </div>
            </div>

            {/* Dòng 4: Nhân viên */}
            <div className={\`grid items-center \${rowHeight} \${borderBot}\`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <label className="text-left pl-2 font-medium text-[#444]">Nhân viên</label>
              <div className="pl-2 h-full flex items-center">
                <input type="text" disabled value={creator} className={\`\${baseInput} bg-[#E8E8E8] border border-[#ccc]\`} />
              </div>
            </div>

            {/* Dòng 5: Người lập */}
            <div className={\`grid items-center \${rowHeight}\`} style={{ gridTemplateColumns: '110px 1fr' }}>
              <label className="text-left pl-2 font-medium text-[#444]">Người lập</label>
              <div className="pl-2 h-full flex items-center">
                <input type="text" disabled value={creator} className={\`\${baseInput} bg-[#E8E8E8] border border-[#ccc]\`} />
              </div>
            </div>
          </div>

          {/* KHU VỰC 3: KHỐI GIỮA (Khách hàng) */}
          <div className="border-r border-[#999] flex flex-col">
            {/* Dòng 1: Khách hàng */}
            <div className={\`grid items-center \${rowHeight} \${borderBot}\`} style={{ gridTemplateColumns: '90px 1fr 32px' }}>
              <label className="text-right pr-2 text-[#333] font-medium">{partnerCodeLabel.split('/')[0]}</label>
              <div className="h-full flex items-center pr-1">
                <input type="text" disabled={isView} value={partnerName} readOnly placeholder={partnerPlaceholder} className={\`\${baseInput} border border-[#999] bg-white\`} />
              </div>
              <div className="h-full flex items-center pr-1">
                <button disabled={isView} onClick={() => !isView && onPartnerSearch()} className="w-full h-[28px] flex items-center justify-center bg-[#d4d0c8] border border-[#999] hover:bg-[#c0c0c0] active:bg-[#a0a0a0]">
                  <Search size={14} />
                </button>
              </div>
            </div>

            {/* Dòng 2: Họ tên */}
            <div className={\`grid items-center \${rowHeight} \${borderBot}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333] font-medium">Họ tên</label>
              <div className="pr-2 h-full flex items-center">
                <input type="text" disabled={isView} value={contactPerson} onChange={e => setContactPerson(e.target.value)} className={\`\${baseInput} border border-[#ccc] bg-white\`} />
              </div>
            </div>

            {/* Dòng 3: Điện thoại */}
            <div className={\`grid items-center \${rowHeight} \${borderBot}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333] font-medium">Điện thoại</label>
              <div className="pr-2 h-full flex items-center">
                <input type="text" disabled={isView} value={phone} onChange={e => setPhone(e.target.value)} className={\`\${baseInput} border border-[#ccc] bg-white\`} />
              </div>
            </div>

            {/* Dòng 4: Địa chỉ */}
            <div className={\`grid items-center \${rowHeight} \${borderBot}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333] font-medium">Địa chỉ</label>
              <div className="pr-2 h-full flex items-center">
                <input type="text" disabled={isView} value={address} onChange={e => setAddress(e.target.value)} className={\`\${baseInput} border border-[#ccc] bg-white\`} />
              </div>
            </div>

            {/* Dòng 5: Ghi chú */}
            <div className={\`grid items-center \${rowHeight}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333] font-medium">Ghi chú</label>
              <div className="pr-2 h-full flex items-center">
                <input type="text" disabled={isView} value={note} onChange={e => setNote(e.target.value)} className={\`\${baseInput} border border-[#ccc] bg-white\`} />
              </div>
            </div>
          </div>

          {/* KHU VỰC 4: KHỐI TÀI CHÍNH (Phải) */}
          <div className="flex flex-col pr-2">
            {/* Dòng 1: Nợ trước */}
            <div className={\`grid items-center \${rowHeight}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333]">Nợ trước</label>
              <div className="flex items-center">
                <input type="number" disabled={isView} value={oldDebt} onChange={e => setOldDebt(Number(e.target.value) || 0)} className={rightInput} />
              </div>
            </div>
            
            {/* Dòng 2: Tiền hàng */}
            <div className={\`grid items-center \${rowHeight}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333]">Tiền hàng</label>
              <div className="flex items-center">
                <input type="text" disabled value={totalAmount.toLocaleString()} className={\`\${rightInput} bg-[#E8E8E8]\`} />
              </div>
            </div>

            {/* Dòng 3: Chiết khấu / VAT */}
            <div className={\`grid items-center \${rowHeight}\`} style={{ gridTemplateColumns: '90px 80px 50px 1fr' }}>
              <label className="text-right pr-2 text-[#333]">Chiết khấu</label>
              <div className="flex items-center">
                <input type="number" disabled={isView} value={discount} onChange={e => setDiscount(Number(e.target.value) || 0)} className={rightInput} />
              </div>
              <label className="text-right pr-2 text-[#333]">Thuế</label>
              <div className="flex items-center">
                <input type="number" disabled={isView} value={tax} onChange={e => setTax(Number(e.target.value) || 0)} className={rightInput} />
              </div>
            </div>

            {/* Dòng 4: Trả trước */}
            <div className={\`grid items-center \${rowHeight}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333]">Trả trước</label>
              <div className="flex items-center">
                <input type="number" disabled={isView} value={advancePayment} onChange={e => setAdvancePayment(Number(e.target.value) || 0)} className={rightInput} />
              </div>
            </div>

            {/* Dòng 5: Còn lại */}
            <div className={\`grid items-center \${rowHeight}\`} style={{ gridTemplateColumns: '90px 1fr' }}>
              <label className="text-right pr-2 text-[#333]">Còn lại</label>
              <div className="flex items-center">
                <input type="text" disabled value={remainingBalance.toLocaleString()} className={\`\${rightInput} bg-[#E8E8E8] font-bold text-[14px]\`} />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* KHU VỰC 5: BẢNG DANH SÁCH HÀNG HÓA */}
      <div className="flex-1 overflow-auto bg-white p-2">
        <table className="w-full border-collapse border border-[#333] table-fixed">
          <thead className="sticky top-0 bg-[#f0f0f0] z-10">
            <tr>
              <th className="border border-[#333] px-2 py-1.5 font-normal text-center" style={{ width: '5%' }}>STT</th>
              <th className="border border-[#333] px-2 py-1.5 font-normal text-center" style={{ width: '30%' }}>Hàng hóa</th>
              <th className="border border-[#333] px-2 py-1.5 font-normal text-center text-red-600" style={{ width: '12%' }}>Số lượng</th>
              <th className="border border-[#333] px-2 py-1.5 font-normal text-center" style={{ width: '13%' }}>Đơn giá</th>
              <th className="border border-[#333] px-2 py-1.5 font-normal text-center" style={{ width: '10%' }}>Chiết khấu</th>
              <th className="border border-[#333] px-2 py-1.5 font-normal text-center" style={{ width: '15%' }}>Thành tiền</th>
              <th className="border border-[#333] px-2 py-1.5 font-normal text-center" style={{ width: '15%' }}>Ghi chú</th>
              {!isView && <th className="border border-[#333] px-2 py-1.5" style={{ width: '40px' }}></th>}
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={item.id} className="hover:bg-[#E2EDF8]">
                <td className="border border-[#333] px-2 py-1 text-center">{index + 1}</td>
                <td className="border border-[#333] px-2 py-0">
                  <input
                    type="text"
                    disabled={isView}
                    placeholder="Bấm chọn..."
                    readOnly
                    onClick={() => { if (!isView) onProductSearch(item.id); }}
                    className="w-full h-[28px] bg-transparent outline-none cursor-pointer uppercase text-left"
                    value={item.productName}
                  />
                </td>
                <td className="border border-[#333] px-2 py-0">
                  <input
                    type="number"
                    disabled={isView}
                    min="1"
                    className="w-full h-[28px] bg-transparent outline-none text-right text-red-600 font-bold"
                    value={item.quantity}
                    onChange={(e) => onUpdateItem(item.id, 'quantity', parseInt(e.target.value) || 0)}
                  />
                </td>
                <td className="border border-[#333] px-2 py-0">
                  <input
                    type="number"
                    disabled={isView}
                    min="0"
                    className="w-full h-[28px] bg-transparent outline-none text-right"
                    value={item.unitPrice}
                    onChange={(e) => onUpdateItem(item.id, 'unitPrice', parseFloat(e.target.value) || 0)}
                  />
                </td>
                <td className="border border-[#333] px-2 py-0">
                  <input type="number" disabled={isView} className="w-full h-[28px] bg-transparent outline-none text-right" value={0} />
                </td>
                <td className="border border-[#333] px-2 py-1 text-right font-bold text-black">
                  {(item.quantity * item.unitPrice).toLocaleString()}
                </td>
                <td className="border border-[#333] px-2 py-0">
                  <input type="text" disabled={isView} className="w-full h-[28px] bg-transparent outline-none text-left" />
                </td>
                {!isView && (
                  <td className="border border-[#333] text-center">
                    <button type="button" onClick={() => onRemoveItem(item.id)} className="text-red-600 hover:text-red-800 p-1">
                      <Trash2 size={14} />
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {!isView && items.length === 0 && (
              <tr>
                <td colSpan={8} className="border border-[#333] px-2 py-8 text-center text-[#666]">
                  Chưa có dữ liệu. Hãy bấm "Thêm dòng" bên dưới.
                </td>
              </tr>
            )}
            {!isView && items.length > 0 && (
              <tr>
                <td colSpan={8} className="border border-[#333] px-2 py-1">
                  <button type="button" onClick={onAddItem} className="text-blue-700 hover:underline flex items-center gap-1 font-medium">
                    <Plus size={14} /> Dòng mới
                  </button>
                </td>
              </tr>
            )}
            {!isView && items.length === 0 && (
              <tr className="hidden">
                 <td><button onClick={onAddItem} id="btn-add-hidden">Add</button></td>
              </tr>
            )}
          </tbody>
          <tfoot className="sticky bottom-0 bg-[#E6ECF5]">
            {!isView && items.length === 0 && (
               <tr>
                <td colSpan={8} className="border border-[#333] px-2 py-2 text-center">
                  <button type="button" onClick={onAddItem} className="inline-flex items-center gap-1 text-blue-700 hover:underline font-medium">
                    <Plus size={14} /> Thêm dòng mới
                  </button>
                </td>
              </tr>
            )}
            <tr>
              <td colSpan={2} className="border border-[#333] border-t-2 border-t-[#333] px-2 py-2 text-center font-bold text-[#333]">
                TỔNG CỘNG
              </td>
              <td className="border border-[#333] border-t-2 border-t-[#333] px-2 py-2 text-right font-bold text-red-600">
                {items.reduce((sum, item) => sum + item.quantity, 0).toLocaleString()}
              </td>
              <td colSpan={2} className="border border-[#333] border-t-2 border-t-[#333]"></td>
              <td className="border border-[#333] border-t-2 border-t-[#333] px-2 py-2 text-right font-bold text-black text-[14px]">
                {totalAmount.toLocaleString()}
              </td>
              <td colSpan={isView ? 1 : 2} className="border border-[#333] border-t-2 border-t-[#333]"></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
};
`

fs.writeFileSync('src/components/common/document/GenericDocumentForm.tsx', fileContent, 'utf8');
console.log('Successfully applied super dense precise grid layout!');
