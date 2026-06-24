package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.event.BookingConfirmedEvent;
import com.reyzarium.eventforge.ticketservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.ticketservice.application.mapper.TicketMapper;
import com.reyzarium.eventforge.ticketservice.application.service.QrTokenService;
import com.reyzarium.eventforge.ticketservice.application.service.TicketNumberGenerator;
import com.reyzarium.eventforge.ticketservice.application.service.TicketPdfService;
import com.reyzarium.eventforge.ticketservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketIssuanceStatus;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketIssuanceLogEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketIssuanceLogRepository;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class TicketIssuanceServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID BOOKING_EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TICKET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketIssuanceLogRepository ticketIssuanceLogRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private TicketNumberGenerator ticketNumberGenerator;

    @Mock
    private TicketPdfService ticketPdfService;

    @Mock
    private QrTokenService qrTokenService;

    private TicketIssuanceServiceImpl service;

    @BeforeEach
    void setUp() {
        TicketMapper ticketMapper = Mappers.getMapper(TicketMapper.class);
        service = new TicketIssuanceServiceImpl(
                ticketRepository,
                ticketIssuanceLogRepository,
                outboxEventRepository,
                ticketMapper,
                outboxMapper,
                ticketNumberGenerator,
                ticketPdfService,
                qrTokenService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void issueTickets_whenBookingConfirmed_shouldCreateTicketsOutboxAndIssuanceLog() {
        when(ticketIssuanceLogRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
        when(ticketRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
        when(ticketNumberGenerator.generate()).thenReturn("EF-1", "EF-2");
        when(qrTokenService.generateToken()).thenReturn("token-1", "token-2");
        when(qrTokenService.hash("token-1")).thenReturn("hash-1");
        when(qrTokenService.hash("token-2")).thenReturn("hash-2");
        when(ticketPdfService.createFileKey(any(UUID.class))).thenAnswer(invocation -> "tickets/" + invocation.getArgument(0) + ".pdf");
        when(outboxMapper.toTicketIssuedOutbox(any(TicketEntity.class), eq(NOW))).thenReturn(outboxEvent());

        service.issueTickets(bookingConfirmedEvent());

        ArgumentCaptor<Iterable> ticketsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(ticketRepository).saveAll(ticketsCaptor.capture());
        List<TicketEntity> tickets = toList(ticketsCaptor.getValue());
        assertThat(tickets).hasSize(2);
        assertThat(tickets).extracting(TicketEntity::getTicketNumber).containsExactly("EF-1", "EF-2");
        assertThat(tickets).extracting(TicketEntity::getQrTokenHash).containsExactly("hash-1", "hash-2");
        assertThat(tickets).extracting(TicketEntity::getPdfFileKey).allSatisfy(pdfFileKey ->
                assertThat(pdfFileKey).asString().startsWith("tickets/").endsWith(".pdf"));
        assertThat(tickets).allSatisfy(ticket -> {
            assertThat(ticket.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(ticket.getUserId()).isEqualTo(USER_ID);
            assertThat(ticket.getEventId()).isEqualTo(EVENT_ID);
            assertThat(ticket.getTicketTypeId()).isEqualTo(TICKET_TYPE_ID);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
            assertThat(ticket.getIssuedAt()).isEqualTo(NOW);
        });

        verify(outboxEventRepository, times(2)).save(any(OutboxEventEntity.class));

        ArgumentCaptor<TicketIssuanceLogEntity> logCaptor = ArgumentCaptor.forClass(TicketIssuanceLogEntity.class);
        verify(ticketIssuanceLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(TicketIssuanceStatus.SUCCEEDED);
        assertThat(logCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void issueTickets_whenIssuanceLogAlreadyExists_shouldDoNothing() {
        when(ticketIssuanceLogRepository.existsByBookingId(BOOKING_ID)).thenReturn(true);

        service.issueTickets(bookingConfirmedEvent());

        verifyNoInteractions(ticketRepository, outboxEventRepository, outboxMapper, ticketNumberGenerator, ticketPdfService, qrTokenService);
    }

    @Test
    void issueTickets_whenTicketsAlreadyExist_shouldDoNothing() {
        when(ticketIssuanceLogRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
        when(ticketRepository.existsByBookingId(BOOKING_ID)).thenReturn(true);

        service.issueTickets(bookingConfirmedEvent());

        verifyNoInteractions(outboxEventRepository, outboxMapper, ticketNumberGenerator, ticketPdfService, qrTokenService);
    }

    private BookingConfirmedEvent bookingConfirmedEvent() {
        return new BookingConfirmedEvent(
                BOOKING_EVENT_ID,
                BOOKING_ID,
                USER_ID,
                EVENT_ID,
                TICKET_TYPE_ID,
                2,
                BigDecimal.valueOf(100),
                "EUR"
        );
    }

    private List<TicketEntity> toList(Iterable tickets) {
        return StreamSupport.stream(tickets.spliterator(), false)
                .map(TicketEntity.class::cast)
                .toList();
    }

    private OutboxEventEntity outboxEvent() {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("TICKET")
                .eventType("ticket.issued")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW)
                .retryCount(0)
                .build();
    }
}
