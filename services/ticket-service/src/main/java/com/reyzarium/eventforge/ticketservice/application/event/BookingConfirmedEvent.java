package com.reyzarium.eventforge.ticketservice.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID eventId,
        UUID bookingId,
        UUID userId,
        UUID businessEventId,
        UUID ticketTypeId,
        Integer quantity,
        BigDecimal amount,
        String currency
) {
}
