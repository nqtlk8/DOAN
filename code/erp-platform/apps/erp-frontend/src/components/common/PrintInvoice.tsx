import React from 'react';

interface PrintInvoiceProps {
  storeName?: string;
  storePhone?: string;
  storeAddress?: string;
  customerName: string;
  customerPhone?: string;
  customerAddress?: string;
  items: Array<{
    productName: string;
    quantity: number;
    unitPrice: number;
  }>;
  totalAmount: number;
  advancePayment: number;
  remainingBalance: number;
  oldDebt?: number;
  mode?: 'STAFF' | 'quote' | 'return' | 'edit' | 'purchase' | 'sales';
  title?: string;
  hidePrices?: boolean;
  className?: string;
}

export const PrintInvoice: React.FC<PrintInvoiceProps> = ({
  storeName = 'CỬA HÀNG VẬT TƯ ERP',
  storePhone = '0903.092.027 - 02773.924.460',
  storeAddress = '123 Đường Số 1, Quận 1, TP.HCM',
  customerName,
  customerPhone = '',
  customerAddress = '',
  items,
  totalAmount,
  advancePayment,
  remainingBalance,
  oldDebt = 0,
  mode = 'STAFF',
  title,
  hidePrices = false,
  className = '',
}) => {
  const displayTitle =
    title ||
    (mode === 'quote'
      ? 'BÁO GIÁ / ĐẶT TRƯỚC'
      : mode === 'return'
        ? 'PHIẾU KHÁCH TRẢ HÀNG'
        : mode === 'purchase'
          ? 'PHIẾU NHẬP HÀNG'
          : 'PHIẾU XUẤT HÀNG');
  const now = new Date();

  // Format numbers to currency without decimal if not needed, typical VN format
  const formatMoney = (amount: number) => {
    return new Intl.NumberFormat('vi-VN').format(amount);
  };

  return (
    <div className={`p-8 bg-white text-black font-sans text-[15px] ${className}`}>
      {/* Thông tin cửa hàng (Top Left) */}
      <div className="mb-4 text-sm leading-tight">
        <h1 className="font-bold uppercase text-lg">{storeName}</h1>
        <p>{storePhone}</p>
        <p>{storeAddress}</p>
      </div>

      {/* Tiêu đề Hóa Đơn */}
      <div className="text-center mb-6">
        <h2 className="text-2xl font-bold uppercase">{displayTitle}</h2>
        <p className="italic text-sm">
          ngày {now.getDate()} tháng {now.getMonth() + 1} năm {now.getFullYear()}
        </p>
      </div>

      {/* Thông tin Khách Hàng / Đối tác */}
      <div className="mb-4">
        <table className="w-full text-left">
          <tbody>
            <tr>
              <td className="w-24 align-top font-semibold">Tên:</td>
              <td className="uppercase">{customerName || 'KHÁCH LẺ'}</td>
            </tr>
            <tr>
              <td className="w-24 align-top font-semibold">Điện thoại:</td>
              <td>{customerPhone}</td>
            </tr>
            <tr>
              <td className="w-24 align-top font-semibold">Địa chỉ:</td>
              <td>{customerAddress}</td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* Bảng Hàng Hóa */}
      <table className="w-full text-left border-collapse border border-black mb-1 text-sm">
        <thead>
          <tr className="border-b border-black">
            <th className="py-1 border-r border-black text-center w-12">STT</th>
            <th className="py-1 border-r border-black text-center">HÀNG HÓA</th>
            <th className="py-1 border-r border-black text-center w-24">SỐ LƯỢNG</th>
            {!hidePrices && <th className="py-1 border-r border-black text-center w-32">ĐƠN GIÁ</th>}
            {!hidePrices && <th className="py-1 text-center w-32">THÀNH TIỀN</th>}
          </tr>
        </thead>
        <tbody>
          {items.map((item, index) => (
            <tr key={index} className="border-b border-slate-300">
              <td className="py-1 border-r border-black text-center">{index + 1}</td>
              <td className="py-1 border-r border-black px-2">{item.productName || 'Chưa nhập tên'}</td>
              <td className="py-1 border-r border-black text-center font-semibold">{item.quantity}</td>
              {!hidePrices && (
                <td className="py-1 border-r border-black text-right px-2">{formatMoney(item.unitPrice)}</td>
              )}
              {!hidePrices && <td className="py-1 text-right px-2">{formatMoney(item.quantity * item.unitPrice)}</td>}
            </tr>
          ))}
          {items.length === 0 && (
            <tr>
              <td colSpan={hidePrices ? 3 : 5} className="py-4 text-center text-slate-500 italic">
                Không có sản phẩm
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {/* Chữ ký & Tổng kết tài chính */}
      <div className="flex justify-between items-start mb-8 text-sm border border-black border-t-0 p-1">
        {/* Chữ ký (Trái) */}
        <div className="flex justify-around w-1/2 pt-2">
          <div className="text-center">
            <p className="font-semibold">{mode === 'purchase' ? 'Nhà Cung Cấp' : 'Khách Hàng'}</p>
            <p className="italic text-slate-600 text-xs">(Ký/họ tên)</p>
            <div className="h-24"></div>
          </div>
          <div className="text-center">
            <p className="font-semibold">Lập Phiếu</p>
            <p className="italic text-slate-600 text-xs">(Ký/họ tên)</p>
            <div className="h-24"></div>
          </div>
        </div>

        {/* Tổng kết (Phải) */}
        {!hidePrices && (
          <div className="w-1/2 border-l border-black pl-2">
            <table className="w-full text-left font-semibold">
              <tbody>
                <tr>
                  <td className="py-1 border-b border-slate-300">Tiền hàng:</td>
                  <td className="py-1 border-b border-slate-300 text-right">{formatMoney(totalAmount)}</td>
                </tr>
                {mode !== 'return' && mode !== 'quote' && (
                  <>
                    <tr>
                      <td className="py-1 border-b border-slate-300">Nợ cũ:</td>
                      <td className="py-1 border-b border-slate-300 text-right">{formatMoney(oldDebt)}</td>
                    </tr>
                    <tr>
                      <td className="py-1 border-b border-slate-300">Đã trả:</td>
                      <td className="py-1 border-b border-slate-300 text-right">{formatMoney(advancePayment)}</td>
                    </tr>
                    <tr>
                      <td className="py-1 pt-2">Còn lại:</td>
                      <td className="py-1 pt-2 text-right">{formatMoney(remainingBalance)}</td>
                    </tr>
                  </>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Thông tin chuyển khoản / Lời cảm ơn (Đáy) */}
      <div className="text-center text-sm">
        <p className="font-semibold">Thông tin chuyển khoản:</p>
        <p>Tên chủ TK: {storeName}</p>
        <p>Số TK: 070123456789</p>
        <p className="mb-2">SACOMBANK - CHI NHÁNH ĐỒNG THÁP</p>
        <p className="border-t border-slate-300 mx-auto w-1/3 my-2"></p>
        <p className="italic">Mua trọn gói trang trí Tặng Bộ Bàn.</p>
        <p className="italic">Mua sắt thép, cát đá, xi măng, gạch ống không tặng.</p>
        <p className="italic mt-1 font-semibold">Cảm ơn quý khách!</p>
      </div>
    </div>
  );
};
