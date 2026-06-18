package com.reyzarium.eventforge.bookingservice.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketTypeDetailsResponse(
        UUID eventId,
        UUID ticketTypeId,
        String status,
        BigDecimal price,
        String currency,
        Integer capacity,
        Instant startsAt
) {
}