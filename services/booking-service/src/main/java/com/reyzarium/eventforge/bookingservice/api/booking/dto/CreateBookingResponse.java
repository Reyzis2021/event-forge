package com.reyzarium.eventforge.bookingservice.api.booking.dto;

import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateBookingResponse(
        UUID bookingId,
        BookingStatus status,
        Instant expiresAt,
        BigDecimal amount,
        String currency
) {
}
