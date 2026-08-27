import json
import os

openapi_file = 'openapi.json'
out_file = 'docs/API_CONTRACT.md'

if not os.path.exists('docs'):
    os.makedirs('docs')

def get_ref_name(ref):
    if not ref: return ''
    return ref.split('/')[-1]

def parse_schema(schema, components_schemas):
    if not schema: return 'any'
    if '' in schema:
        return get_ref_name(schema[''])
    if schema.get('type') == 'array':
        items = schema.get('items', {})
        if '' in items:
            return f"Array<{get_ref_name(items[''])}>"
        return f"Array<{items.get('type', 'any')}>"
    return schema.get('type', 'any')

with open(openapi_file, 'r', encoding='utf-8') as f:
    openapi = json.load(f)

paths = openapi.get('paths', {})
components = openapi.get('components', {}).get('schemas', {})

with open(out_file, 'w', encoding='utf-8') as out:
    out.write('# API CONTRACT (FRONTEND - BACKEND)\n\n')
    out.write('Tài liệu này định nghĩa chuẩn giao tiếp API giữa hệ thống React Frontend và Java Spring Backend dựa trên OpenAPI Swagger mới nhất.\n\n')
    
    out.write('## 1. Chuẩn Response Toàn Cục\n')
    out.write('Mọi REST API từ Backend đều bọc dữ liệu trong cấu trúc chuẩn sau (trừ khi có ghi chú khác):\n')
    out.write('`json\n{\n  "success": boolean,\n  "data": T | null,\n  "message": string,\n  "errors": Array<string> | null\n}\n`\n\n')
    
    out.write('## 2. API Endpoints theo Domain\n\n')
    
    domains = {}
    for path, methods in paths.items():
        for method, details in methods.items():
            tags = details.get('tags', ['General'])
            domain = tags[0]
            if domain not in domains:
                domains[domain] = []
            
            req_body = 'N/A'
            if 'requestBody' in details:
                content = details['requestBody'].get('content', {})
                if 'application/json' in content:
                    req_schema = content['application/json'].get('schema', {})
                    req_body = parse_schema(req_schema, components)
            
            res_body = 'N/A'
            if '200' in details.get('responses', {}):
                content = details['responses']['200'].get('content', {})
                if 'application/json' in content:
                    res_schema = content['application/json'].get('schema', {})
                    res_body = parse_schema(res_schema, components)
                elif '*/*' in content:
                    res_schema = content['*/*'].get('schema', {})
                    res_body = parse_schema(res_schema, components)
            
            domains[domain].append({
                'method': method.upper(),
                'path': path,
                'summary': details.get('summary', ''),
                'req': req_body,
                'res': res_body
            })
    
    for domain, endpoints in sorted(domains.items()):
        out.write(f'### Domain: {domain}\n')
        out.write('| Method | Path | Request Body | Response Data (T) | Summary |\n')
        out.write('|---|---|---|---|---|\n')
        for ep in endpoints:
            out.write(f"| {ep['method']} | {ep['path']} | {ep['req']} | {ep['res']} | {ep['summary']} |\n")
        out.write('\n')

    out.write('## 3. Cấu trúc DTO (Data Transfer Objects) Tham khảo\n')
    out.write('Một số DTO chính được sử dụng trong Request/Response:\n\n')
    
    important_dtos = ['SalesInvoiceDto', 'SalesInvoiceRequest', 'InboundReceiptDto', 'ProductDto', 'StockOnHandDto', 'BranchDto']
    for dto_name in important_dtos:
        if dto_name in components:
            out.write(f'### {dto_name}\n`json\n')
            props = components[dto_name].get('properties', {})
            out.write(json.dumps(props, indent=2, ensure_ascii=False))
            out.write('\n`\n\n')

print('Generated API_CONTRACT.md successfully.')
