package com.reyzarium.eventforge.eventservice.application.service.impl;

import com.reyzarium.eventforge.eventservice.api.organizer.dto.CancelEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CancelEventResponse;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventResponse;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.PublishEventResponse;
import com.reyzarium.eventforge.eventservice.application.mapper.EventMapper;
import com.reyzarium.eventforge.eventservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.eventservice.application.service.EventCommandService;
import com.reyzarium.eventforge.eventservice.common.error.EventErrorCode;
import com.reyzarium.eventforge.eventservice.common.error.EventServiceException;
import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.EventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventCommandServiceImpl implements EventCommandService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final Clock clock;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxMapper outboxMapper;

    @Override
    @Transactional
    public CreateEventResponse createEvent(UUID organizerId, CreateEventRequest request) {
        Instant now = Instant.now(clock);
        EventEntity event = eventMapper.toDraftEntity(organizerId, request, now);
        EventEntity savedEvent = eventRepository.save(event);
        outboxEventRepository.save(outboxMapper.toEventCreatedOutbox(savedEvent, now));

        return new CreateEventResponse(savedEvent.getId(), savedEvent.getStatus());
    }

    @Override
    @Transactional
    public PublishEventResponse publishEvent(UUID organizerId, UUID eventId) {
        EventEntity event = eventRepository.findByIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new EventServiceException(
                        EventErrorCode.EVENT_NOT_FOUND,
                        "Event not found"
                ));

        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new EventServiceException(
                    EventErrorCode.EVENT_ALREADY_PUBLISHED,
                    "Event already published"
            );
        }

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new EventServiceException(
                    EventErrorCode.EVENT_INVALID_STATUS,
                    "Only DRAFT event can be published"
            );
        }

        Instant now = Instant.now(clock);

        event.setStatus(EventStatus.PUBLISHED);
        event.setAvailableForBooking(true);
        event.setUpdatedAt(now);

        OutboxEventEntity outboxEvent = outboxMapper.toEventPublishedOutbox(event, now);
        outboxEventRepository.save(outboxEvent);

        return new PublishEventResponse(event.getId(), event.getStatus());
    }

    @Override
    @Transactional
    public CancelEventResponse cancelEvent(UUID organizerId, UUID eventId, CancelEventRequest request) {
        EventEntity event = eventRepository.findByIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new EventServiceException(
                        EventErrorCode.EVENT_NOT_FOUND,
                        "Event not found"
                ));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new EventServiceException(
                    EventErrorCode.EVENT_ALREADY_CANCELLED,
                    "Event already cancelled"
            );
        }

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new EventServiceException(
                    EventErrorCode.EVENT_INVALID_STATUS,
                    "Only PUBLISHED event can be cancelled"
            );
        }

        Instant now = Instant.now(clock);
        event.setStatus(EventStatus.CANCELLED);
        event.setAvailableForBooking(false);
        event.setUpdatedAt(now);

        outboxEventRepository.save(outboxMapper.toEventCancelledOutbox(event, request.reason(), now));

        return new CancelEventResponse(event.getId(), event.getStatus());
    }
}
