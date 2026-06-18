package com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    List<BookingEntity> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            BookingStatus status,
            Instant now
    );

    Optional<BookingEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<BookingEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
