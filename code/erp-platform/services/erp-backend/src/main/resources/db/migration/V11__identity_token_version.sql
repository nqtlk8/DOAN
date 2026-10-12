-- Phien ban phien dang nhap cua user (claim "tv" trong JWT).
-- Tang len 1 khi dang xuat hoac phat hien refresh token bi dung lai: moi token cu het hieu luc.
--
-- LUU Y TRIEN KHAI (logical replication): user_account la bang MASTER (HQ -> Branch).
-- Subscriber phai co cot truoc khi publisher gui du lieu co cot moi, nen:
--   1) khoi dong lai cac server CHI NHANH truoc (Flyway them cot o DB chi nhanh),
--   2) sau do moi khoi dong lai HQ.
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
