package com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity;

import com.reyzarium.eventforge.paymentservice.domain.payment.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_invoice_id", nullable = false)
    private String providerInvoiceId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public void markSucceeded(Instant now) {
        this.status = PaymentStatus.SUCCEEDED;
        this.paidAt = now;
        this.updatedAt = now;
    }

    public void markFailed(Instant now) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = now;
        this.updatedAt = now;
    }
}
