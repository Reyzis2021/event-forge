package com.reyzarium.eventforge.bookingservice.api.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingItemResponse(
        UUID ticketTypeId,
        Integer quantity,
        BigDecimal unitPrice,
        String currency
) {
}