package com.reyzarium.eventforge.eventservice.api.publicapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketTypeResponse(
        UUID ticketTypeId,
        String name,
        BigDecimal price,
        String currency,
        Integer capacity
) {
}