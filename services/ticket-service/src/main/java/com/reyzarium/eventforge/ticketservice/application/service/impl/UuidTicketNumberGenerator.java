package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.service.TicketNumberGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidTicketNumberGenerator implements TicketNumberGenerator {

    @Override
    public String generate() {
        return "EF-" + UUID.randomUUID();
    }
}
