package com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.ticketservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
