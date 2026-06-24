package com.reyzarium.eventforge.ticketservice.common.error;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}
