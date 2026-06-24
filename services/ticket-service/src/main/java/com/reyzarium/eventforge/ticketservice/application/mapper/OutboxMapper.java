package com.reyzarium.eventforge.ticketservice.application.mapper;

import com.reyzarium.eventforge.ticketservice.application.event.outbox.EventEnvelope;
import com.reyzarium.eventforge.ticketservice.application.event.outbox.TicketIssuedPayload;
import com.reyzarium.eventforge.ticketservice.application.event.outbox.TicketUsedPayload;
import com.reyzarium.eventforge.ticketservice.domain.outbox.OutboxEventType;
import com.reyzarium.eventforge.ticketservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class OutboxMapper {

    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "ticket-service";

    @Autowired
    private ObjectMapper objectMapper;

    public OutboxEventEntity toTicketIssuedOutbox(TicketEntity ticket, Instant now) {
        TicketIssuedPayload payload = new TicketIssuedPayload(
                ticket.getId(),
                ticket.getBookingId(),
                ticket.getUserId(),
                ticket.getEventId(),
                ticket.getTicketNumber()
        );

        UUID outboxEventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(
                outboxEventId,
                OutboxEventType.TICKET_ISSUED,
                EVENT_VERSION,
                now,
                PRODUCER,
                UUID.randomUUID(),
                payload
        );

        return OutboxEventEntity.builder()
                .id(outboxEventId)
                .aggregateId(ticket.getId())
                .aggregateType("TICKET")
                .eventType(OutboxEventType.TICKET_ISSUED)
                .eventVersion(EVENT_VERSION)
                .payload(objectMapper.writeValueAsString(envelope))
                .status(OutboxStatus.PENDING)
                .createdAt(now)
                .retryCount(0)
                .build();
    }

    public OutboxEventEntity toTicketUsedOutbox(TicketEntity ticket, Instant now) {
        TicketUsedPayload payload = new TicketUsedPayload(
                ticket.getId(),
                ticket.getBookingId(),
                ticket.getUserId(),
                ticket.getEventId(),
                ticket.getTicketNumber(),
                ticket.getUsedAt()
        );

        UUID outboxEventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(
                outboxEventId,
                OutboxEventType.TICKET_USED,
                EVENT_VERSION,
                now,
                PRODUCER,
                UUID.randomUUID(),
                payload
        );

        return OutboxEventEntity.builder()
                .id(outboxEventId)
                .aggregateId(ticket.getId())
                .aggregateType("TICKET")
                .eventType(OutboxEventType.TICKET_USED)
                .eventVersion(EVENT_VERSION)
                .payload(objectMapper.writeValueAsString(envelope))
                .status(OutboxStatus.PENDING)
                .createdAt(now)
                .retryCount(0)
                .build();
    }
}
