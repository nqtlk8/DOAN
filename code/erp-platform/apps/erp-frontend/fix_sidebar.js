const fs = require('fs');
let code = fs.readFileSync('src/components/layout/MdiModuleLayout.tsx', 'utf8');

// Replace Left Sidebar
const newSidebar = `{/* Left Vertical Tabs (Sidebar) */}
      <div className="w-[26px] bg-[#E3E9F0] border-r border-[#999] flex flex-col items-center shrink-0">
        <button
          onClick={() => onSubViewChange('FORM')}
          className={\`w-full py-8 flex items-center justify-center border-b border-[#ccc] hover:bg-[#d0d0d0] \${
            activeSubView === 'FORM' ? 'bg-white font-bold text-[#000]' : 'text-[#333]'
          }\`}
        >
          <span style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }} className="text-[12px] whitespace-nowrap">Nội dung</span>
        </button>
        <button
          onClick={() => onSubViewChange('LIST')}
          className={\`w-full py-8 flex items-center justify-center border-b border-[#ccc] hover:bg-[#d0d0d0] \${
            activeSubView === 'LIST' ? 'bg-white font-bold text-[#000]' : 'text-[#333]'
          }\`}
        >
          <span style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }} className="text-[12px] whitespace-nowrap">Danh sách phiếu</span>
        </button>
      </div>`;

code = code.replace(/\{\/\* Left Sidebar \*\/\}[\s\S]*?\{\/\* Main Content \*\/\}/, newSidebar + '\n\n      {/* Main Content */}');

fs.writeFileSync('src/components/layout/MdiModuleLayout.tsx', code, 'utf8');
console.log('Sidebar updated');
