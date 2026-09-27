import React, { useState, useEffect } from 'react';
import { FilePlus, ShoppingCart, FileText, RefreshCcw, PackageSearch, Truck, CreditCard, LogOut, Settings, Users, Box, Users2, BarChart2, UserCircle2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { Dashboard } from '../sales/Dashboard';
import { SalesModule } from '../sales/SalesModule';
import { InboundReceiptModule } from '../inventory/InboundReceiptModule';
import { GoodsReturnModule } from '../inventory/GoodsReturnModule';
import { ProductList } from '../catalog/ProductList';
import { CustomerList } from '../catalog/CustomerList';
import { SupplierList } from '../catalog/SupplierList';
import { StockList } from '../inventory/StockList';
import { DebtList } from '../crm/DebtList';
import { BranchList } from '../admin/BranchList';

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

  useEffect(() => {
    if (tabs.length > 0 && !tabs.find((t) => t.id === activeTab)) {
      setActiveTab(tabs[0].id);
    }
  }, [activeTab, tabs]);

  return (
    <div className="flex flex-col w-full shrink-0 bg-surface border-b border-line">
      {/* Ribbon Tầng 1: Tabs ngang */}
      <div className="flex items-end justify-between px-1 pt-1 border-b border-line bg-slate-50">
        <div className="flex gap-0.5">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              data-testid={`ribbon-tab-${tab.id}`}
              onClick={() => setActiveTab(tab.id)}
              className={`px-3 py-1 text-[13px] whitespace-nowrap transition-none border-t-2 rounded-t-sm ${
                activeTab === tab.id
                  ? 'bg-surface text-primary border-t-primary font-semibold z-10 -mb-[1px]'
                  : 'bg-transparent text-ink-muted border-t-transparent hover:bg-slate-100'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
        
        <div className="flex items-center gap-4 px-3 py-1">
          <div data-testid="ribbon-user" className="flex items-center gap-2">
            <UserCircle2 size={18} className="text-ink-subtle" />
            <span className="text-[13px] font-semibold text-ink">{user?.username}</span>
            <span className="px-1.5 py-0.5 text-[11px] font-semibold rounded bg-slate-200 text-ink-muted">
              {user?.role === 'ADMIN' ? 'Quản trị' : 'Nhân viên'}
            </span>
          </div>
          <button data-testid="ribbon-logout" onClick={logout} className="btn-ghost px-2">
            <LogOut size={16} className="text-danger" />
            <span className="text-danger">Đăng xuất</span>
          </button>
        </div>
      </div>

      {/* Ribbon Tầng 2: Toolbar Icons */}
      <div className="flex items-start flex-nowrap overflow-x-auto scrollbar-hide px-2 py-1 gap-1 h-[64px] bg-surface">
        {activeTab === 'ChucNang' && (
          <>
            <button
              onClick={() => handleOpenTab('new-order', 'BÁN HÀNG', <SalesModule initialSubView="FORM" mode="ADD" />, false)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <ShoppingCart size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Bán Hàng</span>
            </button>
            
            <div className="w-[1px] h-[80%] my-auto bg-line mx-1 shrink-0"></div>
            
            {/* TODO: Xuất trả hàng mua — chưa có API */}
            
            <button 
              data-testid="ribbon-btn-return"
              onClick={() => handleOpenTab('goods-return', 'NHẬP LẠI HÀNG BÁN', <GoodsReturnModule />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <PackageSearch size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Nhập Lại Hàng Bán</span>
            </button>
            
            <button 
              data-testid="ribbon-btn-inbound"
              onClick={() => handleOpenTab('new-inbound', 'NHẬP HÀNG', <InboundReceiptModule mode="ADD" />, false)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Truck size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Nhập Hàng</span>
            </button>
          </>
        )}

        {activeTab === 'DanhMuc' && (
          <>
            <button
              onClick={() => handleOpenTab('products', 'SẢN PHẨM', <ProductList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Box size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Sản Phẩm</span>
            </button>
            <button
              onClick={() => handleOpenTab('customers', 'KHÁCH HÀNG', <CustomerList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Users size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Khách Hàng</span>
            </button>
            <button
              onClick={() => handleOpenTab('distributors', 'NHÀ PHÂN PHỐI', <SupplierList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Users2 size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Nhà Phân Phối</span>
            </button>
          </>
        )}
        
        {activeTab === 'TonKho' && (
          <>
            <button
              onClick={() => handleOpenTab('stocks', 'TỒN KHO', <StockList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Box size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Tồn Kho</span>
            </button>
          </>
        )}

        {activeTab === 'CongNo' && (
          <>
            <button
              onClick={() => handleOpenTab('debts', 'CÔNG NỢ', <DebtList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <CreditCard size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Công Nợ</span>
            </button>
          </>
        )}

        {activeTab === 'ThongKe' && (
          <>
            <button
              onClick={() => handleOpenTab('dashboard', 'TỔNG QUAN', <Dashboard />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <BarChart2 size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Dashboard</span>
            </button>
          </>
        )}

        {activeTab === 'HeThong' && (
          <>
            <button
              onClick={() => handleOpenTab('branches', 'CHI NHÁNH', <BranchList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Settings size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Chi Nhánh</span>
            </button>
          </>
        )}
      </div>
    </div>
  );
};


