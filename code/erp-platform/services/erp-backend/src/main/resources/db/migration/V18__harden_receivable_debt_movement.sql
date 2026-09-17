-- Thêm balance_before và idempotency_key
ALTER TABLE receivable_debt_movement
ADD COLUMN balance_before DECIMAL(19,4),
ADD COLUMN idempotency_key VARCHAR(255);

-- 1. Chuyển đổi amount sang signed amount dựa trên movement_type
UPDATE receivable_debt_movement
SET amount = CASE
    WHEN movement_type IN ('INVOICE', 'OPENING_BALANCE') THEN ABS(amount)
    WHEN movement_type IN ('PAYMENT', 'RETURN') THEN -ABS(amount)
    ELSE amount
END;

-- 2. Tính balance_before từ balance_after và signed amount
UPDATE receivable_debt_movement
SET balance_before = balance_after - amount;

-- Đặt balance_before thành NOT NULL
ALTER TABLE receivable_debt_movement
ALTER COLUMN balance_before SET NOT NULL;

-- 3. Tạo idempotency_key cho dữ liệu cũ
UPDATE receivable_debt_movement
SET idempotency_key = CASE 
    WHEN movement_type = 'OPENING_BALANCE' THEN movement_type || ':' || ref_type || ':' || ref_id || ':' || customer_id || ':' || branch_id
    ELSE movement_type || ':' || ref_type || ':' || ref_id
END
WHERE idempotency_key IS NULL;

-- 4. Đặt idempotency_key thành UNIQUE
ALTER TABLE receivable_debt_movement
ADD CONSTRAINT uk_debt_movement_idempotency UNIQUE (idempotency_key);
