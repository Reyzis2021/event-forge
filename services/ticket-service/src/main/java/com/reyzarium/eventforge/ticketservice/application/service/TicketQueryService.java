package com.reyzarium.eventforge.ticketservice.application.service;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.TicketResponse;

import java.util.List;
import java.util.UUID;

public interface TicketQueryService {

    List<TicketResponse> getMyTickets(UUID userId);

    TicketResponse getTicket(UUID userId, UUID ticketId);
}
