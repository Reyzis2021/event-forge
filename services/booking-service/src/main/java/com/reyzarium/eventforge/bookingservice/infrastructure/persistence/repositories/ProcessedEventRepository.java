package com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
