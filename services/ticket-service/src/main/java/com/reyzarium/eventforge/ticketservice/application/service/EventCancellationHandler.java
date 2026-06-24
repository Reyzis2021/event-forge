package com.reyzarium.eventforge.ticketservice.application.service;

import com.reyzarium.eventforge.ticketservice.application.event.EventCancelledEvent;

public interface EventCancellationHandler {

    void handle(EventCancelledEvent event);
}
