package com.reyzarium.eventforge.eventservice.infrastructure.kafka;

import com.reyzarium.eventforge.eventservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.repositories.OutboxEventRepository;
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

    private final OutboxEventRepository outboxRepository;
    private final EventPublisher publisher;
    private final Clock clock;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {

        var events = outboxRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING
                );

        for (var event : events) {

            try {

                publisher.publish(
                        KafkaTopicNames.EVENT_EVENTS,
                        event.getAggregateId().toString(),
                        event.getPayload()
                );

                event.markPublished(Instant.now(clock));

            } catch (Exception exception) {

                log.error(
                        "Failed to publish outbox event {}",
                        event.getId(),
                        exception
                );

                event.incrementRetry(exception.getMessage());

                if (event.getRetryCount() >= 3) {
                    event.markFailed(exception.getMessage());
                }
            }
        }
    }
}
