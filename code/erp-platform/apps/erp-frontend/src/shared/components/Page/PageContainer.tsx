import React, { type ReactNode } from 'react';

interface PageContainerProps {
  children: ReactNode;
  className?: string;
}

export const PageContainer: React.FC<PageContainerProps> = ({ children, className = '' }) => {
  return (
    <div className={`h-full overflow-auto ${className}`}>
      <div className="mx-auto max-w-[1440px] space-y-4 p-5">{children}</div>
    </div>
  );
};
