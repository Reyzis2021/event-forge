package com.reyzarium.eventforge.bookingservice.application.service;

import com.reyzarium.eventforge.bookingservice.application.event.PaymentFailedEvent;

public interface PaymentFailedEventHandler {

    void handle(PaymentFailedEvent event);
}
