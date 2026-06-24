package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.ValidateTicketRequest;
import com.reyzarium.eventforge.ticketservice.api.ticket.dto.ValidateTicketResponse;
import com.reyzarium.eventforge.ticketservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.ticketservice.application.service.QrTokenService;
import com.reyzarium.eventforge.ticketservice.application.service.TicketCommandService;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TicketCommandServiceImpl implements TicketCommandService {

    private final TicketRepository ticketRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxMapper outboxMapper;
    private final QrTokenService qrTokenService;
    private final Clock clock;

    @Override
    @Transactional
    public ValidateTicketResponse validateTicket(ValidateTicketRequest request) {
        String qrTokenHash = qrTokenService.hash(request.qrToken());

        return ticketRepository.findByQrTokenHash(qrTokenHash)
                .map(this::validateExistingTicket)
                .orElseGet(() -> new ValidateTicketResponse(
                        null,
                        null,
                        null,
                        false,
                        "QR token is invalid"
                ));
    }

    private ValidateTicketResponse validateExistingTicket(TicketEntity ticket) {
        if (ticket.isUsed()) {
            return toResponse(ticket, false, "Ticket already used");
        }

        if (!ticket.isActive()) {
            return toResponse(ticket, false, "Ticket is not active");
        }

        Instant now = Instant.now(clock);
        ticket.markUsed(now);
        outboxEventRepository.save(outboxMapper.toTicketUsedOutbox(ticket, now));

        return toResponse(ticket, true, "Ticket is valid");
    }

    private ValidateTicketResponse toResponse(TicketEntity ticket, boolean valid, String message) {
        return new ValidateTicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getStatus(),
                valid,
                message
        );
    }
}
