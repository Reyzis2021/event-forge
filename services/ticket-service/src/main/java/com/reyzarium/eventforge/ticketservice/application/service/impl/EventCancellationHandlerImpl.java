package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.event.EventCancelledEvent;
import com.reyzarium.eventforge.ticketservice.application.service.EventCancellationHandler;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventCancellationHandlerImpl implements EventCancellationHandler {

    private final TicketRepository ticketRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void handle(EventCancelledEvent event) {
        var activeTickets = ticketRepository.findByEventIdAndStatus(
                event.businessEventId(),
                TicketStatus.ACTIVE
        );
        Instant now = Instant.now(clock);

        activeTickets.forEach(ticket -> ticket.markCancelled(now));
    }
}
