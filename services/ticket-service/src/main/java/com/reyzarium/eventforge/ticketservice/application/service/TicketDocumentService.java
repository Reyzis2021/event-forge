package com.reyzarium.eventforge.ticketservice.application.service;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.TicketPdfResponse;

import java.util.UUID;

public interface TicketDocumentService {

    TicketPdfResponse getTicketPdf(UUID userId, UUID ticketId);
}
