import React from 'react';
import { PackageOpen } from 'lucide-react';

interface EmptyStateProps {
  title?: string;
  message?: string;
  action?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({ 
  title = 'Chưa có dữ liệu', 
  message = 'Không tìm thấy bản ghi nào trong hệ thống.',
  action 
}) => {
  return (
    <div className="w-full flex flex-col items-center justify-center p-12 text-ink-subtle bg-app rounded-md border border-line border-dashed">
      <PackageOpen size={32} className="text-ink-subtle mb-3" />
      <h3 className="text-[14px] font-medium text-ink mb-1">{title}</h3>
      <p className="text-[13px] text-ink-muted mb-4 text-center max-w-md">
        {message}
      </p>
      
      {action && (
        <div className="mt-2">
          {action}
        </div>
      )}
    </div>
  );
};
