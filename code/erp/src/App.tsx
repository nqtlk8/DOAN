import { useEffect } from 'react';
import { TopRibbon } from './components/layout/TopRibbon';
import { MainContent } from './components/layout/MainContent';
import { Dashboard } from './components/sales/Dashboard';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Login } from './components/auth/Login';
import { TabProvider, useTabs } from './context/TabContext';
import { X } from 'lucide-react';
import { SalesModule } from './components/sales/SalesModule';

const TabBar = () => {
  const { tabs, activeTabId, setActiveTabId, closeTab } = useTabs();

  if (tabs.length === 0) return null;

  return (
    <div className="flex bg-slate-50 border-b border-slate-200 overflow-x-auto scrollbar-hide">
      {tabs.map(tab => (
        <div
          key={tab.id}
          onClick={() => setActiveTabId(tab.id)}
          className={`flex items-center gap-2 px-3 py-1.5 border-r border-slate-200 cursor-pointer min-w-[150px] max-w-[200px] group transition-colors ${
            activeTabId === tab.id ? 'bg-white text-blue-600 border-b-2 border-b-blue-600 shadow-sm z-10' : 'bg-slate-50 text-slate-600 hover:bg-slate-100'
          }`}
        >
          <span className="truncate flex-1 text-sm font-medium select-none">{tab.title}</span>
          {tab.isClosable !== false && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                closeTab(tab.id);
              }}
              className="p-1 rounded-full hover:bg-red-50 text-slate-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all"
            >
              <X size={14} />
            </button>
          )}
        </div>
      ))}
    </div>
  );
};

const AppContent = () => {
  const { isAuthenticated, user } = useAuth();
  const { tabs, activeTabId, openTab } = useTabs();

  useEffect(() => {
    if (isAuthenticated && user) {
      if (user.role === 'sales') {
        openTab('new-order', 'Tạo Đơn Bán Hàng', <SalesModule initialSubView="FORM" mode="ADD" />, false);
      } else {
        openTab('dashboard', 'Tổng Quan', <Dashboard />, false);
      }
    }
  }, [isAuthenticated, user, openTab]);

  if (!isAuthenticated) {
    return <Login />;
  }

  const activeTab = tabs.find(t => t.id === activeTabId);

  const renderContent = () => {
    if (!activeTab) return null;
    return activeTab.component as React.ReactNode;
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-slate-50">
      <TopRibbon />
      <TabBar />

      <MainContent>
        {renderContent()}
      </MainContent>
    </div>
  );
};

function App() {
  return (
    <AuthProvider>
      <TabProvider>
        <AppContent />
      </TabProvider>
    </AuthProvider>
  );
}

export default App;
