package com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity;

import com.reyzarium.eventforge.bookingservice.domain.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error")
    private String lastError;

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.lastError = errorMessage;
    }

    public void incrementRetry(String errorMessage) {
        this.retryCount = retryCount + 1;
        this.lastError = errorMessage;
    }
}