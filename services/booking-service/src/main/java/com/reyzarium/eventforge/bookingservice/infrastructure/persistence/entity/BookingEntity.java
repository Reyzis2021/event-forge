package com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity;

import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bookings")
public class BookingEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingItemEntity> items = new ArrayList<>();

    public void addItem(BookingItemEntity item) {
        items.add(item);
        item.setBooking(this);
    }

    public void markExpired(Instant now) {
        this.status = BookingStatus.EXPIRED;
        this.updatedAt = now;
    }

    public void markConfirmed(Instant now) {
        this.status = BookingStatus.CONFIRMED;
        this.updatedAt = now;
    }

    public void markCancelled(Instant now) {
        this.status = BookingStatus.CANCELLED;
        this.updatedAt = now;
    }

    public void markPaymentFailed(Instant now) {
        this.status = BookingStatus.PAYMENT_FAILED;
        this.updatedAt = now;
    }

    public boolean isPendingPayment() {
        return status == BookingStatus.PENDING_PAYMENT;
    }
}
