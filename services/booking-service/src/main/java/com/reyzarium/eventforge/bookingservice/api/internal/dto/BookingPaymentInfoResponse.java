package com.reyzarium.eventforge.bookingservice.api.internal.dto;

import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingPaymentInfoResponse(
        UUID bookingId,
        UUID userId,
        BookingStatus status,
        BigDecimal amount,
        String currency,
        Instant expiresAt
) {
}
