import React, { useEffect } from 'react';
import { Trash2, Save, X, Printer, Edit2, FilePlus, LogOut } from 'lucide-react';

export type SubViewType = 'FORM' | 'LIST';
export type FormMode = 'VIEW' | 'ADD' | 'EDIT';

export interface MdiModuleLayoutProps {
  children: React.ReactNode;
  activeSubView: SubViewType;
  onSubViewChange: (view: SubViewType) => void;
  mode?: FormMode;
  onAdd?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onCancel?: () => void;
  onDelete?: () => void;
  onPrint?: () => void;
  onExit?: () => void;
  isLoading?: boolean;
}

export const MdiModuleLayout: React.FC<MdiModuleLayoutProps> = ({
  children,
  activeSubView,
  onSubViewChange,
  mode = 'VIEW',
  onAdd,
  onEdit,
  onSave,
  onCancel,
  onDelete,
  onPrint,
  onExit,
  isLoading = false
}) => {
  const isView = mode === 'VIEW';

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (activeSubView !== 'FORM') return; // Only trigger hotkeys in FORM view
      // Note: If there are modals open (like Print Preview), you might need an external state to skip hotkeys.

      switch (e.key) {
        case 'F2':
          e.preventDefault();
          if (isView && onAdd) onAdd();
          break;
        case 'F3':
          e.preventDefault();
          if (isView && onEdit) onEdit();
          break;
        case 'F4':
          e.preventDefault();
          if (!isView && onSave) onSave();
          break;
        case 'Escape':
          e.preventDefault();
          if (!isView && onCancel) onCancel();
          break;
        case 'F8':
          e.preventDefault();
          if (isView && onDelete) onDelete();
          break;
        case 'F7':
          e.preventDefault();
          if (isView && onPrint) onPrint();
          break;
        case 'F12':
          e.preventDefault();
          if (onExit) onExit();
          break;
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeSubView, isView, onAdd, onEdit, onSave, onCancel, onDelete, onPrint, onExit]);

  return (
    <div className="flex h-full bg-slate-100">
      {/* Left Sidebar */}
      <div className="w-48 bg-white border-r border-slate-200 flex flex-col shrink-0">
        <div className="p-4 border-b border-slate-200">
          <h3 className="font-semibold text-slate-700">Chức năng</h3>
        </div>
        <div className="flex flex-col p-2 space-y-1">
          <button
            onClick={() => onSubViewChange('FORM')}
            className={`px-4 py-2 text-left rounded-md text-sm font-medium transition-colors ${
              activeSubView === 'FORM'
                ? 'bg-blue-50 text-blue-700'
                : 'text-slate-600 hover:bg-slate-50'
            }`}
          >
            Nội dung
          </button>
          <button
            onClick={() => onSubViewChange('LIST')}
            className={`px-4 py-2 text-left rounded-md text-sm font-medium transition-colors ${
              activeSubView === 'LIST'
                ? 'bg-blue-50 text-blue-700'
                : 'text-slate-600 hover:bg-slate-50'
            }`}
          >
            Danh sách phiếu
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col relative overflow-hidden bg-white min-w-0">
        <div className="flex-1 overflow-auto">
          {children}
        </div>

        {/* Bottom Toolbar (Sticky at bottom of main content) */}
        {activeSubView === 'FORM' && (
          <div className="bg-slate-800 text-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)] px-4 py-2 flex items-center justify-between z-40 shrink-0 h-14">
            {/* Left side actions */}
            <div className="flex gap-2">
              {isView && (
                <>
                  <button onClick={onAdd} className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 rounded-md text-sm font-medium transition-colors">
                    <FilePlus size={16} /> Thêm (F2)
                  </button>
                  <button onClick={onEdit} className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 rounded-md text-sm font-medium transition-colors">
                    <Edit2 size={16} /> Sửa (F3)
                  </button>
                  <button onClick={onDelete} className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 hover:bg-red-600 hover:text-white rounded-md text-sm font-medium transition-colors text-red-400">
                    <Trash2 size={16} /> Xóa (F8)
                  </button>
                </>
              )}
              {!isView && (
                <>
                  <button onClick={onSave} disabled={isLoading} className="flex items-center gap-2 px-4 py-1.5 bg-blue-600 hover:bg-blue-500 rounded-md text-sm font-medium transition-colors disabled:opacity-50">
                    <Save size={16} /> Lưu (F4)
                  </button>
                  <button onClick={onCancel} disabled={isLoading} className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 rounded-md text-sm font-medium transition-colors disabled:opacity-50">
                    <X size={16} /> Hủy (Esc)
                  </button>
                </>
              )}
            </div>

            {/* Right side actions */}
            <div className="flex gap-2 border-l border-slate-600 pl-4">
              <button onClick={onPrint} disabled={!isView} className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 rounded-md text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                <Printer size={16} /> In (F7)
              </button>
              <button onClick={onExit} className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 hover:bg-red-600 rounded-md text-sm font-medium transition-colors">
                <LogOut size={16} /> Thoát (F12)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
