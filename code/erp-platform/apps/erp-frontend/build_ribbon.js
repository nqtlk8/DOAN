const fs = require('fs');

const ribbonContent = `import React, { useState } from 'react';
import {
  FilePlus,
  ShoppingCart,
  FileText,
  RefreshCcw,
  PackageSearch,
  Truck,
  CreditCard,
  LogOut
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTabs } from '../../context/TabContext';
import { Dashboard } from '../sales/Dashboard';
import { SalesModule } from '../sales/SalesModule';

export const TopRibbon: React.FC = () => {
  const { user, logout } = useAuth();
  const { openTab } = useTabs();
  const isAdmin = user?.role === 'admin';
  const [activeTab, setActiveTab] = useState('BanHang');

  const handleOpenTab = (id: string, title: string, component: React.ReactNode, isClosable?: boolean) => {
    openTab(id, title, component, isClosable);
  };

  const tabs = [
    { id: 'ChucNang', label: 'Chức năng' },
    { id: 'DanhMuc', label: 'Danh mục' },
    { id: 'CongNo', label: 'Công nợ' },
    { id: 'TonKho', label: 'Tồn kho' },
    { id: 'ThongKe', label: 'Thống kê' },
    { id: 'HeThong', label: 'Hệ thống' },
  ];

  return (
    <div className="flex flex-col w-full shrink-0">
      {/* Ribbon Tầng 1: Tabs ngang */}
      <div className="flex items-end bg-[#E3E9F0] pt-1 px-1 border-b border-[#a0a0a0]">
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={\`px-4 py-1 text-[12px] font-medium border border-b-0 rounded-t-sm \${
              activeTab === tab.id 
                ? 'bg-[#F3F2EE] border-[#a0a0a0] z-10 -mb-[1px]' 
                : 'bg-transparent border-transparent hover:bg-[#d0d0d0] text-slate-700'
            }\`}
          >
            {tab.label}
          </button>
        ))}
      </div>
      
      {/* Ribbon Tầng 2: Toolbar Icons */}
      <div className="bg-[#F3F2EE] border-b border-[#a0a0a0] px-4 py-2 flex items-start gap-6 h-[72px]">
        <button 
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px]"
        >
          <FilePlus size={28} className="text-orange-500" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Phiếu Thu</span>
        </button>
        
        <button 
          onClick={() => handleOpenTab('new-order', 'BÁN HÀNG', <SalesModule initialSubView="FORM" mode="ADD" />, false)}
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px]"
        >
          <ShoppingCart size={28} className="text-blue-600" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Bán Hàng</span>
        </button>

        <button 
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px]"
        >
          <FileText size={28} className="text-blue-500" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Bán Lẻ</span>
        </button>
        
        <div className="w-[1px] h-full bg-[#ccc] mx-1"></div>

        <button 
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px]"
        >
          <RefreshCcw size={28} className="text-red-500" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Xuất Trả<br/>Hàng Mua</span>
        </button>
        
        <button 
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px]"
        >
          <PackageSearch size={28} className="text-green-600" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Nhập Lại<br/>Hàng Bán</span>
        </button>
        
        <button 
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px]"
        >
          <Truck size={28} className="text-orange-600" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Mua Hàng</span>
        </button>
        
        <div className="w-[1px] h-full bg-[#ccc] mx-1"></div>

        <button 
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px]"
        >
          <CreditCard size={28} className="text-purple-600" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Phiếu Chi</span>
        </button>
        
        <button 
          onClick={logout}
          className="flex flex-col items-center gap-1 hover:bg-[#E3E9F0] p-1 rounded-sm border border-transparent hover:border-[#a0a0a0] min-w-[64px] ml-auto"
        >
          <LogOut size={28} className="text-red-600" />
          <span className="text-[11px] text-slate-800 text-center leading-tight">Thoát Hệ Thống</span>
        </button>
      </div>
    </div>
  );
};
`
fs.writeFileSync('src/components/layout/TopRibbon.tsx', ribbonContent, 'utf8');
console.log('Successfully wrote TopRibbon.tsx');
