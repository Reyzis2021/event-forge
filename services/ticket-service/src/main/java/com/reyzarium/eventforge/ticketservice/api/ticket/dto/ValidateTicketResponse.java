package com.reyzarium.eventforge.ticketservice.api.ticket.dto;

import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;

import java.util.UUID;

public record ValidateTicketResponse(
        UUID ticketId,
        String ticketNumber,
        TicketStatus status,
        boolean valid,
        String message
) {
}
