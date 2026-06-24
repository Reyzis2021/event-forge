package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.TicketPdfResponse;
import com.reyzarium.eventforge.ticketservice.application.service.TicketDocumentService;
import com.reyzarium.eventforge.ticketservice.application.service.TicketPdfService;
import com.reyzarium.eventforge.ticketservice.common.error.TicketErrorCode;
import com.reyzarium.eventforge.ticketservice.common.error.TicketServiceException;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketDocumentServiceImpl implements TicketDocumentService {

    private final TicketRepository ticketRepository;
    private final TicketPdfService ticketPdfService;

    @Override
    @Transactional(readOnly = true)
    public TicketPdfResponse getTicketPdf(UUID userId, UUID ticketId) {
        TicketEntity ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new TicketServiceException(
                        TicketErrorCode.TICKET_NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getPdfFileKey() == null || ticket.getPdfFileKey().isBlank()) {
            throw new TicketServiceException(
                    TicketErrorCode.PDF_GENERATION_FAILED,
                    "Ticket PDF is not available"
            );
        }

        return new TicketPdfResponse(
                ticket.getId(),
                ticketPdfService.createPdfUrl(ticket.getPdfFileKey())
        );
    }
}
