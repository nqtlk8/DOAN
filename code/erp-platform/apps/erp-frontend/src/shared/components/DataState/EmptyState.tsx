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
    <div className="w-full flex flex-col items-center justify-center p-12 text-slate-500 bg-slate-50/50 rounded-lg border border-slate-100 border-dashed">
      <PackageOpen className="w-12 h-12 text-slate-300 mb-4" />
      <h3 className="text-base font-medium text-slate-700 mb-1">{title}</h3>
      <p className="text-sm text-slate-500 mb-6 text-center">
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
