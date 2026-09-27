import React, { type ReactNode } from 'react';

interface PageContainerProps extends React.HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  className?: string;
}

export const PageContainer: React.FC<PageContainerProps> = ({ children, className = '', ...rest }) => {
  return (
    <div className={`h-full overflow-auto ${className}`} {...rest}>
      <div className="mx-auto max-w-[1440px] space-y-4 p-5">{children}</div>
    </div>
  );
};
