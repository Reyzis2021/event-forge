package com.reyzarium.eventforge.eventservice.api.organizer;

import com.reyzarium.eventforge.eventservice.api.organizer.dto.CancelEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CancelEventResponse;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventResponse;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.PublishEventResponse;
import com.reyzarium.eventforge.eventservice.application.service.EventCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/organizer/events")
public class OrganizerEventController {

    private final EventCommandService eventCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateEventResponse createEvent(@RequestHeader("X-Organizer-Id") UUID organizerId,
                                           @Valid @RequestBody CreateEventRequest request) {
        return eventCommandService.createEvent(organizerId, request);
    }

    @PostMapping("/{eventId}/publish")
    public PublishEventResponse publishEvent(@RequestHeader("X-Organizer-Id") UUID organizerId,
                                             @PathVariable UUID eventId) {
        return eventCommandService.publishEvent(organizerId, eventId);
    }

    @PostMapping("/{eventId}/cancel")
    public CancelEventResponse cancelEvent(@RequestHeader("X-Organizer-Id") UUID organizerId,
                                           @PathVariable UUID eventId,
                                           @Valid @RequestBody CancelEventRequest request) {
        return eventCommandService.cancelEvent(organizerId, eventId, request);
    }
}
