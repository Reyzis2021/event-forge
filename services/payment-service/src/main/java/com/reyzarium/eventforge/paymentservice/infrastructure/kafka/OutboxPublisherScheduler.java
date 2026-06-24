package com.reyzarium.eventforge.paymentservice.infrastructure.kafka;

import com.reyzarium.eventforge.paymentservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.OutboxEventRepository;
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
    private final PaymentPublisher paymentPublisher;
    private final Clock clock;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        var events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (var event : events) {
            try {
                paymentPublisher.publish(
                        KafkaTopicNames.PAYMENT_EVENTS,
                        event.getAggregateId().toString(),
                        event.getPayload()
                );
                event.markPublished(Instant.now(clock));
            } catch (Exception exception) {
                log.error("Failed to publish payment outbox event {}", event.getId(), exception);
                event.incrementRetry(exception.getMessage());

                if (event.getRetryCount() >= 3) {
                    event.markFailed(exception.getMessage());
                }
            }
        }
    }
}
