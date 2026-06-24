package com.reyzarium.eventforge.paymentservice.domain.outbox;

public final class OutboxEventType {

    private OutboxEventType() {
    }

    public static final String PAYMENT_CREATED = "payment.created";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
}
