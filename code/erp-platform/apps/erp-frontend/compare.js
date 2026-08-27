const fs = require('fs');
const path = require('path');

function getFiles(dir, files = []) {
  if (!fs.existsSync(dir)) return files;
  const list = fs.readdirSync(dir);
  for (const file of list) {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      getFiles(fullPath, files);
    } else if (fullPath.endsWith('.tsx') || fullPath.endsWith('.ts')) {
      files.push(fullPath);
    }
  }
  return files;
}

const erpDir = 'D:/Docs/CodeProject/DOAN/code/erp/src/components';
const frontendDir = 'src/components';

const files = getFiles(frontendDir);
for (const file of files) {
  const relPath = path.relative(frontendDir, file);
  const origPath = path.join(erpDir, relPath);
  
  if (fs.existsSync(origPath)) {
    const f1 = fs.readFileSync(origPath, 'utf8');
    const f2 = fs.readFileSync(file, 'utf8');
    
    const matches1 = f1.match(/className=(?:"[^"]*"|\{[^}]*\})/g) || [];
    const matches2 = f2.match(/className=(?:"[^"]*"|\{[^}]*\})/g) || [];
    
    if (matches1.length !== matches2.length) {
      console.log('MISMATCH:', relPath, 'F1:', matches1.length, 'F2:', matches2.length);
    }
  }
}
console.log('Done checking counts.');
