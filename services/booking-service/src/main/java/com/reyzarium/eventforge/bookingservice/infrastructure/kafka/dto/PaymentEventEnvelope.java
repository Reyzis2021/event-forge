package com.reyzarium.eventforge.bookingservice.infrastructure.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record PaymentEventEnvelope(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        Instant occurredAt,
        String producer,
        UUID correlationId,
        PaymentEventPayload payload
) {
}
