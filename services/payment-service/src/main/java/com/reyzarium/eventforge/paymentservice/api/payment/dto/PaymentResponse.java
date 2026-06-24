package com.reyzarium.eventforge.paymentservice.api.payment.dto;

import com.reyzarium.eventforge.paymentservice.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID bookingId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String providerInvoiceId,
        Instant createdAt,
        Instant paidAt
) {
}
