package com.reyzarium.eventforge.paymentservice.application.mapper;

import com.reyzarium.eventforge.paymentservice.application.event.outbox.EventEnvelope;
import com.reyzarium.eventforge.paymentservice.application.event.outbox.PaymentCreatedPayload;
import com.reyzarium.eventforge.paymentservice.application.event.outbox.PaymentFailedPayload;
import com.reyzarium.eventforge.paymentservice.application.event.outbox.PaymentSucceededPayload;
import com.reyzarium.eventforge.paymentservice.domain.outbox.OutboxEventType;
import com.reyzarium.eventforge.paymentservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class OutboxMapper {

    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "payment-service";

    @Autowired
    private ObjectMapper objectMapper;

    public OutboxEventEntity toPaymentCreatedOutbox(PaymentEntity payment, Instant now) {
        PaymentCreatedPayload payload = new PaymentCreatedPayload(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProviderInvoiceId()
        );

        return toOutboxEvent(payment, OutboxEventType.PAYMENT_CREATED, payload, now);
    }

    public OutboxEventEntity toPaymentSucceededOutbox(PaymentEntity payment, Instant now) {
        PaymentSucceededPayload payload = new PaymentSucceededPayload(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt()
        );

        return toOutboxEvent(payment, OutboxEventType.PAYMENT_SUCCEEDED, payload, now);
    }

    public OutboxEventEntity toPaymentFailedOutbox(PaymentEntity payment, Instant now) {
        PaymentFailedPayload payload = new PaymentFailedPayload(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getFailedAt()
        );

        return toOutboxEvent(payment, OutboxEventType.PAYMENT_FAILED, payload, now);
    }

    private OutboxEventEntity toOutboxEvent(PaymentEntity payment, String eventType, Object payload, Instant now) {
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
                .aggregateId(payment.getId())
                .aggregateType("PAYMENT")
                .eventType(eventType)
                .eventVersion(EVENT_VERSION)
                .payload(objectMapper.writeValueAsString(envelope))
                .status(OutboxStatus.PENDING)
                .createdAt(now)
                .retryCount(0)
                .build();
    }

}
