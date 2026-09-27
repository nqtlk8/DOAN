import React, { useEffect } from 'react';
import { X } from 'lucide-react';

export interface QuickCreateDialogProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  onSave: () => void;
  isSaving?: boolean;
}

export const QuickCreateDialog: React.FC<QuickCreateDialogProps> = ({
  isOpen,
  onClose,
  title,
  children,
  onSave,
  isSaving = false,
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;
      if (e.key === 'Escape') {
        e.preventDefault();
        e.stopPropagation();
        onClose();
      } else if (e.key === 'Enter' && !e.shiftKey) {
        // Option to trigger save on Enter
        // Must ensure we're not inside a textarea
        const target = e.target as HTMLElement;
        if (target.tagName !== 'TEXTAREA') {
          e.preventDefault();
          onSave();
        }
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown, true);
    }
    return () => window.removeEventListener('keydown', handleKeyDown, true);
  }, [isOpen, onClose, onSave]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 z-[400] flex items-center justify-center p-4">
      <div className="card w-full max-w-md flex flex-col" onClick={e => e.stopPropagation()}>
        <div className="h-[44px] px-4 border-b border-line flex justify-between items-center bg-slate-50 rounded-t-lg">
          <h2 className="text-[15px] font-semibold text-ink">{title}</h2>
          <button onClick={onClose} className="text-ink-muted hover:text-ink">
            <X size={18} />
          </button>
        </div>
        
        <div className="p-4 space-y-4 bg-surface">
          {children}
        </div>

        <div className="p-3 border-t border-line bg-slate-50 flex justify-end gap-2 rounded-b-lg">
          <button onClick={onClose} disabled={isSaving} className="btn btn-secondary">
            Hủy
          </button>
          <button onClick={onSave} disabled={isSaving} className="btn btn-primary min-w-[80px]">
            {isSaving ? 'Đang lưu...' : 'Lưu'}
          </button>
        </div>
      </div>
    </div>
  );
};
