import React from 'react';
import { LayoutDashboard, ShoppingCart, Package, FolderOpen, Wallet, BarChart3, Bell, Settings, User, ChevronDown, LogOut } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { Dashboard } from '../sales/Dashboard';
import { SalesModule } from '../sales/SalesModule';
import { PurchaseModule } from '../purchasing/PurchaseModule';
import { ProductList } from '../catalog/ProductList';
import { CustomerList } from '../catalog/CustomerList';
import { DistributorList } from '../catalog/DistributorList';

export const TopRibbon: React.FC = () => {
  const { user, logout } = useAuth();
  const { openTab } = useTabs();
  const isAdmin = user?.role === 'admin';
  const isSales = user?.role === 'sales';

  const handleOpenTab = (id: string, title: string, component: React.ReactNode, isClosable?: boolean) => {
    openTab(id, title, component, isClosable);
  };

  return (
    <header className="bg-white/80 backdrop-blur-md border-b border-slate-200 sticky top-0 z-50 shadow-sm">
      <div className="flex items-center justify-between px-6 h-16">
        
        {/* Brand */}
        <div className="flex items-center gap-3 mr-8 cursor-pointer" onClick={() => isAdmin ? handleOpenTab('dashboard', 'Tổng Quan', <Dashboard />, false) : handleOpenTab('new-order', 'Tạo Đơn Bán Hàng', <SalesModule initialSubView="FORM" mode="ADD" />, false)}>
          <div className="w-9 h-9 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-200">
            <span className="text-white font-bold text-sm tracking-wider">ERP</span>
          </div>
          <span className="text-xl font-bold bg-gradient-to-r from-blue-700 to-indigo-600 bg-clip-text text-transparent hidden sm:block">
            StoreERP
          </span>
        </div>

        {/* Navigation Menu */}
        <nav className="flex-1 flex items-center space-x-2">
          
          {/* Tổng Quan - Chỉ Admin */}
          {!isSales && (
            <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab('dashboard', 'Tổng Quan', <Dashboard />); }} className="flex items-center gap-2 px-4 py-2 text-blue-600 bg-blue-50 rounded-lg font-medium transition-colors">
              <LayoutDashboard size={18} />
              <span>Tổng Quan</span>
            </a>
          )}

          {/* Bán Hàng (Dropdown) */}
          <div className="relative group">
            <button className="flex items-center gap-2 px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg font-medium transition-colors">
              <ShoppingCart size={18} />
              <span>Bán Hàng</span>
              <ChevronDown size={14} className="group-hover:rotate-180 transition-transform duration-200" />
            </button>
            <div className="absolute top-full left-0 mt-1 w-56 bg-white border border-slate-100 rounded-xl shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform origin-top-left z-50">
              <div className="p-2 flex flex-col">
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab(`quote-${Date.now()}`, 'Tạo Báo Giá', <SalesModule initialSubView="FORM" mode="ADD" />); }} className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Tạo Báo Giá / Đặt Trước</a>
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab(`new-order-${Date.now()}`, 'Tạo Đơn Bán Hàng', <SalesModule initialSubView="FORM" mode="ADD" />); }} className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Tạo Đơn Bán Hàng</a>
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab(`return-order-${Date.now()}`, 'Khách Trả Hàng', <SalesModule initialSubView="FORM" mode="ADD" />); }} className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Khách Trả Hàng</a>
              </div>
            </div>
          </div>

          {/* Mua & Kho (Dropdown) */}
          <div className="relative group">
            <button className="flex items-center gap-2 px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg font-medium transition-colors">
              <Package size={18} />
              <span>Mua & Kho</span>
              <ChevronDown size={14} className="group-hover:rotate-180 transition-transform duration-200" />
            </button>
            <div className="absolute top-full left-0 mt-1 w-56 bg-white border border-slate-100 rounded-xl shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
              <div className="p-2 flex flex-col">
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab(`purchase-order-${Date.now()}`, 'Phiếu Nhập Hàng (PO)', <PurchaseModule initialSubView="LIST" />); }} className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Phiếu Nhập Hàng (PO)</a>
                <a href="#" className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Xem Tồn Kho</a>
              </div>
            </div>
          </div>

          {/* Danh Mục (Dropdown) */}
          <div className="relative group hidden lg:block">
            <button className="flex items-center gap-2 px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg font-medium transition-colors">
              <FolderOpen size={18} />
              <span>Danh Mục</span>
              <ChevronDown size={14} className="group-hover:rotate-180 transition-transform duration-200" />
            </button>
            <div className="absolute top-full left-0 mt-1 w-48 bg-white border border-slate-100 rounded-xl shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
              <div className="p-2 flex flex-col">
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab('catalog-products', 'Sản Phẩm', <ProductList />); }} className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Sản Phẩm</a>
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab('catalog-customers', 'Khách Hàng', <CustomerList />); }} className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Khách Hàng</a>
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpenTab('catalog-distributors', 'Nhà Phân Phối', <DistributorList />); }} className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Nhà Phân Phối</a>
              </div>
            </div>
          </div>

          {/* Sổ Quỹ (Dropdown) - Chỉ Admin */}
          {isAdmin && (
            <div className="relative group hidden xl:block">
              <button className="flex items-center gap-2 px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg font-medium transition-colors">
                <Wallet size={18} />
                <span>Sổ Quỹ</span>
                <ChevronDown size={14} className="group-hover:rotate-180 transition-transform duration-200" />
              </button>
              <div className="absolute top-full left-0 mt-1 w-48 bg-white border border-slate-100 rounded-xl shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
                <div className="p-2 flex flex-col">
                  <a href="#" className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Phiếu Thu</a>
                  <a href="#" className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Phiếu Chi</a>
                </div>
              </div>
            </div>
          )}

          {/* Thống Kê (Dropdown) - Chỉ Admin */}
          {isAdmin && (
            <div className="relative group hidden xl:block">
              <button className="flex items-center gap-2 px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg font-medium transition-colors">
                <BarChart3 size={18} />
                <span>Thống Kê</span>
                <ChevronDown size={14} className="group-hover:rotate-180 transition-transform duration-200" />
              </button>
              <div className="absolute top-full left-0 mt-1 w-48 bg-white border border-slate-100 rounded-xl shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
                <div className="p-2 flex flex-col">
                  <a href="#" className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Theo Khách Hàng</a>
                  <a href="#" className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Theo Sản Phẩm</a>
                  <a href="#" className="px-4 py-2 text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-600 rounded-lg transition-colors">Theo Nhà Phân Phối</a>
                </div>
              </div>
            </div>
          )}

        </nav>

        {/* Actions & Profile */}
        <div className="flex items-center gap-2 pl-4 border-l border-slate-200">
          <button className="p-2 text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-full transition-colors relative">
            <Bell size={20} />
            <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 border-2 border-white rounded-full"></span>
          </button>
          <button className="p-2 text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-full transition-colors">
            <Settings size={20} />
          </button>
          
          <div className="relative group">
            <button className="flex items-center gap-3 p-1.5 ml-2 hover:bg-slate-100 rounded-full transition-colors">
              <span className="text-sm font-medium text-slate-700 hidden md:block">
                {user?.username} ({user?.role})
              </span>
              <div className="w-8 h-8 rounded-full bg-gradient-to-r from-blue-500 to-indigo-600 flex items-center justify-center shadow-sm">
                <User size={16} className="text-white" />
              </div>
            </button>
            <div className="absolute top-full right-0 mt-1 w-48 bg-white border border-slate-100 rounded-xl shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
              <div className="p-2 flex flex-col">
                <button
                  onClick={logout}
                  className="flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-lg transition-colors w-full text-left"
                >
                  <LogOut size={16} />
                  <span>Đăng xuất</span>
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </header>
  );
};
