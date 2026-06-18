package com.reyzarium.eventforge.eventservice.api.publicapi.dto;

import java.time.Instant;
import java.util.UUID;

public record EventListItemResponse(
        UUID eventId,
        String title,
        String category,
        String city,
        String location,
        Instant startsAt,
        Instant endsAt,
        Integer capacity
) {
}