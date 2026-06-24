package com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<PaymentEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    Optional<PaymentEntity> findByProviderInvoiceId(String providerInvoiceId);
}
