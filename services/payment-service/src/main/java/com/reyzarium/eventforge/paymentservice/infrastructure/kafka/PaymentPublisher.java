package com.reyzarium.eventforge.paymentservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PaymentPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka publish failed", exception);
        }
    }
}
