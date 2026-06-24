package com.reyzarium.eventforge.ticketservice.application.event.outbox;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        Instant occurredAt,
        String producer,
        UUID correlationId,
        Object payload
) {
}
