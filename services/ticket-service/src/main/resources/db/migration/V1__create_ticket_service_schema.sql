CREATE TABLE tickets (
                         id UUID PRIMARY KEY,
                         booking_id UUID NOT NULL,
                         user_id UUID NOT NULL,
                         event_id UUID NOT NULL,
                         ticket_type_id UUID NOT NULL,
                         ticket_number VARCHAR(100) UNIQUE NOT NULL,
                         status VARCHAR(50) NOT NULL,
                         qr_token_hash VARCHAR(255) UNIQUE NOT NULL,
                         pdf_file_key VARCHAR(255),
                         issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         used_at TIMESTAMP WITH TIME ZONE,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         version BIGINT NOT NULL
);

CREATE TABLE ticket_issuance_log (
                                     id UUID PRIMARY KEY,
                                     booking_id UUID UNIQUE NOT NULL,
                                     status VARCHAR(50) NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     error_message TEXT
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

CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_user_id ON tickets(user_id);
CREATE INDEX idx_tickets_event_id ON tickets(event_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_ticket_issuance_log_booking_id ON ticket_issuance_log(booking_id);
CREATE INDEX idx_outbox_events_status_created_at ON outbox_events(status, created_at);
