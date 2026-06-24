package com.reyzarium.eventforge.ticketservice.infrastructure.kafka;

import com.reyzarium.eventforge.ticketservice.application.event.EventCancelledEvent;
import com.reyzarium.eventforge.ticketservice.application.service.EventCancellationHandler;
import com.reyzarium.eventforge.ticketservice.infrastructure.kafka.dto.EventEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventEventsListener {

    private static final String EVENT_CANCELLED = "event.cancelled";

    private final ObjectMapper objectMapper;
    private final EventCancellationHandler eventCancellationHandler;

    @KafkaListener(topics = KafkaTopicNames.EVENT_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void onEventEvent(String message) {
        EventEventEnvelope envelope = objectMapper.readValue(message, EventEventEnvelope.class);

        if (!EVENT_CANCELLED.equals(envelope.eventType())) {
            log.debug("Ignoring event event type {}", envelope.eventType());
            return;
        }

        eventCancellationHandler.handle(new EventCancelledEvent(
                envelope.eventId(),
                envelope.payload().eventId(),
                envelope.payload().organizerId(),
                envelope.payload().reason()
        ));
    }
}
