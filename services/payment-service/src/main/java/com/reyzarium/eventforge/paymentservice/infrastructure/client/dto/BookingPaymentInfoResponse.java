package com.reyzarium.eventforge.paymentservice.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingPaymentInfoResponse(
        UUID bookingId,
        UUID userId,
        String status,
        BigDecimal amount,
        String currency,
        Instant expiresAt
) {
}
