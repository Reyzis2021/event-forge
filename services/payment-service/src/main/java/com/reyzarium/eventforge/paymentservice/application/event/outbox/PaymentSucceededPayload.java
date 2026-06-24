package com.reyzarium.eventforge.paymentservice.application.event.outbox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededPayload(
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        Instant paidAt
) {
}
