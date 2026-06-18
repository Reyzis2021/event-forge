package com.reyzarium.eventforge.eventservice.application.service;

import com.reyzarium.eventforge.eventservice.api.organizer.dto.CancelEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CancelEventResponse;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventResponse;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.PublishEventResponse;

import java.util.UUID;

public interface EventCommandService {

    CreateEventResponse createEvent(UUID organizerId, CreateEventRequest request);

    PublishEventResponse publishEvent(UUID organizerId, UUID eventId);

    CancelEventResponse cancelEvent(UUID organizerId, UUID eventId, CancelEventRequest request);
}
