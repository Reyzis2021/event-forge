package com.reyzarium.eventforge.ticketservice.application.service;

import com.reyzarium.eventforge.ticketservice.application.event.BookingConfirmedEvent;

public interface TicketIssuanceService {

    void issueTickets(BookingConfirmedEvent event);
}
