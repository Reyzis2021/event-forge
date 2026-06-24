package com.reyzarium.eventforge.bookingservice.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID eventId,
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        Instant paidAt
) {
}
