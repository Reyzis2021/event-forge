package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.mapper.TicketResponseMapper;
import com.reyzarium.eventforge.ticketservice.common.error.TicketErrorCode;
import com.reyzarium.eventforge.ticketservice.common.error.TicketServiceException;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID TICKET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TICKET_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock
    private TicketRepository ticketRepository;

    private TicketQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        TicketResponseMapper ticketResponseMapper = Mappers.getMapper(TicketResponseMapper.class);
        service = new TicketQueryServiceImpl(ticketRepository, ticketResponseMapper);
    }

    @Test
    void getMyTickets_whenUserHasTickets_shouldReturnMappedTickets() {
        TicketEntity ticket = activeTicket("EF-1");
        when(ticketRepository.findByUserIdOrderByIssuedAtDesc(USER_ID)).thenReturn(List.of(ticket));

        var response = service.getMyTickets(USER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().ticketId()).isEqualTo(ticket.getId());
        assertThat(response.getFirst().bookingId()).isEqualTo(BOOKING_ID);
        assertThat(response.getFirst().eventId()).isEqualTo(EVENT_ID);
        assertThat(response.getFirst().ticketNumber()).isEqualTo("EF-1");
        assertThat(response.getFirst().status()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(response.getFirst().issuedAt()).isEqualTo(NOW);
    }

    @Test
    void getMyTickets_whenUserHasNoTickets_shouldReturnEmptyList() {
        when(ticketRepository.findByUserIdOrderByIssuedAtDesc(USER_ID)).thenReturn(List.of());

        var response = service.getMyTickets(USER_ID);

        assertThat(response).isEmpty();
    }

    @Test
    void getTicket_whenTicketBelongsToUser_shouldReturnTicket() {
        TicketEntity ticket = activeTicket("EF-1");
        ticket.setId(TICKET_ID);
        when(ticketRepository.findByIdAndUserId(TICKET_ID, USER_ID)).thenReturn(Optional.of(ticket));

        var response = service.getTicket(USER_ID, TICKET_ID);

        assertThat(response.ticketId()).isEqualTo(TICKET_ID);
        assertThat(response.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        assertThat(response.ticketNumber()).isEqualTo("EF-1");
        assertThat(response.status()).isEqualTo(TicketStatus.ACTIVE);
    }

    @Test
    void getTicket_whenTicketDoesNotBelongToUser_shouldThrowNotFound() {
        when(ticketRepository.findByIdAndUserId(TICKET_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicket(USER_ID, TICKET_ID))
                .isInstanceOfSatisfying(TicketServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_FOUND));
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
                .issuedAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .version(0L)
                .build();
    }
}
