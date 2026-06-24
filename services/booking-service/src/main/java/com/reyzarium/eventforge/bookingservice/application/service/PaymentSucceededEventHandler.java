package com.reyzarium.eventforge.bookingservice.application.service;

import com.reyzarium.eventforge.bookingservice.application.event.PaymentSucceededEvent;

public interface PaymentSucceededEventHandler {

    void handle(PaymentSucceededEvent event);
}
