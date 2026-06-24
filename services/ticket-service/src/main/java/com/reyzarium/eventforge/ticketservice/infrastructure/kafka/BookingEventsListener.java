package com.reyzarium.eventforge.ticketservice.infrastructure.kafka;

import com.reyzarium.eventforge.ticketservice.application.event.BookingConfirmedEvent;
import com.reyzarium.eventforge.ticketservice.application.service.TicketIssuanceService;
import com.reyzarium.eventforge.ticketservice.infrastructure.kafka.dto.BookingEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventsListener {

    private static final String BOOKING_CONFIRMED = "booking.confirmed";

    private final ObjectMapper objectMapper;
    private final TicketIssuanceService ticketIssuanceService;

    @KafkaListener(topics = KafkaTopicNames.BOOKING_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingEvent(String message) {
        BookingEventEnvelope envelope = objectMapper.readValue(message, BookingEventEnvelope.class);

        if (!BOOKING_CONFIRMED.equals(envelope.eventType())) {
            log.debug("Ignoring booking event type {}", envelope.eventType());
            return;
        }

        ticketIssuanceService.issueTickets(new BookingConfirmedEvent(
                envelope.eventId(),
                envelope.payload().bookingId(),
                envelope.payload().userId(),
                envelope.payload().eventId(),
                envelope.payload().ticketTypeId(),
                envelope.payload().quantity(),
                envelope.payload().amount(),
                envelope.payload().currency()
        ));
    }
}
