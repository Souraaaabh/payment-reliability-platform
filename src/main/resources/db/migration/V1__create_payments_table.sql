CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    failure_reason VARCHAR(512),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payment_transaction_id UNIQUE (transaction_id),
    CONSTRAINT uk_payment_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payment_user_id ON payments (user_id);
CREATE INDEX idx_payment_status ON payments (status);
