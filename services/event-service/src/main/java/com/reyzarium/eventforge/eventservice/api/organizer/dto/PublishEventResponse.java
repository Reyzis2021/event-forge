package com.reyzarium.eventforge.eventservice.api.organizer.dto;

import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;

import java.util.UUID;

public record PublishEventResponse(
        UUID eventId,
        EventStatus status
) {
}