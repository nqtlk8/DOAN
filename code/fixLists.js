const fs = require('fs');

function fixFile(file, isProduct) {
  let c = fs.readFileSync(file, 'utf8');

  // Fix response unwrap
  c = c.replace(/response\?\.data \|\| \[\]/, 'response || []');

  // Replace save logic
  const oldSave = /const handleSave = \(\) => \{[\s\S]*?\}\s*?\};\n/;
  let newSave = `const handleSave = () => {
    createMutation.mutate({
      customerCode: formData.code,
      name: formData.name,
      phone: formData.phone,
      email: formData.email,
      address: formData.address,
      taxCode: formData.taxCode,
      customerType: (formData as any).type || 'RETAIL'
    });
  };
`;
  if (isProduct) {
    newSave = `const handleSave = () => {
    createMutation.mutate({
      productCode: formData.sku || formData.code,
      name: formData.name,
      baseUnit: formData.unit || 'CAI',
      categoryId: 1,
      description: formData.description
    });
  };
`;
  }
  c = c.replace(oldSave, newSave);

  // Remove update and delete mutations entirely
  c = c.replace(/const updateMutation = useMutation\(\{[\s\S]*?\}\);\n/g, '');
  c = c.replace(/const deleteMutation = useMutation\(\{[\s\S]*?\}\);\n/g, '');
  
  // Remove handleDelete
  c = c.replace(/const handleDelete = \([\s\S]*?\}\s*?\};\n/g, '');

  // Remove Admin td with edit and delete buttons
  c = c.replace(/\{isAdmin && \(\s*<td className="px-4 py-1\.5 text-sm text-right">[\s\S]*?<\/div>\s*<\/td>\s*\)\}/g, '');

  // Fix types in select
  if (!isProduct) {
    c = c.replace(/<option value="WHOLESALE">KhAch S\%<\/option>/, '<option value="WHOLESALE">Khách Sỉ</option><option value="CONSTRUCTION">Công Trình</option>');
    c = c.replace(/<option value="WHOLESALE">.*?<\/option>/, '<option value="WHOLESALE">Khách Sỉ</option><option value="CONSTRUCTION">Công Trình</option>');
  }

  // Remove submitting condition since updateMutation is gone
  c = c.replace(/const submitting = createMutation\.isPending \|\| updateMutation\.isPending;/, 'const submitting = createMutation.isPending;');
  c = c.replace(/updateMutation\.isPending/g, 'false');

  fs.writeFileSync(file, c, 'utf8');
}

fixFile('erp-platform/apps/erp-frontend/src/components/catalog/CustomerList.tsx', false);
fixFile('erp-platform/apps/erp-frontend/src/components/catalog/ProductList.tsx', true);
console.log('Done');
