package com.reyzarium.eventforge.ticketservice.api.ticket.dto;

import java.util.UUID;

public record TicketPdfResponse(
        UUID ticketId,
        String pdfUrl
) {
}
