package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.TicketResponse;
import com.reyzarium.eventforge.ticketservice.application.mapper.TicketResponseMapper;
import com.reyzarium.eventforge.ticketservice.application.service.TicketQueryService;
import com.reyzarium.eventforge.ticketservice.common.error.TicketErrorCode;
import com.reyzarium.eventforge.ticketservice.common.error.TicketServiceException;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketRepository ticketRepository;
    private final TicketResponseMapper ticketResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(UUID userId) {
        return ticketRepository.findByUserIdOrderByIssuedAtDesc(userId)
                .stream()
                .map(ticketResponseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID userId, UUID ticketId) {
        return ticketRepository.findByIdAndUserId(ticketId, userId)
                .map(ticketResponseMapper::toResponse)
                .orElseThrow(() -> new TicketServiceException(
                        TicketErrorCode.TICKET_NOT_FOUND,
                        "Ticket not found"
                ));
    }
}
