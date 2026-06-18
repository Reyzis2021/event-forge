package com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Optional<EventEntity> findByIdAndOrganizerId(UUID id, UUID organizerId);

    @Query("""
            select e
            from EventEntity e
            where e.status = :status
              and (:city is null or lower(e.city) = lower(:city))
              and (:category is null or lower(e.category) = lower(:category))
              and (:from is null or e.startsAt >= :from)
              and (:to is null or e.startsAt <= :to)
            order by e.startsAt asc
            """)
    Page<EventEntity> searchPublishedEvents(
            @Param("status") EventStatus status,
            @Param("city") String city,
            @Param("category") String category,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    Page<EventEntity> findAllByStatus(EventStatus status, Pageable pageable);

    Page<EventEntity> findAllByStatusAndCityIgnoreCase(
            EventStatus status,
            String city,
            Pageable pageable
    );

    Page<EventEntity> findAllByStatusAndCategoryIgnoreCase(
            EventStatus status,
            String category,
            Pageable pageable
    );

    Page<EventEntity> findAllByStatusAndStartsAtBetween(
            EventStatus status,
            Instant from,
            Instant to,
            Pageable pageable
    );
}
