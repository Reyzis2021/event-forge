package com.reyzarium.eventforge.ticketservice.infrastructure.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record BookingEventEnvelope(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        Instant occurredAt,
        String producer,
        UUID correlationId,
        BookingEventPayload payload
) {
}
