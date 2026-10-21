import type { components } from '@erp/api-contract';

/** Biến động tồn kho (GET /api/v1/stock-movements), đúng với StockMovementResponseDto của contract. */
export type StockMovement = components['schemas']['StockMovementResponseDto'];
