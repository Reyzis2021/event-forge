package com.reyzarium.eventforge.eventservice.application.mapper;

import com.reyzarium.eventforge.eventservice.domain.outbox.OutboxEventType;
import com.reyzarium.eventforge.eventservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.EventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.TicketTypeEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class OutboxMapper {

    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "event-service";

    @Autowired
    private ObjectMapper objectMapper;

    public OutboxEventEntity toEventCreatedOutbox(EventEntity event, Instant now) {
        EventCreatedPayload payload = new EventCreatedPayload(
                event.getId(),
                event.getOrganizerId(),
                event.getStatus().name()
        );

        return toOutboxEvent(event, OutboxEventType.EVENT_CREATED, payload, now);
    }

    public OutboxEventEntity toEventPublishedOutbox(EventEntity event, Instant now) {
        EventPublishedPayload payload = new EventPublishedPayload(
                event.getId(),
                event.getOrganizerId(),
                event.getTicketTypes()
                        .stream()
                        .map(this::toTicketTypePayload)
                        .toList()
        );

        return toOutboxEvent(event, OutboxEventType.EVENT_PUBLISHED, payload, now);
    }

    public OutboxEventEntity toEventCancelledOutbox(EventEntity event, String reason, Instant now) {
        EventCancelledPayload payload = new EventCancelledPayload(
                event.getId(),
                event.getOrganizerId(),
                reason
        );

        return toOutboxEvent(event, OutboxEventType.EVENT_CANCELLED, payload, now);
    }

    private OutboxEventEntity toOutboxEvent(EventEntity event, String eventType, Object payload, Instant now) {
        UUID outboxEventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(
                outboxEventId,
                eventType,
                EVENT_VERSION,
                now,
                PRODUCER,
                UUID.randomUUID(),
                payload
        );

        return OutboxEventEntity.builder()
                .id(outboxEventId)
                .aggregateId(event.getId())
                .aggregateType("EVENT")
                .eventType(eventType)
                .eventVersion(EVENT_VERSION)
                .payload(toJson(envelope))
                .status(OutboxStatus.PENDING)
                .createdAt(now)
                .retryCount(0)
                .build();
    }

    private EventPublishedTicketTypePayload toTicketTypePayload(TicketTypeEntity ticketType) {
        return new EventPublishedTicketTypePayload(
                ticketType.getId(),
                ticketType.getCapacity(),
                ticketType.getPrice(),
                ticketType.getCurrency()
        );
    }

    private String toJson(Object payload) {
        return objectMapper.writeValueAsString(payload);
    }

    private record EventPublishedPayload(
            UUID eventId,
            UUID organizerId,
            List<EventPublishedTicketTypePayload> ticketTypes
    ) {
    }

    private record EventCreatedPayload(
            UUID eventId,
            UUID organizerId,
            String status
    ) {
    }

    private record EventPublishedTicketTypePayload(
            UUID ticketTypeId,
            Integer capacity,
            BigDecimal price,
            String currency
    ) {
    }

    private record EventCancelledPayload(
            UUID eventId,
            UUID organizerId,
            String reason
    ) {
    }

    private record EventEnvelope(
            UUID eventId,
            String eventType,
            Integer eventVersion,
            Instant occurredAt,
            String producer,
            UUID correlationId,
            Object payload
    ) {
    }
}
