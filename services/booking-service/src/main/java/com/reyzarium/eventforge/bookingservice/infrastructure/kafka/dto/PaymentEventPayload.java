package com.reyzarium.eventforge.bookingservice.infrastructure.kafka.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEventPayload(
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        Instant paidAt,
        Instant failedAt
) {
}
