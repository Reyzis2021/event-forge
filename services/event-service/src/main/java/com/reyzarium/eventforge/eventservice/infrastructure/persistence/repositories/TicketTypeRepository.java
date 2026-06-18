package com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.TicketTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketTypeEntity, UUID> {

    Optional<TicketTypeEntity> findByIdAndEventId(UUID id, UUID eventId);
}
