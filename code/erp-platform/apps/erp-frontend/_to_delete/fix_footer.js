const fs = require('fs');
let code = fs.readFileSync('src/components/layout/MdiModuleLayout.tsx', 'utf8');

// The bottom toolbar starts with:
// {/* Bottom Toolbar (Sticky at bottom of main content) */}
// {activeSubView === 'FORM' && (
//   <div className="bg-slate-800 text-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)] px-4 py-2 flex items-center justify-between z-40 shrink-0 h-14">

const startTag = `{/* Bottom Toolbar (Sticky at bottom of main content) */}`;
const endTag = `</div>\n    </div>\n  );\n};`;

const newToolbar = `{/* Bottom Toolbar (Sticky at bottom of main content) */}
        {activeSubView === 'FORM' && (
          <div className="h-[48px] bg-[#E3E9F0] text-[#000] border-t border-[#999] px-[12px] flex justify-between items-center shrink-0">
            {/* Left side actions */}
            <div className="flex items-center gap-[8px]">
              {isView && (
                <>
                  <button onClick={onAdd} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-[2px] text-[13px] font-medium transition-colors">
                    <FilePlus size={16} /> Thêm (F2)
                  </button>
                  <button onClick={onEdit} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-[2px] text-[13px] font-medium transition-colors">
                    <Edit2 size={16} /> Sửa (F3)
                  </button>
                  <button onClick={onDelete} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-red-50 hover:text-red-600 rounded-[2px] text-[13px] font-medium transition-colors">
                    <Trash2 size={16} /> Xóa (F8)
                  </button>
                </>
              )}
              {!isView && (
                <>
                  <button onClick={onSave} disabled={isLoading} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-4 bg-teal-600 border border-teal-700 text-white hover:bg-teal-700 rounded-[2px] text-[13px] font-medium transition-colors disabled:opacity-50">
                    <Save size={16} /> Lưu (F4)
                  </button>
                  <button onClick={onCancel} disabled={isLoading} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-[2px] text-[13px] font-medium transition-colors disabled:opacity-50">
                    <X size={16} /> Hủy (Esc)
                  </button>
                </>
              )}
            </div>

            {/* Right side actions */}
            <div className="flex items-center gap-[8px]">
              <button onClick={onPrint} disabled={!isView} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-[#e0e0e0] rounded-[2px] text-[13px] font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                <Printer size={16} /> In (F7)
              </button>
              <button onClick={onExit} className="h-[32px] inline-flex items-center justify-center gap-[6px] px-3 bg-white border border-[#999] hover:bg-red-50 hover:text-red-600 rounded-[2px] text-[13px] font-medium transition-colors">
                <LogOut size={16} /> Thoát (F12)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};`;

const startIndex = code.indexOf(startTag);
if(startIndex > -1) {
    code = code.substring(0, startIndex) + newToolbar;
    fs.writeFileSync('src/components/layout/MdiModuleLayout.tsx', code, 'utf8');
    console.log('Fixed MdiModuleLayout bottom toolbar!');
} else {
    console.log('Could not find start tag');
}
