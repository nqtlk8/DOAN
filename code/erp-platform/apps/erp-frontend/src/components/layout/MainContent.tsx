import React, { type ReactNode } from 'react';

interface MainContentProps {
  children: ReactNode;
}

export const MainContent: React.FC<MainContentProps> = ({ children }) => {
  return (
    <main className="flex-1 p-6 overflow-auto">
      <div className="max-w-7xl mx-auto space-y-6">{children}</div>
    </main>
  );
};
