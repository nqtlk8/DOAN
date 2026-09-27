const fs = require('fs');

const replaceColors = (filePath) => {
    let code = fs.readFileSync(filePath, 'utf8');
    
    // Borders
    code = code.replace(/#666/g, '#94a3b8'); // slate-400
    code = code.replace(/#333/g, '#64748b'); // slate-500
    code = code.replace(/#ccc/g, '#cbd5e1'); // slate-300
    code = code.replace(/#a0a0a0/g, '#94a3b8'); // slate-400
    
    // Backgrounds
    code = code.replace(/#f0f0f0/g, '#f1f5f9'); // slate-100
    code = code.replace(/#E3E9F0/g, '#e2e8f0'); // slate-200
    code = code.replace(/#e0e0e0/g, '#e2e8f0'); // slate-200
    code = code.replace(/#d0d0d0/g, '#cbd5e1'); // slate-300
    code = code.replace(/#F3F2EE/g, '#f8fafc'); // slate-50
    code = code.replace(/#FFFDE7/g, '#fefce8'); // yellow-50
    code = code.replace(/#E2EDF8/g, '#e0f2fe'); // sky-100 (hover)
    
    // Text
    code = code.replace(/#000/g, '#0f172a'); // slate-900
    // Keep text-[#333] ? Already replaced #333 with #64748b, let's change text-[#64748b] to text-[#334155] (slate-700)
    code = code.replace(/text-\[\#64748b\]/g, 'text-slate-700'); 
    code = code.replace(/text-black/g, 'text-slate-900');
    code = code.replace(/bg-white/g, 'bg-white');

    // Make sure fonts are Inter
    if(filePath.includes('GenericDocumentForm')) {
        code = code.replace(/className="flex-1/g, 'className="font-sans flex-1');
    }
    
    fs.writeFileSync(filePath, code, 'utf8');
};

replaceColors('src/components/common/document/GenericDocumentForm.tsx');
replaceColors('src/components/layout/MdiModuleLayout.tsx');
replaceColors('src/components/layout/TopRibbon.tsx');

console.log('Colors and typography updated via UI-UX-Pro-Max rules (Slate palette)!');
