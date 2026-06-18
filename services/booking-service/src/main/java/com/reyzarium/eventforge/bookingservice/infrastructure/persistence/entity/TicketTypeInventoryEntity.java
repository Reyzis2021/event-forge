package com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity;

import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ticket_type_inventory")
public class TicketTypeInventoryEntity {

    @EmbeddedId
    private TicketTypeInventoryId id;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer reserved;

    @Column(nullable = false)
    private Integer sold;

    @Version
    @Column(nullable = false)
    private Long version;

    public int available() {
        return capacity - reserved - sold;
    }

    public void reserve(int quantity) {
        if (available() < quantity) {
            throw new BookingServiceException(
                    BookingErrorCode.INSUFFICIENT_TICKETS,
                    "Not enough tickets available"
            );
        }
        reserved += quantity;
    }

    public void releaseReserved(int quantity) {
        reserved = Math.max(0, reserved - quantity);
    }

    public void confirmReserved(int quantity) {
        releaseReserved(quantity);
        sold += quantity;
    }
}
