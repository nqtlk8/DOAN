import re

with open('src/components/layout/TopRibbon.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Add UserCircle2 to lucide imports
content = re.sub(r'import\s+\{\s*([^}]+)\s*\}\s+from\s+[\'"]lucide-react[\'"]', 
                 lambda m: "import { " + m.group(1) + ", UserCircle2 } from 'lucide-react'", 
                 content)

# Replace the two layers
replacement = '''
  return (
    <div className="flex flex-col w-full shrink-0 bg-surface border-b border-line">
      {/* Ribbon T?ng 1: Tabs ngang */}
      <div className="flex items-end justify-between px-1 pt-1 border-b border-line bg-slate-50">
        <div className="flex gap-0.5">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              data-testid={ibbon-tab-}
              onClick={() => setActiveTab(tab.id)}
              className={px-3 py-1 text-[13px] whitespace-nowrap transition-none border-t-2 }
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
              {user?.role === 'ADMIN' ? 'Qu?n tr?' : 'Nhân viên'}
            </span>
          </div>
          <button data-testid="ribbon-logout" onClick={logout} className="btn-ghost px-2">
            <LogOut size={16} />
            Ðang xu?t
          </button>
        </div>
      </div>

      {/* Ribbon T?ng 2: Toolbar Icons */}
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
            
            {/* TODO: Xu?t tr? hàng mua — chua có API */}
            
            <button 
              data-testid="ribbon-btn-return"
              onClick={() => handleOpenTab('goods-return', 'NH?P L?I HÀNG BÁN', <GoodsReturnModule />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <PackageSearch size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Nh?p L?i Hàng Bán</span>
            </button>
            
            <button 
              data-testid="ribbon-btn-inbound"
              onClick={() => handleOpenTab('new-inbound', 'NH?P HÀNG', <InboundReceiptModule mode="ADD" />, false)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Truck size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Nh?p Hàng</span>
            </button>
          </>
        )}

        {activeTab === 'DanhMuc' && (
          <>
            <button
              onClick={() => handleOpenTab('products', 'S?N PH?M', <ProductList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Box size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">S?n Ph?m</span>
            </button>
            <button
              onClick={() => handleOpenTab('customers', 'KHÁCH HÀNG', <CustomerList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Users size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Khách Hàng</span>
            </button>
            <button
              onClick={() => handleOpenTab('distributors', 'NHÀ PHÂN PH?I', <SupplierList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Users2 size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Nhà Phân Ph?i</span>
            </button>
          </>
        )}
        
        {activeTab === 'TonKho' && (
          <>
            <button
              onClick={() => handleOpenTab('stocks', 'T?N KHO', <StockList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Box size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">T?n Kho</span>
            </button>
          </>
        )}

        {activeTab === 'CongNo' && (
          <>
            <button
              onClick={() => handleOpenTab('debts', 'CÔNG N?', <DebtList />)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <CreditCard size={20} className="text-primary" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Công N?</span>
            </button>
          </>
        )}

        {activeTab === 'ThongKe' && (
          <>
            <button
              onClick={() => handleOpenTab('dashboard', 'T?NG QUAN', <Dashboard />)}
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
'''

content = re.sub(r'return\s*\(\s*<div className="flex flex-col w-full shrink-0.*', replacement, content, flags=re.DOTALL)

with open('src/components/layout/TopRibbon.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
