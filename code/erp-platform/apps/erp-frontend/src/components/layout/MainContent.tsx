import React, { type ReactNode } from 'react';

interface MainContentProps {
  children: ReactNode;
}

export const MainContent: React.FC<MainContentProps> = ({ children }) => {
  return (
    <main className="flex-1 min-h-0 overflow-hidden bg-app">
      <div className="h-full">{children}</div>
    </main>
  );
};
