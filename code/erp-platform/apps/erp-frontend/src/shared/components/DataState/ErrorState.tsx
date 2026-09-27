import React from 'react';
import { AlertTriangle, RefreshCcw } from 'lucide-react';
import { ApiError } from '../../errors/ApiError';

interface ErrorStateProps {
  error?: Error | ApiError | null;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({ error, onRetry }) => {
  const getErrorMessage = () => {
    if (error instanceof ApiError) {
      return error.message;
    }
    if (error?.message) {
      return error.message;
    }
    return 'Đã xảy ra lỗi không xác định khi tải dữ liệu. Vui lòng thử lại sau.';
  };

  return (
    <div className="w-full flex flex-col items-center justify-center p-8 text-danger bg-danger-soft rounded-md border border-line">
      <AlertTriangle className="w-10 h-10 text-danger mb-3" />
      <h3 className="text-[14px] font-semibold text-danger mb-1">Lỗi tải dữ liệu</h3>
      <p className="text-[13px] text-danger/80 mb-5 text-center max-w-md">
        {getErrorMessage()}
      </p>
      
      {onRetry && (
        <button 
          onClick={onRetry}
          className="btn-secondary h-8 flex items-center justify-center gap-2"
        >
          <RefreshCcw size={14} />
          <span>Thử lại</span>
        </button>
      )}
    </div>
  );
};
