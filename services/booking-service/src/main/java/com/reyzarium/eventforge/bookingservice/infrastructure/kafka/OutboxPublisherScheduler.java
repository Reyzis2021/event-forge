package com.reyzarium.eventforge.bookingservice.infrastructure.kafka;

import com.reyzarium.eventforge.bookingservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final BookingPublisher bookingPublisher;
    private final Clock clock;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        var events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (var event : events) {
            try {
                bookingPublisher.publish(
                        KafkaTopicNames.BOOKING_EVENTS,
                        event.getAggregateId().toString(),
                        event.getPayload()
                );
                event.markPublished(Instant.now(clock));
            } catch (Exception exception) {
                log.error("Failed to publish booking outbox event {}", event.getId(), exception);
                event.incrementRetry(exception.getMessage());

                if (event.getRetryCount() >= 3) {
                    event.markFailed(exception.getMessage());
                }
            }
        }
    }
}
