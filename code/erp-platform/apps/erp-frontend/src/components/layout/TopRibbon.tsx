import React, { useState } from 'react';
import { FilePlus, ShoppingCart, FileText, RefreshCcw, PackageSearch, Truck, CreditCard, LogOut, Settings, Users, Box, Users2, BarChart2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { Dashboard } from '../sales/Dashboard';
import { SalesModule } from '../sales/SalesModule';
import { InboundReceiptModule } from '../inventory/InboundReceiptModule';
import { ProductList } from '../catalog/ProductList';
import { CustomerList } from '../catalog/CustomerList';
import { SupplierList } from '../catalog/SupplierList';
import { StockList } from '../inventory/StockList';
import { DebtList } from '../crm/DebtList';
import { BranchList } from '../admin/BranchList';
import { GoodsReturnModule } from '../returns/GoodsReturnModule';

import { allTabs } from '../../config/menuConfig';

export const TopRibbon: React.FC = () => {
  const { user, logout } = useAuth();
  const { openTab } = useTabs();
  const isAdmin = user?.role === 'ADMIN';
  const [activeTab, setActiveTab] = useState('BanHang');

  const handleOpenTab = (id: string, title: string, component: React.ReactNode, isClosable?: boolean) => {
    openTab(id, title, component, isClosable);
  };

  const tabs = React.useMemo(() => allTabs.filter(t => t.roles.includes(user?.role || '')), [user?.role]);

  // Nếu tab hiện tại không được phép, reset về tab đầu tiên được phép
  React.useEffect(() => {
    if (tabs.length > 0 && !tabs.find(t => t.id === activeTab)) {
      setActiveTab(tabs[0].id);
    }
  }, [activeTab, tabs]);

  return (
    <div className="flex flex-col w-full shrink-0 bg-erp-bg-ribbon border-b border-erp-bg-ribbon-border font-erp">
      {/* Ribbon Tầng 1: Tabs ngang */}
      <div className="flex items-end px-1 pt-1 gap-0.5 border-b border-erp-bg-ribbon-border">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            data-testid={`ribbon-tab-${tab.id}`}
            onClick={() => setActiveTab(tab.id)}
            className={`px-3 py-1 text-erp-label whitespace-nowrap border border-b-0 rounded-t-erp transition-none ${
              activeTab === tab.id
                ? 'bg-erp-bg-content border-erp-bg-ribbon-border z-10 -mb-[1px]'
                : 'bg-transparent border-transparent hover:bg-white/40 text-slate-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Ribbon Tầng 2: Toolbar Icons */}
      <div className="flex items-start flex-nowrap overflow-x-auto scrollbar-hide px-2 py-1 gap-1 h-[60px] bg-erp-bg-content">
        {activeTab === 'ChucNang' && (
          <>
            <button
              onClick={() => handleOpenTab('new-order', 'BÁN HÀNG', <SalesModule initialSubView="FORM" mode="ADD" />, false)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <ShoppingCart size={20} className="text-blue-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Bán Hàng</span>
            </button>
            
            <div className="w-[1px] h-[80%] my-auto bg-erp-bg-ribbon-border mx-1 shrink-0"></div>
            
            <button className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1">
              <RefreshCcw size={20} className="text-red-500" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Xuất Trả Hàng Mua</span>
            </button>
            
            <button 
              onClick={() => handleOpenTab('goods-return', 'NHẬP LẠI HÀNG BÁN', <GoodsReturnModule />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <PackageSearch size={20} className="text-green-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Nhập Lại Hàng Bán</span>
            </button>
            
            <button 
              data-testid="ribbon-btn-inbound"
              onClick={() => handleOpenTab('new-inbound', 'NHẬP HÀNG', <InboundReceiptModule mode="ADD" />, false)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <Truck size={20} className="text-orange-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Nhập Hàng</span>
            </button>
          </>
        )}

        {activeTab === 'DanhMuc' && (
          <>
            <button
              onClick={() => handleOpenTab('products', 'SẢN PHẨM', <ProductList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <Box size={20} className="text-blue-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Sản Phẩm</span>
            </button>
            <button
              onClick={() => handleOpenTab('customers', 'KHÁCH HÀNG', <CustomerList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <Users size={20} className="text-green-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Khách Hàng</span>
            </button>
            <button
              onClick={() => handleOpenTab('distributors', 'NHÀ PHÂN PHỐI', <SupplierList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <Users2 size={20} className="text-orange-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Nhà Phân Phối</span>
            </button>
          </>
        )}
        
        {activeTab === 'TonKho' && (
          <>
            <button
              onClick={() => handleOpenTab('stocks', 'TỒN KHO', <StockList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <Box size={20} className="text-blue-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Tồn Kho</span>
            </button>
          </>
        )}

        {activeTab === 'CongNo' && (
          <>
            <button
              onClick={() => handleOpenTab('debts', 'CÔNG NỢ', <DebtList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <CreditCard size={20} className="text-red-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Công Nợ</span>
            </button>
          </>
        )}

        {activeTab === 'ThongKe' && (
          <>
            <button
              onClick={() => handleOpenTab('dashboard', 'TỔNG QUAN', <Dashboard />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <BarChart2 size={20} className="text-blue-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Dashboard</span>
            </button>
          </>
        )}

        {activeTab === 'HeThong' && (
          <>
            <button
              onClick={() => handleOpenTab('branches', 'CHI NHÁNH', <BranchList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <Settings size={20} className="text-slate-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Chi Nhánh</span>
            </button>
            <div className="w-[1px] h-[80%] my-auto bg-erp-bg-ribbon-border mx-1 shrink-0"></div>
            <button
              onClick={logout}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 border border-transparent hover:border-erp-btn-border hover:bg-erp-btn-hover-bg rounded-erp gap-1"
            >
              <LogOut size={20} className="text-red-600" />
              <span className="text-erp-label whitespace-nowrap leading-none text-slate-800">Đăng Xuất</span>
            </button>
          </>
        )}
      </div>
    </div>
  );
};
