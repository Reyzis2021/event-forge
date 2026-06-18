package com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TicketTypeInventoryRepository extends JpaRepository<TicketTypeInventoryEntity, TicketTypeInventoryId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select inventory
            from TicketTypeInventoryEntity inventory
            where inventory.id.eventId = :eventId
              and inventory.id.ticketTypeId = :ticketTypeId
            """)
    Optional<TicketTypeInventoryEntity> findByIdForUpdate(
            @Param("eventId") UUID eventId,
            @Param("ticketTypeId") UUID ticketTypeId
    );
}
