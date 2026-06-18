package com.reyzarium.eventforge.eventservice.application.service.impl;

import com.reyzarium.eventforge.eventservice.api.internal.dto.TicketTypeDetailsResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventListItemResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.PageResponse;
import com.reyzarium.eventforge.eventservice.application.mapper.EventResponseMapper;
import com.reyzarium.eventforge.eventservice.application.service.EventQueryService;
import com.reyzarium.eventforge.eventservice.common.error.EventErrorCode;
import com.reyzarium.eventforge.eventservice.common.error.EventServiceException;
import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories.EventRepository;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventQueryServiceImpl implements EventQueryService {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventResponseMapper eventResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .map(eventResponseMapper::toResponse)
                .orElseThrow(() -> new EventServiceException(
                        EventErrorCode.EVENT_NOT_FOUND,
                        "Event not found"
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EventListItemResponse> searchPublishedEvents(
            String city,
            String category,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        var page = eventRepository.searchPublishedEvents(
                EventStatus.PUBLISHED,
                normalize(city),
                normalize(category),
                from,
                to,
                pageable
        );

        var mappedPage = page.map(eventResponseMapper::toListItemResponse);
        return eventResponseMapper.toPageResponse(mappedPage);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketTypeDetailsResponse getTicketTypeDetails(UUID eventId, UUID ticketTypeId) {
        var ticketType = ticketTypeRepository.findByIdAndEventId(ticketTypeId, eventId)
                .orElseThrow(() -> new EventServiceException(
                        EventErrorCode.TICKET_TYPE_NOT_FOUND,
                        "Ticket type not found"
                ));
        var event = ticketType.getEvent();

        return new TicketTypeDetailsResponse(
                event.getId(),
                ticketType.getId(),
                event.getStatus(),
                ticketType.getPrice(),
                ticketType.getCurrency(),
                ticketType.getCapacity(),
                event.getStartsAt()
        );
    }
}
