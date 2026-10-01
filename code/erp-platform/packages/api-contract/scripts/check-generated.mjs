/**
 * Kiểm tra src/generated/api.d.ts có khớp với openapi.json hay không.
 *
 * openapi.json được backend sinh lại mỗi lần chạy test (OpenApiGeneratorTest), còn api.d.ts phải
 * sinh bằng tay (`npm run generate`). Bước tay này từng bị bỏ quên khiến type ở frontend lệch với
 * API thật. Chạy: `npm run check --workspace=packages/api-contract` (thoát mã 1 nếu lệch).
 */
import { readFile } from 'node:fs/promises';
import openapiTS from 'openapi-typescript';

const specUrl = new URL('../openapi.json', import.meta.url);
const typesUrl = new URL('../src/generated/api.d.ts', import.meta.url);

const normalize = (text) => text.replace(/\r\n/g, '\n').trimEnd();

const expected = normalize(await openapiTS(specUrl));
const actual = normalize(await readFile(typesUrl, 'utf8'));

if (expected !== actual) {
  console.error('api.d.ts LỆCH với openapi.json. Chạy: npm run generate --workspace=packages/api-contract');
  process.exit(1);
}
console.log('api.d.ts khớp openapi.json');
