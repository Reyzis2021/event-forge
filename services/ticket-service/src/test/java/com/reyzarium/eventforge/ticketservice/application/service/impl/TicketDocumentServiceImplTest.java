package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.service.TicketPdfService;
import com.reyzarium.eventforge.ticketservice.common.error.TicketErrorCode;
import com.reyzarium.eventforge.ticketservice.common.error.TicketServiceException;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketDocumentServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID TICKET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TICKET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final String PDF_FILE_KEY = "tickets/00000000-0000-0000-0000-000000000001.pdf";
    private static final String PDF_URL = "http://localhost:8084/mock-files/" + PDF_FILE_KEY;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketPdfService ticketPdfService;

    private TicketDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketDocumentServiceImpl(ticketRepository, ticketPdfService);
    }

    @Test
    void getTicketPdf_whenTicketBelongsToUserAndPdfExists_shouldReturnPdfUrl() {
        TicketEntity ticket = activeTicket();
        when(ticketRepository.findByIdAndUserId(TICKET_ID, USER_ID)).thenReturn(Optional.of(ticket));
        when(ticketPdfService.createPdfUrl(PDF_FILE_KEY)).thenReturn(PDF_URL);

        var response = service.getTicketPdf(USER_ID, TICKET_ID);

        assertThat(response.ticketId()).isEqualTo(TICKET_ID);
        assertThat(response.pdfUrl()).isEqualTo(PDF_URL);
    }

    @Test
    void getTicketPdf_whenTicketDoesNotBelongToUser_shouldThrowNotFound() {
        when(ticketRepository.findByIdAndUserId(TICKET_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicketPdf(USER_ID, TICKET_ID))
                .isInstanceOfSatisfying(TicketServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    void getTicketPdf_whenPdfFileKeyIsMissing_shouldThrowPdfGenerationFailed() {
        TicketEntity ticket = activeTicket();
        ticket.setPdfFileKey(null);
        when(ticketRepository.findByIdAndUserId(TICKET_ID, USER_ID)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getTicketPdf(USER_ID, TICKET_ID))
                .isInstanceOfSatisfying(TicketServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(TicketErrorCode.PDF_GENERATION_FAILED));
    }

    private TicketEntity activeTicket() {
        return TicketEntity.builder()
                .id(TICKET_ID)
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .eventId(EVENT_ID)
                .ticketTypeId(TICKET_TYPE_ID)
                .ticketNumber("EF-1")
                .status(TicketStatus.ACTIVE)
                .qrTokenHash("hash-1")
                .pdfFileKey(PDF_FILE_KEY)
                .issuedAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .version(0L)
                .build();
    }
}
