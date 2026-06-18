package com.reyzarium.eventforge.eventservice.application.service;

import com.reyzarium.eventforge.eventservice.api.internal.dto.TicketTypeDetailsResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventListItemResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface EventQueryService {

    EventResponse getEvent(UUID eventId);

    PageResponse<EventListItemResponse> searchPublishedEvents(
            String city,
            String category,
            Instant from,
            Instant to,
            Pageable pageable
    );

    TicketTypeDetailsResponse getTicketTypeDetails(UUID eventId, UUID ticketTypeId);
}
