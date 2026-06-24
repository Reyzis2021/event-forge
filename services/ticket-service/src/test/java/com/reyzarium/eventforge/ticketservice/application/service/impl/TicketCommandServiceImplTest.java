package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.ValidateTicketRequest;
import com.reyzarium.eventforge.ticketservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.ticketservice.application.service.QrTokenService;
import com.reyzarium.eventforge.ticketservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID TICKET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TICKET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final String QR_TOKEN = "raw-token";
    private static final String QR_TOKEN_HASH = "hashed-token";

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private QrTokenService qrTokenService;

    private TicketCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketCommandServiceImpl(
                ticketRepository,
                outboxEventRepository,
                outboxMapper,
                qrTokenService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void validateTicket_whenTicketIsActive_shouldMarkUsedAndCreateOutbox() {
        TicketEntity ticket = ticket(TicketStatus.ACTIVE);
        OutboxEventEntity outboxEvent = outboxEvent();
        when(qrTokenService.hash(QR_TOKEN)).thenReturn(QR_TOKEN_HASH);
        when(ticketRepository.findByQrTokenHash(QR_TOKEN_HASH)).thenReturn(Optional.of(ticket));
        when(outboxMapper.toTicketUsedOutbox(ticket, NOW)).thenReturn(outboxEvent);

        var response = service.validateTicket(new ValidateTicketRequest(QR_TOKEN));

        assertThat(response.valid()).isTrue();
        assertThat(response.message()).isEqualTo("Ticket is valid");
        assertThat(response.ticketId()).isEqualTo(TICKET_ID);
        assertThat(response.ticketNumber()).isEqualTo("EF-1");
        assertThat(response.status()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getUsedAt()).isEqualTo(NOW);
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void validateTicket_whenTicketIsAlreadyUsed_shouldReturnInvalidWithoutChangingUsedAt() {
        Instant usedAt = NOW.minusSeconds(60);
        TicketEntity ticket = ticket(TicketStatus.USED);
        ticket.setUsedAt(usedAt);
        when(qrTokenService.hash(QR_TOKEN)).thenReturn(QR_TOKEN_HASH);
        when(ticketRepository.findByQrTokenHash(QR_TOKEN_HASH)).thenReturn(Optional.of(ticket));

        var response = service.validateTicket(new ValidateTicketRequest(QR_TOKEN));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).isEqualTo("Ticket already used");
        assertThat(response.status()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getUsedAt()).isEqualTo(usedAt);
        verifyNoInteractions(outboxEventRepository, outboxMapper);
    }

    @Test
    void validateTicket_whenQrTokenIsUnknown_shouldReturnInvalid() {
        when(qrTokenService.hash(QR_TOKEN)).thenReturn(QR_TOKEN_HASH);
        when(ticketRepository.findByQrTokenHash(QR_TOKEN_HASH)).thenReturn(Optional.empty());

        var response = service.validateTicket(new ValidateTicketRequest(QR_TOKEN));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).isEqualTo("QR token is invalid");
        assertThat(response.ticketId()).isNull();
        assertThat(response.ticketNumber()).isNull();
        assertThat(response.status()).isNull();
        verifyNoInteractions(outboxEventRepository, outboxMapper);
    }

    @Test
    void validateTicket_whenTicketIsCancelled_shouldReturnInvalidWithoutOutbox() {
        TicketEntity ticket = ticket(TicketStatus.CANCELLED);
        when(qrTokenService.hash(QR_TOKEN)).thenReturn(QR_TOKEN_HASH);
        when(ticketRepository.findByQrTokenHash(QR_TOKEN_HASH)).thenReturn(Optional.of(ticket));

        var response = service.validateTicket(new ValidateTicketRequest(QR_TOKEN));

        assertThat(response.valid()).isFalse();
        assertThat(response.message()).isEqualTo("Ticket is not active");
        assertThat(response.status()).isEqualTo(TicketStatus.CANCELLED);
        verifyNoInteractions(outboxEventRepository, outboxMapper);
    }

    private TicketEntity ticket(TicketStatus status) {
        return TicketEntity.builder()
                .id(TICKET_ID)
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .eventId(EVENT_ID)
                .ticketTypeId(TICKET_TYPE_ID)
                .ticketNumber("EF-1")
                .status(status)
                .qrTokenHash(QR_TOKEN_HASH)
                .issuedAt(NOW.minusSeconds(600))
                .createdAt(NOW.minusSeconds(600))
                .updatedAt(NOW.minusSeconds(600))
                .version(0L)
                .build();
    }

    private OutboxEventEntity outboxEvent() {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(TICKET_ID)
                .aggregateType("TICKET")
                .eventType("ticket.used")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW)
                .retryCount(0)
                .build();
    }
}
