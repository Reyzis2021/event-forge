CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          booking_id UUID NOT NULL,
                          user_id UUID NOT NULL,
                          amount NUMERIC(12, 2) NOT NULL,
                          currency VARCHAR(10) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          provider VARCHAR(50) NOT NULL,
                          provider_invoice_id VARCHAR(100) NOT NULL,
                          idempotency_key VARCHAR(100) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          paid_at TIMESTAMP WITH TIME ZONE,
                          failed_at TIMESTAMP WITH TIME ZONE,
                          version BIGINT NOT NULL
);

CREATE UNIQUE INDEX idx_payments_user_id_idempotency_key ON payments(user_id, idempotency_key);
CREATE UNIQUE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE UNIQUE INDEX idx_payments_provider_invoice_id ON payments(provider_invoice_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);

CREATE TABLE payment_webhooks (
                                  id UUID PRIMARY KEY,
                                  provider VARCHAR(50) NOT NULL,
                                  provider_event_id VARCHAR(100) NOT NULL,
                                  provider_invoice_id VARCHAR(100) NOT NULL,
                                  payload JSONB NOT NULL,
                                  processed BOOLEAN NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  processed_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_payment_webhooks_provider_event_id ON payment_webhooks(provider_event_id);
CREATE INDEX idx_payment_webhooks_provider_invoice_id ON payment_webhooks(provider_invoice_id);

CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,
                               aggregate_id UUID NOT NULL,
                               aggregate_type VARCHAR(100) NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               event_version INT NOT NULL,
                               payload JSONB NOT NULL,
                               status VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               published_at TIMESTAMP WITH TIME ZONE,
                               retry_count INT NOT NULL DEFAULT 0,
                               last_error TEXT
);

CREATE INDEX idx_outbox_events_status_created_at ON outbox_events(status, created_at);
