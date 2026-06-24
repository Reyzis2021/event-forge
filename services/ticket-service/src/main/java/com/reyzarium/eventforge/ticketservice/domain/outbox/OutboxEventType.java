package com.reyzarium.eventforge.ticketservice.domain.outbox;

public final class OutboxEventType {

    private OutboxEventType() {
    }

    public static final String TICKET_ISSUED = "ticket.issued";
    public static final String TICKET_USED = "ticket.used";
}
