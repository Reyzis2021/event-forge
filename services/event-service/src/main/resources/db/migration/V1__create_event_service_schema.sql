CREATE TABLE events (
                        id UUID PRIMARY KEY,
                        organizer_id UUID NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        category VARCHAR(100) NOT NULL,
                        city VARCHAR(100) NOT NULL,
                        location VARCHAR(255) NOT NULL,
                        starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        capacity INT NOT NULL,
                        available_for_booking BOOLEAN NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        version BIGINT NOT NULL
);

CREATE TABLE ticket_types (
                              id UUID PRIMARY KEY,
                              event_id UUID NOT NULL REFERENCES events(id),
                              name VARCHAR(100) NOT NULL,
                              price NUMERIC(12, 2) NOT NULL,
                              currency VARCHAR(10) NOT NULL,
                              capacity INT NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              version BIGINT NOT NULL
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

CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_city ON events(city);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_starts_at ON events(starts_at);
CREATE INDEX idx_ticket_types_event_id ON ticket_types(event_id);
CREATE INDEX idx_outbox_status_created_at ON outbox_events(status, created_at);
