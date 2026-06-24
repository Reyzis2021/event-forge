package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.event.BookingConfirmedEvent;
import com.reyzarium.eventforge.ticketservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.ticketservice.application.mapper.TicketMapper;
import com.reyzarium.eventforge.ticketservice.application.service.QrTokenService;
import com.reyzarium.eventforge.ticketservice.application.service.TicketIssuanceService;
import com.reyzarium.eventforge.ticketservice.application.service.TicketNumberGenerator;
import com.reyzarium.eventforge.ticketservice.application.service.TicketPdfService;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketIssuanceStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketIssuanceLogEntity;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketIssuanceLogRepository;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketIssuanceServiceImpl implements TicketIssuanceService {

    private final TicketRepository ticketRepository;
    private final TicketIssuanceLogRepository ticketIssuanceLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TicketMapper ticketMapper;
    private final OutboxMapper outboxMapper;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final TicketPdfService ticketPdfService;
    private final QrTokenService qrTokenService;
    private final Clock clock;

    @Override
    @Transactional
    public void issueTickets(BookingConfirmedEvent event) {
        if (ticketIssuanceLogRepository.existsByBookingId(event.bookingId())
                || ticketRepository.existsByBookingId(event.bookingId())) {
            return;
        }

        Instant now = Instant.now(clock);
        List<TicketEntity> tickets = createTickets(event, now);

        ticketRepository.saveAll(tickets);
        tickets.forEach(ticket -> outboxEventRepository.save(outboxMapper.toTicketIssuedOutbox(ticket, now)));
        ticketIssuanceLogRepository.save(TicketIssuanceLogEntity.builder()
                .id(UUID.randomUUID())
                .bookingId(event.bookingId())
                .status(TicketIssuanceStatus.SUCCEEDED)
                .createdAt(now)
                .build());
    }

    private List<TicketEntity> createTickets(BookingConfirmedEvent event, Instant now) {
        List<TicketEntity> tickets = new ArrayList<>();

        for (int i = 0; i < event.quantity(); i++) {
            String rawQrToken = qrTokenService.generateToken();
            TicketEntity ticket = ticketMapper.toActiveTicket(
                    event,
                    ticketNumberGenerator.generate(),
                    qrTokenService.hash(rawQrToken),
                    now
            );
            ticket.setPdfFileKey(ticketPdfService.createFileKey(ticket.getId()));
            tickets.add(ticket);
        }

        return tickets;
    }
}
