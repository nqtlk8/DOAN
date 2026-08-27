const fs = require('fs');
const lines = fs.readFileSync('src/App.tsx', 'utf8');
const matches = lines.match(/className=(?:"[^"]*"|\{[^}]*\})/g) || [];
console.log(matches.join('\n'));
