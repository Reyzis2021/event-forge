package com.reyzarium.eventforge.bookingservice.application.event.outbox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingCreatedPayload(
        UUID bookingId,
        UUID userId,
        UUID eventId,
        UUID ticketTypeId,
        Integer quantity,
        BigDecimal amount,
        String currency,
        Instant expiresAt
) {
}
