CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE currencies (
    code            VARCHAR(3)      PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    fee_percentage  NUMERIC(10,6)    NOT NULL,
    minimum_fee     NUMERIC(19,4)   NOT NULL,
    decimals        SMALLINT        NOT NULL DEFAULT 2
        CONSTRAINT check_decimals CHECK (decimals BETWEEN 0 AND 18),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments (
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    amount            NUMERIC(19,4)   NOT NULL,
    currency          VARCHAR(3)      NOT NULL,
    recipient         VARCHAR(255)    NOT NULL,
    recipient_account VARCHAR(255)    NOT NULL,
    processing_fee    NUMERIC(19,4)   NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
      CONSTRAINT check_payment_status
          CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    created_by        VARCHAR(255),
    deleted           BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_currency FOREIGN KEY (currency) REFERENCES currencies(code)
);

CREATE TRIGGER trigger_update_currencies_timestamp
    BEFORE UPDATE ON currencies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_update_payments_timestamp
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_payments_active_by_time
    ON payments (created_at DESC)
    WHERE deleted = false;
