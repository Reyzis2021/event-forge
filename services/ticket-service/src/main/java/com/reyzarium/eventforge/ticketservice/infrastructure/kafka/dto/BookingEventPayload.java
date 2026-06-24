package com.reyzarium.eventforge.ticketservice.infrastructure.kafka.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingEventPayload(
        UUID bookingId,
        UUID userId,
        UUID eventId,
        UUID ticketTypeId,
        Integer quantity,
        BigDecimal amount,
        String currency
) {
}
