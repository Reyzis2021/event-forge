package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.event.EventCancelledEvent;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventCancellationHandlerImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID KAFKA_EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID TICKET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock
    private TicketRepository ticketRepository;

    private EventCancellationHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new EventCancellationHandlerImpl(
                ticketRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void handle_whenEventCancelled_shouldCancelActiveTickets() {
        TicketEntity firstTicket = activeTicket("EF-1");
        TicketEntity secondTicket = activeTicket("EF-2");
        when(ticketRepository.findByEventIdAndStatus(EVENT_ID, TicketStatus.ACTIVE))
                .thenReturn(List.of(firstTicket, secondTicket));

        handler.handle(eventCancelledEvent());

        assertThat(firstTicket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(firstTicket.getUpdatedAt()).isEqualTo(NOW);
        assertThat(secondTicket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(secondTicket.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void handle_whenNoActiveTickets_shouldDoNothing() {
        when(ticketRepository.findByEventIdAndStatus(EVENT_ID, TicketStatus.ACTIVE))
                .thenReturn(List.of());

        handler.handle(eventCancelledEvent());

        assertThat(ticketRepository.findByEventIdAndStatus(EVENT_ID, TicketStatus.ACTIVE)).isEmpty();
    }

    private EventCancelledEvent eventCancelledEvent() {
        return new EventCancelledEvent(
                KAFKA_EVENT_ID,
                EVENT_ID,
                ORGANIZER_ID,
                "Venue unavailable"
        );
    }

    private TicketEntity activeTicket(String ticketNumber) {
        return TicketEntity.builder()
                .id(UUID.randomUUID())
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .eventId(EVENT_ID)
                .ticketTypeId(TICKET_TYPE_ID)
                .ticketNumber(ticketNumber)
                .status(TicketStatus.ACTIVE)
                .qrTokenHash("hash-" + ticketNumber)
                .issuedAt(NOW.minusSeconds(600))
                .createdAt(NOW.minusSeconds(600))
                .updatedAt(NOW.minusSeconds(600))
                .version(0L)
                .build();
    }
}
