package com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories;

import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentWebhookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhookEntity, UUID> {

    Optional<PaymentWebhookEntity> findByProviderAndProviderEventId(String provider, String providerEventId);
}
