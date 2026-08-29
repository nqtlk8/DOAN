const fs = require('fs');

let c = fs.readFileSync('erp-platform/apps/erp-frontend/src/components/catalog/ProductList.tsx', 'utf8');

c = c.replace(/const updateMutation[\s\S]*?deleteProduct\(id\),\s*onSuccess:[\s\S]*?\}\);\s*/g, '');
c = c.replace(/const handleDelete[\s\S]*?\};\s*/g, '');
c = c.replace(/<td className="px-4 py-1\.5 text-sm text-right">[\s\S]*?<Trash2 size=\{16\} \/>[\s\S]*?<\/td>/g, '');
c = c.replace(/\{isAdmin && \(\s*<td className="px-4 py-1\.5 text-sm text-right">[\s\S]*?<\/td>\s*\)\}/g, '');
c = c.replace(/const handleSave = \(\) => \{[\s\S]*?\};/g, 
`const handleSave = () => {
    createMutation.mutate({
      productCode: formData.sku || formData.code,
      name: formData.name,
      baseUnit: formData.unit || 'CAI',
      categoryId: 1, // Hardcoded for now
      description: formData.description
    });
  };`);
c = c.replace(/isEditing/g, 'false');

fs.writeFileSync('erp-platform/apps/erp-frontend/src/components/catalog/ProductList.tsx', c, 'utf8');
console.log("Done");
