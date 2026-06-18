package com.reyzarium.eventforge.bookingservice.api.booking.dto;

import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID bookingId,
        UUID userId,
        UUID eventId,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        Instant expiresAt,
        List<BookingItemResponse> items
) {
}