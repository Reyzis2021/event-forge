package com.reyzarium.eventforge.paymentservice.common.error;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}
