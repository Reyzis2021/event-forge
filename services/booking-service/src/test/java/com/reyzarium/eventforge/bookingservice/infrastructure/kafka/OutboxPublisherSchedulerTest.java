package com.reyzarium.eventforge.bookingservice.infrastructure.kafka;

import com.reyzarium.eventforge.bookingservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private BookingPublisher bookingPublisher;

    private OutboxPublisherScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxPublisherScheduler(
                outboxEventRepository,
                bookingPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void publishOutboxEvents_whenKafkaPublishSucceeds_shouldMarkEventPublished() {
        OutboxEventEntity event = pendingEvent(0);
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        scheduler.publishOutboxEvents();

        verify(bookingPublisher).publish(KafkaTopicNames.BOOKING_EVENTS, event.getAggregateId().toString(), event.getPayload());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void publishOutboxEvents_whenKafkaPublishFailsBeforeMaxRetries_shouldIncrementRetry() {
        OutboxEventEntity event = pendingEvent(0);
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new IllegalStateException("Kafka down"))
                .when(bookingPublisher)
                .publish(KafkaTopicNames.BOOKING_EVENTS, event.getAggregateId().toString(), event.getPayload());

        scheduler.publishOutboxEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("Kafka down");
    }

    @Test
    void publishOutboxEvents_whenKafkaPublishFailsAtMaxRetries_shouldMarkEventFailed() {
        OutboxEventEntity event = pendingEvent(2);
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new IllegalStateException("Kafka down"))
                .when(bookingPublisher)
                .publish(KafkaTopicNames.BOOKING_EVENTS, event.getAggregateId().toString(), event.getPayload());

        scheduler.publishOutboxEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
    }

    private OutboxEventEntity pendingEvent(int retryCount) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("BOOKING")
                .eventType("booking.created")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW.minusSeconds(60))
                .retryCount(retryCount)
                .build();
    }
}
