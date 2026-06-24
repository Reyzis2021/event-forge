package com.reyzarium.eventforge.ticketservice.infrastructure.kafka.dto;

import java.util.UUID;

public record EventEventPayload(
        UUID eventId,
        UUID organizerId,
        String reason
) {
}
