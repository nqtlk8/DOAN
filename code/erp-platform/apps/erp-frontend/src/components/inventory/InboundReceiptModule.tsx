import React, { useState, useEffect } from 'react';
import { useInboundReceipt } from '../../hooks/useInboundReceipt';
import { ApiService } from '../../api/ApiService';
import { SearchModal } from '../common/SearchModal';
import { Plus, Trash2, Save, Check } from 'lucide-react';
import type { Supplier, Product } from '../../types/catalog';
import { toast } from 'react-hot-toast';

export const InboundReceiptModule: React.FC<{ mode?: 'ADD' | 'VIEW' | 'EDIT' }> = ({ mode: initialMode = 'ADD' }) => {
  const { mode, isLoading, receiptId, handleSubmit, handleConfirm } = useInboundReceipt(null, initialMode);
  
  const [supplierId, setSupplierId] = useState<string>('');
  const [supplierName, setSupplierName] = useState<string>('');
  const [items, setItems] = useState<{ productId: string; productName: string; quantity: number; unitPrice: number; unit: string }[]>([]);
  const [note, setNote] = useState('');
  
  const [showSupplierSearch, setShowSupplierSearch] = useState(false);
  const [showProductSearch, setShowProductSearch] = useState(false);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);

  useEffect(() => {
    ApiService.Catalog.getSuppliers().then(setSuppliers).catch(console.error);
    ApiService.Catalog.getProducts().then(setProducts).catch(console.error);
  }, []);

  const totalAmount = items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);

  const onSaveDraft = () => {
    if (!supplierId || items.length === 0) {
      toast.error('Vui lòng chọn NCC và thêm sản phẩm');
      return;
    }
    const payload = {
      supplierId,
      note,
      lines: items.map(i => ({ productId: i.productId, quantity: i.quantity, unitPrice: i.unitPrice }))
    };
    handleSubmit(payload as any, (data) => {
      toast.success('Lưu nháp thành công');
    });
  };

  const onConfirm = () => {
    if (!receiptId) {
      toast.error('Vui lòng lưu nháp trước khi xác nhận');
      return;
    }
    handleConfirm(receiptId, () => {
      toast.success('Xác nhận phiếu nhập thành công');
    });
  };

  const addItem = (product: Product) => {
    setItems(prev => [...prev, { productId: String(product.id), productName: product.name, quantity: 1, unitPrice: product.price || 0, unit: product.baseUnit || '' }]);
    setShowProductSearch(false);
  };

  const removeItem = (idx: number) => {
    setItems(prev => prev.filter((_, i) => i !== idx));
  };

  return (
    <div className="p-6 max-w-5xl mx-auto flex flex-col h-full overflow-auto">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold">Phiếu Nhập Hàng</h2>
        <div className="flex space-x-2">
          {mode === 'ADD' && !receiptId && (
            <button data-testid="inbound-save-draft" onClick={onSaveDraft} disabled={isLoading} className="bg-blue-600 text-white px-4 py-2 rounded flex items-center hover:bg-blue-700">
              <Save size={18} className="mr-2" /> Lưu nháp
            </button>
          )}
          {receiptId && (
            <button data-testid="inbound-confirm" onClick={onConfirm} disabled={isLoading} className="bg-green-600 text-white px-4 py-2 rounded flex items-center hover:bg-green-700">
              <Check size={18} className="mr-2" /> Xác nhận
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 mb-6">
        <div className="border p-4 rounded bg-white shadow-sm">
          <label className="block text-sm font-medium mb-1">Nhà cung cấp</label>
          <div className="flex space-x-2">
            <input type="text" readOnly value={supplierName} className="border p-2 rounded flex-1 bg-slate-50" placeholder="Chọn nhà cung cấp..." />
            <button data-testid="inbound-supplier-search" onClick={() => setShowSupplierSearch(true)} className="bg-slate-200 px-3 rounded hover:bg-slate-300">Tìm</button>
          </div>
          
          <label className="block text-sm font-medium mb-1 mt-4">Ghi chú</label>
          <textarea value={note} onChange={e => setNote(e.target.value)} className="border p-2 rounded w-full" rows={2} />
        </div>
        <div className="border p-4 rounded bg-white shadow-sm flex flex-col justify-center items-center">
          <div className="text-lg text-slate-500">Tổng tiền</div>
          <div className="text-3xl font-bold text-blue-600">{totalAmount.toLocaleString()} đ</div>
          {receiptId && <div className="mt-2 px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full font-medium">Trạng thái: Bản nháp</div>}
        </div>
      </div>

      <div className="border rounded bg-white shadow-sm flex-1 flex flex-col">
        <div className="p-4 border-b flex justify-between items-center bg-slate-50">
          <h3 className="font-semibold">Danh sách sản phẩm</h3>
          <button data-testid="inbound-add-product" onClick={() => setShowProductSearch(true)} className="text-blue-600 flex items-center hover:text-blue-800">
            <Plus size={18} className="mr-1" /> Thêm dòng
          </button>
        </div>
        <div className="p-0 overflow-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-100">
              <tr>
                <th className="p-3 border-b">Tên sản phẩm</th>
                <th className="p-3 border-b w-24">Số lượng</th>
                <th className="p-3 border-b w-32">Đơn giá</th>
                <th className="p-3 border-b w-32">Thành tiền</th>
                <th className="p-3 border-b w-16"></th>
              </tr>
            </thead>
            <tbody>
              {items.map((item, idx) => (
                <tr key={idx} className="border-b">
                  <td className="p-3">{item.productName} ({item.unit})</td>
                  <td className="p-3">
                    <input type="number" min="1" value={item.quantity} onChange={e => {
                      const newItems = [...items];
                      newItems[idx].quantity = Number(e.target.value);
                      setItems(newItems);
                    }} className="w-full border p-1 rounded" />
                  </td>
                  <td className="p-3">
                    <input type="number" min="0" value={item.unitPrice} onChange={e => {
                      const newItems = [...items];
                      newItems[idx].unitPrice = Number(e.target.value);
                      setItems(newItems);
                    }} className="w-full border p-1 rounded" />
                  </td>
                  <td className="p-3 font-medium">{(item.quantity * item.unitPrice).toLocaleString()}</td>
                  <td className="p-3 text-center">
                    <button onClick={() => removeItem(idx)} className="text-red-500 hover:text-red-700">
                      <Trash2 size={18} />
                    </button>
                  </td>
                </tr>
              ))}
              {items.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-400">Chưa có sản phẩm nào được thêm</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <SearchModal
        isOpen={showSupplierSearch}
        title="Chọn Nhà cung cấp"
        fetchData={async (q) => suppliers.filter(s => s.name.toLowerCase().includes(q.toLowerCase()))}
        renderItem={(s) => <div className="font-medium text-slate-800">{s.name}</div>}
        onSelect={(s) => { setSupplierId(String(s.id)); setSupplierName(s.name); setShowSupplierSearch(false); }}
        onClose={() => setShowSupplierSearch(false)}
      />
      <SearchModal
        isOpen={showProductSearch}
        title="Chọn Sản phẩm"
        fetchData={async (q) => products.filter(p => p.name.toLowerCase().includes(q.toLowerCase()))}
        renderItem={(p) => <div className="font-medium text-slate-800">{p.name} - {p.price?.toLocaleString()} đ</div>}
        onSelect={addItem}
        onClose={() => setShowProductSearch(false)}
      />
    </div>
  );
};
