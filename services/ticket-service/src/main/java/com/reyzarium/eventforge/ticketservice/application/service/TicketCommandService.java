package com.reyzarium.eventforge.ticketservice.application.service;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.ValidateTicketRequest;
import com.reyzarium.eventforge.ticketservice.api.ticket.dto.ValidateTicketResponse;

public interface TicketCommandService {

    ValidateTicketResponse validateTicket(ValidateTicketRequest request);
}
