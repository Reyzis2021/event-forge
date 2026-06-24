package com.reyzarium.eventforge.bookingservice.application.event.outbox;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingConfirmedPayload(
        UUID bookingId,
        UUID userId,
        UUID eventId,
        UUID ticketTypeId,
        Integer quantity,
        BigDecimal amount,
        String currency
) {
}
