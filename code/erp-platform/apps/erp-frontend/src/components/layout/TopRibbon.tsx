import React, { useState } from 'react';
import { FilePlus, ShoppingCart, FileText, RefreshCcw, PackageSearch, Truck, CreditCard, LogOut, Settings } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { Dashboard } from '../sales/Dashboard';
import { SalesModule } from '../sales/SalesModule';
import { PurchaseModule } from '../purchasing/PurchaseModule';
import { ProductList } from '../catalog/ProductList';
import { CustomerList } from '../catalog/CustomerList';
import { DistributorList } from '../catalog/DistributorList';
import { StockList } from '../inventory/StockList';
import { DebtList } from '../crm/DebtList';
import { BranchList } from '../admin/BranchList';
import { GoodsReturnModule } from '../returns/GoodsReturnModule';
import { Users, Box, Users2, BarChart2 } from 'lucide-react';

export const TopRibbon: React.FC = () => {
  const { user, logout } = useAuth();
  const { openTab } = useTabs();
  const isAdmin = user?.role === 'admin';
  const [activeTab, setActiveTab] = useState('BanHang');

  const handleOpenTab = (id: string, title: string, component: React.ReactNode, isClosable?: boolean) => {
    openTab(id, title, component, isClosable);
  };

  const allTabs = [
    { id: 'ChucNang', label: 'Chức năng', roles: ['sales'] },
    { id: 'DanhMuc', label: 'Danh mục', roles: ['admin', 'sales'] },
    { id: 'CongNo', label: 'Công nợ', roles: ['admin', 'sales'] },
    { id: 'TonKho', label: 'Tồn kho', roles: ['admin', 'sales'] },
    { id: 'ThongKe', label: 'Thống kê', roles: ['admin'] },
    { id: 'HeThong', label: 'Hệ thống', roles: ['admin', 'sales'] },
  ];

  const tabs = allTabs.filter(t => t.roles.includes(user?.role || ''));

  // Nếu tab hiện tại không được phép, reset về tab đầu tiên được phép
  React.useEffect(() => {
    if (tabs.length > 0 && !tabs.find(t => t.id === activeTab)) {
      setActiveTab(tabs[0].id);
    }
  }, [user?.role, activeTab, tabs]);

  return (
    <div className="flex flex-col w-full shrink-0">
      {/* Ribbon Tầng 1: Tabs ngang */}
      <div className="flex items-end bg-[#e2e8f0] pt-1 px-1 border-b border-[#94a3b8]">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-1 text-[12px] font-medium border border-b-0 rounded-t-sm ${
              activeTab === tab.id
                ? 'bg-[#f8fafc] border-[#94a3b8] z-10 -mb-[1px]'
                : 'bg-transparent border-transparent hover:bg-[#cbd5e1] text-slate-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Ribbon Tầng 2: Toolbar Icons */}
      <div className="bg-[#f8fafc] border-b border-[#94a3b8] px-4 py-2 flex items-start gap-6 h-[72px]">
        {activeTab === 'ChucNang' && (
          <>
            <button
              onClick={() => handleOpenTab('new-order', 'BÁN HÀNG', <SalesModule initialSubView="FORM" mode="ADD" />, false)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <ShoppingCart size={28} className="text-blue-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Bán Hàng</span>
            </button>
            <div className="w-[1px] h-full bg-[#cbd5e1] mx-1"></div>
            <button className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]">
              <RefreshCcw size={28} className="text-red-500" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Xuất Trả<br />Hàng Mua</span>
            </button>
            <button 
              onClick={() => handleOpenTab('goods-return', 'NHẬP LẠI HÀNG BÁN', <GoodsReturnModule />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <PackageSearch size={28} className="text-green-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Nhập Lại<br />Hàng Bán</span>
            </button>
            <button 
              onClick={() => handleOpenTab('new-purchase', 'NHẬP HÀNG', <PurchaseModule initialSubView="FORM" mode="ADD" />, false)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <Truck size={28} className="text-orange-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Nhập Hàng</span>
            </button>
          </>
        )}

        {activeTab === 'DanhMuc' && (
          <>
            <button
              onClick={() => handleOpenTab('products', 'SẢN PHẨM', <ProductList />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <Box size={28} className="text-blue-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Sản Phẩm</span>
            </button>
            <button
              onClick={() => handleOpenTab('customers', 'KHÁCH HÀNG', <CustomerList />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <Users size={28} className="text-green-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Khách Hàng</span>
            </button>
            <button
              onClick={() => handleOpenTab('distributors', 'NHÀ PHÂN PHỐI', <DistributorList />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <Users2 size={28} className="text-orange-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Nhà Phân Phối</span>
            </button>
          </>
        )}
        
        {activeTab === 'TonKho' && (
          <>
            <button
              onClick={() => handleOpenTab('stocks', 'TỒN KHO', <StockList />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <Box size={28} className="text-blue-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Tồn Kho</span>
            </button>
          </>
        )}

        {activeTab === 'CongNo' && (
          <>
            <button
              onClick={() => handleOpenTab('debts', 'CÔNG NỢ', <DebtList />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <CreditCard size={28} className="text-red-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Công Nợ</span>
            </button>
          </>
        )}

        {activeTab === 'ThongKe' && (
          <>
            <button
              onClick={() => handleOpenTab('dashboard', 'TỔNG QUAN', <Dashboard />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <BarChart2 size={28} className="text-blue-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Dashboard</span>
            </button>
          </>
        )}

        {activeTab === 'HeThong' && (
          <>
            <button
              onClick={() => handleOpenTab('branches', 'CHI NHÁNH', <BranchList />)}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <Settings size={28} className="text-slate-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Chi Nhánh</span>
            </button>
            <div className="w-[1px] h-full bg-[#cbd5e1] mx-1"></div>
            <button
              onClick={logout}
              className="flex flex-col items-center gap-1 hover:bg-[#e2e8f0] p-1 rounded-sm border border-transparent hover:border-[#94a3b8] min-w-[64px]"
            >
              <LogOut size={28} className="text-red-600" />
              <span className="text-[11px] text-slate-800 text-center leading-tight">Đăng Xuất</span>
            </button>
          </>
        )}
      </div>
    </div>
  );
};
