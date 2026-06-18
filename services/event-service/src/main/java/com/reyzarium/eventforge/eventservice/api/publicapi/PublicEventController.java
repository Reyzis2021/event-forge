package com.reyzarium.eventforge.eventservice.api.publicapi;

import com.reyzarium.eventforge.eventservice.application.service.EventQueryService;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventListItemResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class PublicEventController {

    private final EventQueryService eventQueryService;

    @GetMapping
    public PageResponse<EventListItemResponse> searchEvents(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "startsAt") Pageable pageable
    ) {
        return eventQueryService.searchPublishedEvents(city, category, from, to, pageable);
    }

    @GetMapping("/{eventId}")
    public EventResponse getEvent(@PathVariable UUID eventId) {
        return eventQueryService.getEvent(eventId);
    }
}
