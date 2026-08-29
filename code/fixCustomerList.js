const fs = require('fs');

let c = fs.readFileSync('erp-platform/apps/erp-frontend/src/components/catalog/CustomerList.tsx', 'utf8');

c = c.replace(/const updateMutation[\s\S]*?deleteCustomer\(id\),\s*onSuccess:[\s\S]*?\}\);\s*/g, '');
c = c.replace(/const handleDelete[\s\S]*?\};\s*/g, '');
c = c.replace(/<td className="px-4 py-1\.5 text-sm text-right">[\s\S]*?<Trash2 size=\{16\} \/>[\s\S]*?<\/td>/g, '');
c = c.replace(/\{isAdmin && \(\s*<td className="px-4 py-1\.5 text-sm text-right">[\s\S]*?<\/td>\s*\)\}/g, '');
c = c.replace(/const handleSave = \(\) => \{[\s\S]*?\};/g, 
`const handleSave = () => {
    createMutation.mutate({
      customerCode: formData.code,
      name: formData.name,
      phone: formData.phone,
      email: formData.email,
      address: formData.address,
      taxCode: formData.taxCode,
      customerType: (formData as any).type || 'RETAIL'
    });
  };`);
c = c.replace(/<option value="WHOLESALE">.*?<\/option>/, '<option value="WHOLESALE">Khách Sỉ</option><option value="CONSTRUCTION">Công Trình</option>');
c = c.replace(/isEditing/g, 'false');

fs.writeFileSync('erp-platform/apps/erp-frontend/src/components/catalog/CustomerList.tsx', c, 'utf8');
console.log("Done");
