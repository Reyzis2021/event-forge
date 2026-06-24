package com.reyzarium.eventforge.ticketservice.application.event;

import java.util.UUID;

public record EventCancelledEvent(
        UUID eventId,
        UUID businessEventId,
        UUID organizerId,
        String reason
) {
}
