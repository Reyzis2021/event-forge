package com.reyzarium.eventforge.ticketservice.application.event.outbox;

import java.util.UUID;

public record TicketIssuedPayload(
        UUID ticketId,
        UUID bookingId,
        UUID userId,
        UUID eventId,
        String ticketNumber
) {
}
