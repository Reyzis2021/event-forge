package com.reyzarium.eventforge.bookingservice.domain.outbox;

public final class OutboxEventType {

    private OutboxEventType() {
    }

    public static final String BOOKING_CREATED = "booking.created";
    public static final String BOOKING_EXPIRED = "booking.expired";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKING_PAYMENT_FAILED = "booking.payment_failed";
}
