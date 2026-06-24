package com.reyzarium.eventforge.ticketservice.infrastructure.kafka;

import com.reyzarium.eventforge.ticketservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.repositories.OutboxEventRepository;
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
    private final TicketPublisher ticketPublisher;
    private final Clock clock;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        var events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (var event : events) {
            try {
                ticketPublisher.publish(
                        KafkaTopicNames.TICKET_EVENTS,
                        event.getAggregateId().toString(),
                        event.getPayload()
                );
                event.markPublished(Instant.now(clock));
            } catch (Exception exception) {
                log.error("Failed to publish ticket outbox event {}", event.getId(), exception);
                event.incrementRetry(exception.getMessage());

                if (event.getRetryCount() >= 3) {
                    event.markFailed(exception.getMessage());
                }
            }
        }
    }
}
