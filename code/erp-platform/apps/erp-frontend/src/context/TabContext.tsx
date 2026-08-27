import React, { createContext, useContext, useState, type ReactNode, useCallback } from 'react';

export interface TabData {
  id: string;
  title: string;
  component: ReactNode | string;
  isClosable?: boolean;
}

interface TabContextType {
  tabs: TabData[];
  activeTabId: string | null;
  openTab: (id: string, title: string, component: ReactNode | string, isClosable?: boolean) => void;
  closeTab: (id: string) => void;
  setActiveTabId: (id: string) => void;
}

const TabContext = createContext<TabContextType | undefined>(undefined);

export const TabProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [tabs, setTabs] = useState<TabData[]>([]);
  const [activeTabId, setActiveTabIdState] = useState<string | null>(null);

  const setActiveTabId = useCallback((id: string) => {
    setActiveTabIdState(id);
  }, []);

  const openTab = useCallback(
    (id: string, title: string, component: ReactNode | string, isClosable: boolean = true) => {
      setTabs((prevTabs) => {
        const existingTab = prevTabs.find((t) => t.id === id);
        if (existingTab) {
          return prevTabs;
        }
        return [...prevTabs, { id, title, component, isClosable }];
      });
      setActiveTabIdState(id);
    },
    [],
  );

  const closeTab = useCallback((id: string) => {
    setTabs((prevTabs) => {
      const filteredTabs = prevTabs.filter((t) => t.id !== id);
      if (filteredTabs.length > 0) {
        // If the closed tab was active, switch to the last tab in the list
        setActiveTabIdState((currentActive) =>
          currentActive === id ? filteredTabs[filteredTabs.length - 1].id : currentActive,
        );
      } else {
        setActiveTabIdState(null);
      }
      return filteredTabs;
    });
  }, []);

  return (
    <TabContext.Provider value={{ tabs, activeTabId, openTab, closeTab, setActiveTabId }}>
      {children}
    </TabContext.Provider>
  );
};

export const useTabs = () => {
  const context = useContext(TabContext);
  if (context === undefined) {
    throw new Error('useTabs must be used within a TabProvider');
  }
  return context;
};
