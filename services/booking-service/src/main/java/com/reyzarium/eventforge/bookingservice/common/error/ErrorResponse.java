package com.reyzarium.eventforge.bookingservice.common.error;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}