import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { ApiError } from '../../errors/ApiError';

interface ErrorStateProps {
  error: Error | ApiError | null;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({ error, onRetry }) => {
  const errorMessage = error instanceof ApiError 
    ? error.message 
    : error?.message || 'Đã xảy ra lỗi khi tải dữ liệu.';

  return (
    <div className="w-full flex flex-col items-center justify-center p-12 text-slate-500 bg-red-50/30 rounded-lg border border-red-100">
      <AlertCircle className="w-10 h-10 text-red-400 mb-4" />
      <h3 className="text-lg font-medium text-slate-800 mb-2">Không thể tải dữ liệu</h3>
      <p className="text-sm text-slate-500 mb-6 text-center max-w-md">
        {errorMessage}
      </p>
      
      {onRetry && (
        <button
          onClick={onRetry}
          className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-300 rounded-md text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors shadow-sm"
        >
          <RefreshCw size={16} />
          Thử lại
        </button>
      )}
    </div>
  );
};
