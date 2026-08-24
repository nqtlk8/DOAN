import React, { useState, useEffect } from 'react';
import { Search, Filter, MoreVertical, Eye, FileText } from 'lucide-react';
import { SalesService } from '../../services/salesService';
import type { OrderListItem } from '../../types/sales';

interface SalesListProps {
  setActiveTab?: (tab: string) => void;
  onRowDoubleClick?: (orderId: string) => void;
}

export const SalesList: React.FC<SalesListProps> = ({ onRowDoubleClick }) => {
  const [orders, setOrders] = useState<OrderListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Mock fetching data to avoid crash if backend is not ready
    const fetchOrders = async () => {
      setIsLoading(true);
      try {
        const items = await SalesService.getOrders('2023-01-01', '2023-12-31', 0, 10);
        setOrders(items);
      } catch (error) {
        console.warn('Backend not ready, using mock data for SalesList');
        // Mock data
        setOrders([
          { orderId: '1', code: 'ORD-1023', customerId: 'C1', customerName: 'Tech Corp', totalAmount: 4500.00, createdAt: '2026-07-08T10:00:00Z', status: 'CONFIRMED' },
          { orderId: '2', code: 'ORD-1024', customerId: 'C2', customerName: 'Global Logistics', totalAmount: 1250.50, createdAt: '2026-07-08T11:30:00Z', status: 'PENDING' },
          { orderId: '3', code: 'ORD-1025', customerId: 'C3', customerName: 'Sunrise Cafe', totalAmount: 340.00, createdAt: '2026-07-07T14:15:00Z', status: 'DELIVERED' },
          { orderId: '4', code: 'ORD-1026', customerId: 'C4', customerName: 'Alpha Systems', totalAmount: 12400.00, createdAt: '2026-07-06T09:45:00Z', status: 'CANCELLED' },
        ]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchOrders();
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED': return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'DELIVERED': return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'PENDING': return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'CANCELLED': return 'bg-red-50 text-red-700 border-red-200';
      default: return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  const handleEdit = (order: OrderListItem) => {
    if (onRowDoubleClick) {
      onRowDoubleClick(order.orderId);
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-6 py-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-800">Sales Orders</h2>
        
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input 
              type="text" 
              placeholder="Search orders..." 
              className="pl-9 pr-4 py-2 text-sm bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/50 w-full sm:w-64"
            />
          </div>
          <button className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors">
            <Filter size={16} /> Filter
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50/50 border-b border-slate-100">
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Code</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Customer</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Date</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Total</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Status</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-500">Loading orders...</td>
              </tr>
            ) : orders.length > 0 ? (
              orders.map((order) => (
                <tr 
                  key={order.orderId} 
                  className="hover:bg-slate-50/50 transition-colors group cursor-pointer"
                  onDoubleClick={() => { if (onRowDoubleClick) onRowDoubleClick(order.orderId); }}
                >
                  <td className="px-6 py-4 font-medium text-slate-900">{order.code || order.orderId}</td>
                  <td className="px-6 py-4 text-slate-600">{order.customerName || order.customerId}</td>
                  <td className="px-6 py-4 text-slate-500">
                    {new Date(order.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 font-medium text-slate-900">
                    ${order.totalAmount.toFixed(2)}
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium border ${getStatusColor(order.status)}`}>
                      {order.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button 
                        onClick={() => handleEdit(order)}
                        className="flex items-center gap-1 p-1.5 text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg text-sm font-medium transition-colors" 
                        title="Chi Tiết / Sửa"
                      >
                        <Eye size={16} /> <span className="hidden sm:inline">Chi Tiết / Sửa</span>
                      </button>
                      <button className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg" title="Delivery Note">
                        <FileText size={18} />
                      </button>
                      <button className="p-1.5 text-slate-400 hover:text-slate-900 hover:bg-slate-100 rounded-lg">
                        <MoreVertical size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-slate-500">No orders found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      
      <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/30">
        <span className="text-sm text-slate-500">Showing 1 to 10 of 45 results</span>
        <div className="flex gap-1">
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50">Prev</button>
          <button className="px-3 py-1 bg-blue-600 text-white rounded-md text-sm font-medium">1</button>
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50">2</button>
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50">3</button>
          <button className="px-3 py-1 border border-slate-200 rounded-md text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-50">Next</button>
        </div>
      </div>
    </div>
  );
};
