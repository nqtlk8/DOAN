import React, { useEffect, useState } from 'react';
import { AlertTriangle, X, Info } from 'lucide-react';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDestructive?: boolean;
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy',
  isDestructive = true,
  onConfirm,
  onCancel,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Esc = Hủy. Bắt ở pha capture để không lọt xuống phím tắt của form.
  useEffect(() => {
    if (!isOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isSubmitting) {
        e.preventDefault();
        e.stopPropagation();
        onCancel();
      }
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [isOpen, isSubmitting, onCancel]);

  if (!isOpen) return null;

  const handleConfirm = async () => {
    setIsSubmitting(true);
    try {
      await onConfirm();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div role="dialog" aria-modal="true" aria-label={title} className="fixed inset-0 z-[250] flex items-center justify-center p-4 bg-ink/40 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-surface rounded-lg shadow-lg w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200 border border-line">
        <div className="flex justify-between items-start p-5 border-b border-line">
          <div className="flex gap-3">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${isDestructive ? 'bg-danger-soft text-danger' : 'bg-primary-soft text-primary'}`}>
              {isDestructive ? <AlertTriangle size={20} /> : <Info size={20} />}
            </div>
            <div>
              <h3 className="text-[15px] font-semibold text-ink leading-tight mt-0.5">{title}</h3>
              <p className="text-[13px] text-ink-muted mt-1 leading-relaxed">{message}</p>
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onCancel}
            disabled={isSubmitting}
            className="text-ink-subtle hover:text-ink hover:bg-slate-100 p-1.5 rounded-md transition-colors"
          >
            <X size={18} />
          </button>
        </div>
        
        <div className="p-4 bg-app flex justify-end gap-3 border-t border-line">
          <button
            type="button"
            onClick={onCancel}
            disabled={isSubmitting}
            data-testid="confirm-dialog-cancel"
            className="btn btn-secondary h-8 px-4"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={isSubmitting}
            data-testid="confirm-dialog-confirm"
            autoFocus
            className={`btn h-8 px-4 ${
              isDestructive ? 'btn-danger' : 'btn-primary'
            }`}
          >
            {isSubmitting && (
              <svg className="animate-spin -ml-1 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            )}
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
