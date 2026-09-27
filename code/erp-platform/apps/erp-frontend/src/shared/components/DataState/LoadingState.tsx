import React from 'react';

export const LoadingState: React.FC = () => {
  return (
    <div className="w-full flex items-center justify-center p-12">
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-4 border-line-strong border-t-primary rounded-full animate-spin"></div>
        <p className="text-[13px] text-ink-muted">Đang tải dữ liệu...</p>
      </div>
    </div>
  );
};

export const TableSkeleton: React.FC = () => {
  return (
    <div className="w-full">
      <div className="space-y-2 p-4">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="w-full h-[28px] bg-slate-100 animate-pulse rounded-[4px]" />
        ))}
      </div>
    </div>
  );
};
