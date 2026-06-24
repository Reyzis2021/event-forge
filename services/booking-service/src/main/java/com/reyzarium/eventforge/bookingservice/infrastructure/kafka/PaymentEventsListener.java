package com.reyzarium.eventforge.bookingservice.infrastructure.kafka;

import com.reyzarium.eventforge.bookingservice.application.event.PaymentSucceededEvent;
import com.reyzarium.eventforge.bookingservice.application.event.PaymentFailedEvent;
import com.reyzarium.eventforge.bookingservice.application.service.PaymentFailedEventHandler;
import com.reyzarium.eventforge.bookingservice.application.service.PaymentSucceededEventHandler;
import com.reyzarium.eventforge.bookingservice.infrastructure.kafka.dto.PaymentEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsListener {

    private static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    private static final String PAYMENT_FAILED = "payment.failed";

    private final ObjectMapper objectMapper;
    private final PaymentSucceededEventHandler paymentSucceededEventHandler;
    private final PaymentFailedEventHandler paymentFailedEventHandler;

    @KafkaListener(topics = KafkaTopicNames.PAYMENT_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(String message) {
        PaymentEventEnvelope envelope = objectMapper.readValue(message, PaymentEventEnvelope.class);

        if (PAYMENT_SUCCEEDED.equals(envelope.eventType())) {
            paymentSucceededEventHandler.handle(new PaymentSucceededEvent(
                    envelope.eventId(),
                    envelope.payload().paymentId(),
                    envelope.payload().bookingId(),
                    envelope.payload().userId(),
                    envelope.payload().amount(),
                    envelope.payload().currency(),
                    envelope.payload().paidAt()
            ));
            return;
        }

        if (PAYMENT_FAILED.equals(envelope.eventType())) {
            paymentFailedEventHandler.handle(new PaymentFailedEvent(
                    envelope.eventId(),
                    envelope.payload().paymentId(),
                    envelope.payload().bookingId(),
                    envelope.payload().userId(),
                    envelope.payload().amount(),
                    envelope.payload().currency(),
                    envelope.payload().failedAt()
            ));
            return;
        }

        log.debug("Ignoring payment event type {}", envelope.eventType());
    }
}
