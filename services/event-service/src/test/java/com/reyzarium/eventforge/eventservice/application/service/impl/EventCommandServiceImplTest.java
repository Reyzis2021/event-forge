package com.reyzarium.eventforge.eventservice.application.service.impl;

import com.reyzarium.eventforge.eventservice.api.organizer.dto.CancelEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateTicketTypeRequest;
import com.reyzarium.eventforge.eventservice.application.mapper.EventMapper;
import com.reyzarium.eventforge.eventservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.eventservice.common.error.EventErrorCode;
import com.reyzarium.eventforge.eventservice.common.error.EventServiceException;
import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;
import com.reyzarium.eventforge.eventservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.EventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories.EventRepository;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxMapper outboxMapper;

    private EventCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        EventMapper eventMapper = Mappers.getMapper(EventMapper.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new EventCommandServiceImpl(
                eventRepository,
                eventMapper,
                clock,
                outboxEventRepository,
                outboxMapper
        );
    }

    @Test
    void createEvent_whenRequestIsValid_shouldSaveDraftEventAndCreatedOutbox() {
        CreateEventRequest request = validCreateRequest();
        OutboxEventEntity outboxEvent = outboxEvent();
        when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxMapper.toEventCreatedOutbox(any(EventEntity.class), eq(NOW))).thenReturn(outboxEvent);

        var response = service.createEvent(ORGANIZER_ID, request);

        ArgumentCaptor<EventEntity> eventCaptor = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        EventEntity savedEvent = eventCaptor.getValue();
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(response.eventId()).isEqualTo(savedEvent.getId());
        assertThat(savedEvent.getOrganizerId()).isEqualTo(ORGANIZER_ID);
        assertThat(savedEvent.getCapacity()).isEqualTo(100);
        assertThat(savedEvent.getAvailableForBooking()).isFalse();
        assertThat(savedEvent.getTicketTypes()).hasSize(1);
        assertThat(savedEvent.getTicketTypes().getFirst().getEvent()).isSameAs(savedEvent);
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void publishEvent_whenEventIsDraft_shouldPublishEventAndCreateOutbox() {
        EventEntity event = draftEvent();
        OutboxEventEntity outboxEvent = outboxEvent();
        when(eventRepository.findByIdAndOrganizerId(EVENT_ID, ORGANIZER_ID)).thenReturn(Optional.of(event));
        when(outboxMapper.toEventPublishedOutbox(event, NOW)).thenReturn(outboxEvent);

        var response = service.publishEvent(ORGANIZER_ID, EVENT_ID);

        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getAvailableForBooking()).isTrue();
        assertThat(event.getUpdatedAt()).isEqualTo(NOW);
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void publishEvent_whenEventAlreadyPublished_shouldThrowAlreadyPublished() {
        EventEntity event = draftEvent();
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findByIdAndOrganizerId(EVENT_ID, ORGANIZER_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publishEvent(ORGANIZER_ID, EVENT_ID))
                .isInstanceOfSatisfying(EventServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(EventErrorCode.EVENT_ALREADY_PUBLISHED));
    }

    @Test
    void cancelEvent_whenEventIsPublished_shouldCancelEventAndCreateOutbox() {
        EventEntity event = draftEvent();
        event.setStatus(EventStatus.PUBLISHED);
        event.setAvailableForBooking(true);
        OutboxEventEntity outboxEvent = outboxEvent();
        when(eventRepository.findByIdAndOrganizerId(EVENT_ID, ORGANIZER_ID)).thenReturn(Optional.of(event));
        when(outboxMapper.toEventCancelledOutbox(event, "Venue unavailable", NOW)).thenReturn(outboxEvent);

        var response = service.cancelEvent(ORGANIZER_ID, EVENT_ID, new CancelEventRequest("Venue unavailable"));

        assertThat(response.status()).isEqualTo(EventStatus.CANCELLED);
        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        assertThat(event.getAvailableForBooking()).isFalse();
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void cancelEvent_whenEventIsDraft_shouldThrowInvalidStatus() {
        when(eventRepository.findByIdAndOrganizerId(EVENT_ID, ORGANIZER_ID)).thenReturn(Optional.of(draftEvent()));

        assertThatThrownBy(() -> service.cancelEvent(ORGANIZER_ID, EVENT_ID, new CancelEventRequest("No venue")))
                .isInstanceOfSatisfying(EventServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(EventErrorCode.EVENT_INVALID_STATUS));
    }

    private CreateEventRequest validCreateRequest() {
        return new CreateEventRequest(
                "Java Backend Meetup",
                "Spring Boot and Kafka",
                "IT",
                "Sofia",
                "Tech Park",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                List.of(new CreateTicketTypeRequest("Regular", BigDecimal.valueOf(50), "EUR", 100))
        );
    }

    private EventEntity draftEvent() {
        EventEntity event = Mappers.getMapper(EventMapper.class).toDraftEntity(ORGANIZER_ID, validCreateRequest(), NOW);
        event.setId(EVENT_ID);
        return event;
    }

    private OutboxEventEntity outboxEvent() {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(EVENT_ID)
                .aggregateType("EVENT")
                .eventType("event.created")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW)
                .retryCount(0)
                .build();
    }
}
