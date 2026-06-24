package com.reyzarium.eventforge.paymentservice.application.event.outbox;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreatedPayload(
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String providerInvoiceId
) {
}
