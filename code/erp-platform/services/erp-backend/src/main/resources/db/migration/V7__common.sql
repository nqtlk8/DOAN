-- ============================================================================
-- V7 — Module common: khóa chống gửi trùng (Idempotency-Key)
-- Dữ liệu cục bộ của từng instance. Không replicate.
-- ============================================================================

CREATE TABLE idempotency_record (
    id                  UUID         PRIMARY KEY,
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    request_hash        VARCHAR(64),
    response_snapshot   TEXT,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);
