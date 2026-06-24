package com.reyzarium.eventforge.ticketservice.api.ticket.dto;

import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID ticketId,
        UUID bookingId,
        UUID eventId,
        String ticketNumber,
        TicketStatus status,
        Instant issuedAt
) {
}
