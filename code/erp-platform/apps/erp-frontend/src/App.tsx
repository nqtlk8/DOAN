import { useEffect } from 'react';
import { TopRibbon } from './components/layout/TopRibbon';
import { MainContent } from './components/layout/MainContent';
import { Dashboard } from './components/sales/Dashboard';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Login } from './components/auth/Login';
import { TabProvider, useTabs } from './context/TabContext';
import { X } from 'lucide-react';
import { SalesModule } from './components/sales/SalesModule';
import { ErrorBoundary } from './components/common/ErrorBoundary';

const TabBar = () => {
  const { tabs, activeTabId, setActiveTabId, closeTab } = useTabs();

  if (tabs.length === 0) return null;

  return (
    <div className="flex bg-surface border-b border-line overflow-x-auto h-[34px] shrink-0">
      {tabs.map((tab) => {
        const isActive = activeTabId === tab.id;
        return (
          <div
            key={tab.id}
            data-testid={`tab-${tab.id}`}
            onClick={() => setActiveTabId(tab.id)}
            onAuxClick={(e) => {
              if (e.button === 1 && tab.isClosable !== false) {
                closeTab(tab.id);
              }
            }}
            className={`flex items-center gap-2 px-3 border-r border-line cursor-pointer min-w-[140px] max-w-[220px] group transition-colors text-[13px] relative ${
              isActive
                ? 'bg-surface text-primary font-semibold'
                : 'bg-surface text-ink-muted hover:bg-slate-50'
            }`}
          >
            <span className="truncate flex-1 select-none leading-[34px]">{tab.title}</span>
            {isActive && <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary"></div>}
            {tab.isClosable !== false && (
              <button
                aria-label="Đóng tab"
                onClick={(e) => {
                  e.stopPropagation();
                  closeTab(tab.id);
                }}
                className={`p-0.5 rounded-sm hover:bg-slate-200 transition-colors z-10 ${
                  isActive ? 'opacity-100 text-ink-subtle' : 'opacity-0 group-hover:opacity-100 text-ink-subtle'
                }`}
              >
                <X size={14} />
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
};

const AppContent = () => {
  const { isAuthenticated, user } = useAuth();
  const { tabs, activeTabId, openTab } = useTabs();

  useEffect(() => {
    if (isAuthenticated && user) {
      if (user.role === 'STAFF') {
        openTab('new-order', 'Tạo Đơn Bán Hàng', <SalesModule initialSubView="FORM" mode="ADD" />, false);
      } else if (user.role === 'ADMIN') {
        openTab('dashboard', 'Tổng Quan', <Dashboard />, false);
      }
    }
  }, [isAuthenticated, user, openTab]);

  if (!isAuthenticated) {
    return <Login />;
  }

  const activeTab = tabs.find((t) => t.id === activeTabId);

  const renderContent = () => {
    if (!activeTab) return null;
    return activeTab.component as React.ReactNode;
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-slate-50">
      <TopRibbon />
      <TabBar />

      <MainContent>{renderContent()}</MainContent>
    </div>
  );
};

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <TabProvider>
          <AppContent />
        </TabProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;
