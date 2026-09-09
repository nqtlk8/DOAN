export interface StockMovement {
  id: number;
  movementType: 'INBOUND' | 'SALE' | 'RETURN' | 'ADJUSTMENT_UP' | 'ADJUSTMENT_DOWN';
  quantity: number;
  refType: string;
  refId: string;
  createdAt: string;
}
