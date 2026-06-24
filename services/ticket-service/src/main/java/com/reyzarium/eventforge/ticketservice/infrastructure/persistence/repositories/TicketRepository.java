package com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

    boolean existsByBookingId(UUID bookingId);

    List<TicketEntity> findByUserIdOrderByIssuedAtDesc(UUID userId);

    Optional<TicketEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<TicketEntity> findByQrTokenHash(String qrTokenHash);

    List<TicketEntity> findByEventIdAndStatus(UUID eventId, TicketStatus status);
}
