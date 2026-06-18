package com.reyzarium.eventforge.eventservice.api.publicapi.dto;

import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID eventId,
        String title,
        String description,
        String category,
        String city,
        String location,
        Instant startsAt,
        Instant endsAt,
        EventStatus status,
        Integer capacity,
        List<TicketTypeResponse> ticketTypes
) {
}