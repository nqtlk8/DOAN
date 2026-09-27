const fs = require('fs');
let code = fs.readFileSync('src/components/common/document/GenericDocumentForm.tsx', 'utf8');

// The current table wrapper:
// {/* KHU VỰC 5: BẢNG DANH SÁCH HÀNG HÓA */}
// <div className="flex-1 w-full max-w-[1600px] min-w-[1024px] mx-auto overflow-auto bg-white p-[10px]">
//   <table className="w-full border-collapse border border-[#333] table-fixed">
//      <thead ...>
//      <tbody ...>
//      <tfoot className="sticky bottom-0 bg-[#f0f0f0]">

// We will change it to:
// <div className="flex-1 w-full max-w-[1600px] min-w-[1024px] mx-auto flex flex-col bg-white p-[10px]">
//   <div className="flex-1 overflow-auto border border-b-0 border-[#333]">
//     <table className="w-full border-collapse table-fixed">
//       <thead className="sticky top-0 bg-[#f0f0f0] z-10">...</thead>
//       <tbody>...</tbody>
//     </table>
//   </div>
//   <table className="w-full border-collapse border border-[#333] table-fixed shrink-0 bg-[#f0f0f0]">
//     <tfoot>...</tfoot>
//   </table>
// </div>

const startTag = `{/* KHU VỰC 5: BẢNG DANH SÁCH HÀNG HÓA */}`;
const endTag = `</div>\n    </div>\n  );\n};`;

const newTable = `{/* KHU VỰC 5: BẢNG DANH SÁCH HÀNG HÓA */}
      <div className="flex-1 w-full max-w-[1600px] min-w-[1024px] mx-auto flex flex-col bg-white p-[10px] pb-0">
        <div className="flex-1 overflow-auto border border-b-0 border-[#333]">
          <table className="w-full border-collapse table-fixed">
            <thead className="sticky top-0 bg-[#f0f0f0] z-10 shadow-sm">
              <tr>
                <th className="border-b border-r border-[#333] px-[10px] py-[6px] font-semibold text-[#000] text-center" style={{ width: '5%' }}>STT</th>
                <th className="border-b border-r border-[#333] px-[10px] py-[6px] font-semibold text-[#000] text-center" style={{ width: '30%' }}>Hàng hóa</th>
                <th className="border-b border-r border-[#333] px-[10px] py-[6px] font-semibold text-[#000] text-center" style={{ width: '12%' }}>Số lượng</th>
                <th className="border-b border-r border-[#333] px-[10px] py-[6px] font-semibold text-[#000] text-center" style={{ width: '13%' }}>Đơn giá</th>
                <th className="border-b border-r border-[#333] px-[10px] py-[6px] font-semibold text-[#000] text-center" style={{ width: '10%' }}>Chiết khấu</th>
                <th className="border-b border-r border-[#333] px-[10px] py-[6px] font-semibold text-[#000] text-center" style={{ width: '15%' }}>Thành tiền</th>
                <th className="border-b border-r border-[#333] px-[10px] py-[6px] font-semibold text-[#000] text-center" style={{ width: '15%' }}>Ghi chú</th>
                {!isView && <th className="border-b border-[#333] px-[10px] py-[6px]" style={{ width: '40px' }}></th>}
              </tr>
            </thead>
            <tbody>
              {items.map((item, index) => (
                <tr key={item.id} className="hover:bg-[#E2EDF8]">
                  <td className="border-b border-r border-[#333] px-[10px] py-[4px] text-center">{index + 1}</td>
                  <td className="border-b border-r border-[#333] px-0 py-0">
                    <input type="text" disabled={isView} placeholder="Bấm chọn..." readOnly onClick={() => { if (!isView) onProductSearch(item.id); }} className="w-full h-[28px] px-[10px] bg-transparent outline-none cursor-pointer uppercase text-left text-black" value={item.productName} />
                  </td>
                  <td className="border-b border-r border-[#333] px-0 py-0">
                    <input type="number" disabled={isView} min="1" className="w-full h-[28px] px-[10px] bg-transparent outline-none text-right text-red-600 font-bold" value={item.quantity} onChange={(e) => onUpdateItem(item.id, 'quantity', parseInt(e.target.value) || 0)} />
                  </td>
                  <td className="border-b border-r border-[#333] px-0 py-0">
                    <input type="number" disabled={isView} min="0" className="w-full h-[28px] px-[10px] bg-transparent outline-none text-right text-black" value={item.unitPrice} onChange={(e) => onUpdateItem(item.id, 'unitPrice', parseFloat(e.target.value) || 0)} />
                  </td>
                  <td className="border-b border-r border-[#333] px-0 py-0">
                    <input type="number" disabled={isView} className="w-full h-[28px] px-[10px] bg-transparent outline-none text-right text-black" value={0} />
                  </td>
                  <td className="border-b border-r border-[#333] px-[10px] py-[4px] text-right font-bold text-black">
                    {(item.quantity * item.unitPrice).toLocaleString()}
                  </td>
                  <td className="border-b border-r border-[#333] px-0 py-0">
                    <input type="text" disabled={isView} className="w-full h-[28px] px-[10px] bg-transparent outline-none text-left text-black" />
                  </td>
                  {!isView && (
                    <td className="border-b border-[#333] text-center">
                      <button type="button" onClick={() => onRemoveItem(item.id)} className="text-red-600 hover:text-red-800 p-1">
                        <Trash2 size={14} />
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {!isView && items.length === 0 && (
                <tr>
                  <td colSpan={8} className="border-b border-[#333] px-[10px] py-8 text-center text-[#666]">
                    Chưa có dữ liệu. Hãy bấm "Dòng mới" bên dưới.
                  </td>
                </tr>
              )}
              {!isView && items.length > 0 && (
                <tr>
                  <td colSpan={8} className="border-b border-[#333] px-[10px] py-[4px]">
                    <button type="button" onClick={onAddItem} className="text-blue-700 hover:underline flex items-center gap-1 font-medium">
                      <Plus size={14} /> Dòng mới
                    </button>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        
        {/* TỔNG CỘNG (Luôn ở dưới cùng) */}
        <table className="w-full border-collapse border border-[#333] border-t-[2px] table-fixed shrink-0 bg-[#f0f0f0]">
          <tfoot>
            <tr>
              <td colSpan={2} style={{ width: '35%' }} className="border-r border-[#333] px-[10px] py-[6px] text-center font-bold text-[#000]">
                TỔNG CỘNG
              </td>
              <td style={{ width: '12%' }} className="border-r border-[#333] px-[10px] py-[6px] text-right font-bold text-red-600">
                {items.reduce((sum, item) => sum + item.quantity, 0).toLocaleString()}
              </td>
              <td colSpan={2} style={{ width: '23%' }} className="border-r border-[#333]"></td>
              <td style={{ width: '15%' }} className="border-r border-[#333] px-[10px] py-[6px] text-right font-bold text-black text-[14px]">
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
`;

const startIndex = code.indexOf(startTag);
if(startIndex > -1) {
    code = code.substring(0, startIndex) + newTable;
    fs.writeFileSync('src/components/common/document/GenericDocumentForm.tsx', code, 'utf8');
    console.log('Fixed sticky total table!');
} else {
    console.log('Could not find start tag');
}
