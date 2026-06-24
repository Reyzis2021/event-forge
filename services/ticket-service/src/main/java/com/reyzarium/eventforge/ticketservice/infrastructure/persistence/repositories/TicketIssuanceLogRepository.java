package com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketIssuanceLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketIssuanceLogRepository extends JpaRepository<TicketIssuanceLogEntity, UUID> {

    boolean existsByBookingId(UUID bookingId);
}
