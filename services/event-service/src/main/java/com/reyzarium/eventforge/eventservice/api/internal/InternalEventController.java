package com.reyzarium.eventforge.eventservice.api.internal;

import com.reyzarium.eventforge.eventservice.api.internal.dto.TicketTypeDetailsResponse;
import com.reyzarium.eventforge.eventservice.application.service.EventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/events")
public class InternalEventController {

    private final EventQueryService eventQueryService;

    @GetMapping("/{eventId}/ticket-types/{ticketTypeId}")
    public TicketTypeDetailsResponse getTicketType(@PathVariable UUID eventId,
                                                   @PathVariable UUID ticketTypeId) {
        return eventQueryService.getTicketTypeDetails(eventId, ticketTypeId);
    }
}
