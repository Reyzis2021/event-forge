package com.reyzarium.eventforge.ticketservice.application.event.outbox;

import java.time.Instant;
import java.util.UUID;

public record TicketUsedPayload(
        UUID ticketId,
        UUID bookingId,
        UUID userId,
        UUID eventId,
        String ticketNumber,
        Instant usedAt
) {
}
