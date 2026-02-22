-- Remove soft-delete support from payments table.
-- Payments are immutable once created; any business-level status (e.g. refund, cancellation)
-- is expressed via the status column, not by hiding rows from queries.

DROP INDEX IF EXISTS idx_payments_active_by_time;

ALTER TABLE payments DROP COLUMN deleted;

CREATE INDEX idx_payments_by_time
    ON payments (created_at DESC);
