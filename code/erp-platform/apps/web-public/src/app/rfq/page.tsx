'use client';
import { useState } from 'react';
import { submitRfq } from '../../features/products/api/productApi';
import type { components } from '@erp/api-contract';

type SalesInvoiceCreateDto = components['schemas']['SalesInvoiceCreateDto'];

export default function RfqPage() {
  const [formData, setFormData] = useState<SalesInvoiceCreateDto>({
    customerId: '00000000-0000-0000-0000-000000000001', // Giả sử khách hàng ID = UUID (hoặc lấy từ session nếu có login cho khách)
    note: '',
    paymentMethod: 'CASH',
    invoiceCode: 'RFQ-' + Date.now(),
    lines: [
      {
        productId: 1, // Hardcode cho demo (Long)
        productName: 'Gạch Ốp Lát Demo',
        quantity: 10,
        unitPrice: 150000,
        unitOfMeasure: 'Hộp'
      }
    ]
  });
  
  const [status, setStatus] = useState<string>('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatus('Submitting...');
    try {
      const res = await submitRfq(formData);
      if (res.success) {
        setStatus(`Đã gửi yêu cầu báo giá (RFQ) thành công! Mã RFQ: ` + res.data);
      } else {
        setStatus('Lỗi: ' + res.message);
      }
    } catch (err: any) {
      setStatus('Lỗi hệ thống: ' + err.message);
    }
  };

  return (
    <div className="p-8 max-w-lg mx-auto">
      <h1 className="text-3xl font-bold mb-4">Gửi Yêu Cầu Báo Giá (RFQ)</h1>
      <p className="mb-4 text-gray-600">Điền thông tin bên dưới để nhận báo giá tốt nhất cho đơn hàng số lượng lớn.</p>
      
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div>
          <label className="block text-sm font-medium">Ghi chú yêu cầu</label>
          <textarea 
            className="w-full border p-2 mt-1 rounded" 
            rows={4} 
            value={formData.note}
            onChange={(e) => setFormData({...formData, note: e.target.value})}
            placeholder="Ví dụ: Cần giao hàng trong 3 ngày tới tại TPHCM..."
          />
        </div>
        
        <button type="submit" className="bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700">
          Gửi Yêu Cầu RFQ
        </button>
      </form>

      {status && <div className="mt-4 p-4 border rounded bg-gray-50">{status}</div>}
    </div>
  );
}