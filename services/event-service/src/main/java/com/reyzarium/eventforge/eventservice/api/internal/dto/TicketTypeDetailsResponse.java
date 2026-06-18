package com.reyzarium.eventforge.eventservice.api.internal.dto;

import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketTypeDetailsResponse(
        UUID eventId,
        UUID ticketTypeId,
        EventStatus status,
        BigDecimal price,
        String currency,
        Integer capacity,
        Instant startsAt
) {
}
