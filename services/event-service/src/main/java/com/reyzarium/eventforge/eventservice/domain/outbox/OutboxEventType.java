package com.reyzarium.eventforge.eventservice.domain.outbox;

public final class OutboxEventType {

    private OutboxEventType() {
    }

    public static final String EVENT_CREATED = "event.created";
    public static final String EVENT_PUBLISHED = "event.published";
    public static final String EVENT_CANCELLED = "event.cancelled";
}