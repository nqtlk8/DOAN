-- BUG-1 (docs/audit/sales-invoice-create-bug-report.md):
-- user_account.id la BIGINT nhung cac cot audit/nghiep vu (created_by, updated_by,
-- confirmed_by, receivable_debt_movement.created_by) la UUID. JWT subject truoc day la
-- id so ("2") -> UUID.fromString() loi "Invalid UUID string".
-- Them dinh danh cong khai kieu UUID cho user; JWT subject se dung cot nay.
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS public_id UUID;

UPDATE user_account SET public_id = gen_random_uuid() WHERE public_id IS NULL;

ALTER TABLE user_account ALTER COLUMN public_id SET DEFAULT gen_random_uuid();
ALTER TABLE user_account ALTER COLUMN public_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_account_public_id ON user_account (public_id);
