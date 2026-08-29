const fs = require('fs');

let f = 'erp-platform/apps/erp-frontend/src/api/ApiService.ts';
let c = fs.readFileSync(f, 'utf8');

c = c.replace(/getDistributors: \(\) => axiosInstance\.get\('\/api\/v1\/suppliers'\)\.then\(\(res: any\) => res\.data\.data\)/, 
              "getDistributors: () => axiosInstance.get('/api/v1/suppliers').then((res: any) => res.data)");

c = c.replace(/createDistributor: \(payload: any\) =>\s*axiosInstance\.post\('\/api\/v1\/suppliers', payload\)\.then\(\(res: any\) => res\.data\.data\)/, 
              "createDistributor: (payload: any) => axiosInstance.post('/api/v1/suppliers', payload).then((res: any) => res.data)");

c = c.replace(/updateDistributor: \(id: string, payload: any\) =>\s*axiosInstance\.put\('\/api\/v1\/suppliers\/' \+ id, payload\)\.then\(\(res: any\) => res\.data\.data\)/, 
              "updateDistributor: (id: string, payload: any) => axiosInstance.put('/api/v1/suppliers/' + id, payload).then((res: any) => res.data)");

c = c.replace(/deleteDistributor: \(id: string\) =>\s*axiosInstance\.delete\('\/api\/v1\/suppliers\/' \+ id\)\.then\(\(res: any\) => res\.data\.data\)/, 
              "deleteDistributor: (id: string) => axiosInstance.delete('/api/v1/suppliers/' + id).then((res: any) => res.data)");

fs.writeFileSync(f, c, 'utf8');

console.log('Fixed ApiService');
