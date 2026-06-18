CREATE TABLE bookings (
                          id UUID PRIMARY KEY,
                          user_id UUID NOT NULL,
                          event_id UUID NOT NULL,
                          idempotency_key VARCHAR(100) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          total_amount NUMERIC(12, 2) NOT NULL,
                          currency VARCHAR(10) NOT NULL,
                          expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          version BIGINT NOT NULL
);

CREATE TABLE booking_items (
                               id UUID PRIMARY KEY,
                               booking_id UUID NOT NULL REFERENCES bookings(id),
                               ticket_type_id UUID NOT NULL,
                               quantity INT NOT NULL,
                               unit_price NUMERIC(12, 2) NOT NULL,
                               currency VARCHAR(10) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               version BIGINT NOT NULL
);

CREATE TABLE ticket_type_inventory (
                                       event_id UUID NOT NULL,
                                       ticket_type_id UUID NOT NULL,
                                       capacity INT NOT NULL,
                                       reserved INT NOT NULL,
                                       sold INT NOT NULL,
                                       version BIGINT NOT NULL,
                                       PRIMARY KEY (event_id, ticket_type_id)
);

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

CREATE UNIQUE INDEX idx_bookings_user_id_idempotency_key ON bookings(user_id, idempotency_key);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_event_id ON bookings(event_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_expires_at ON bookings(expires_at);

CREATE INDEX idx_booking_items_booking_id ON booking_items(booking_id);
CREATE INDEX idx_booking_items_ticket_type_id ON booking_items(ticket_type_id);

CREATE INDEX idx_ticket_type_inventory_event_id ON ticket_type_inventory(event_id);
CREATE INDEX idx_outbox_events_status_created_at ON outbox_events(status, created_at);
