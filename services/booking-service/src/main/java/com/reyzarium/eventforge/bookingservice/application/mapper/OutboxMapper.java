package com.reyzarium.eventforge.bookingservice.application.mapper;

import com.reyzarium.eventforge.bookingservice.application.event.outbox.BookingConfirmedPayload;
import com.reyzarium.eventforge.bookingservice.application.event.outbox.BookingCreatedPayload;
import com.reyzarium.eventforge.bookingservice.application.event.outbox.BookingPaymentFailedPayload;
import com.reyzarium.eventforge.bookingservice.application.event.outbox.EventEnvelope;
import com.reyzarium.eventforge.bookingservice.domain.outbox.OutboxEventType;
import com.reyzarium.eventforge.bookingservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingItemEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class OutboxMapper {

    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "booking-service";

    @Autowired
    private ObjectMapper objectMapper;

    public OutboxEventEntity toBookingCreatedOutbox(BookingEntity booking, Instant now) {
        BookingItemEntity item = booking.getItems().getFirst();
        BookingCreatedPayload payload = new BookingCreatedPayload(
                booking.getId(),
                booking.getUserId(),
                booking.getEventId(),
                item.getTicketTypeId(),
                item.getQuantity(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getExpiresAt()
        );

        return toOutboxEvent(booking, OutboxEventType.BOOKING_CREATED, payload, now);
    }

    public OutboxEventEntity toBookingConfirmedOutbox(BookingEntity booking, Instant now) {
        BookingItemEntity item = booking.getItems().getFirst();
        BookingConfirmedPayload payload = new BookingConfirmedPayload(
                booking.getId(),
                booking.getUserId(),
                booking.getEventId(),
                item.getTicketTypeId(),
                item.getQuantity(),
                booking.getTotalAmount(),
                booking.getCurrency()
        );

        return toOutboxEvent(booking, OutboxEventType.BOOKING_CONFIRMED, payload, now);
    }

    public OutboxEventEntity toBookingPaymentFailedOutbox(BookingEntity booking, Instant now) {
        BookingItemEntity item = booking.getItems().getFirst();
        BookingPaymentFailedPayload payload = new BookingPaymentFailedPayload(
                booking.getId(),
                booking.getUserId(),
                booking.getEventId(),
                item.getTicketTypeId(),
                item.getQuantity(),
                booking.getTotalAmount(),
                booking.getCurrency()
        );

        return toOutboxEvent(booking, OutboxEventType.BOOKING_PAYMENT_FAILED, payload, now);
    }

    private OutboxEventEntity toOutboxEvent(BookingEntity booking, String eventType, Object payload, Instant now) {
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
                .aggregateId(booking.getId())
                .aggregateType("BOOKING")
                .eventType(eventType)
                .eventVersion(EVENT_VERSION)
                .payload(objectMapper.writeValueAsString(envelope))
                .status(OutboxStatus.PENDING)
                .createdAt(now)
                .retryCount(0)
                .build();
    }

}
