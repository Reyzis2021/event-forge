package com.reyzarium.eventforge.ticketservice.application.service;

public interface QrTokenService {

    String generateToken();

    String hash(String rawToken);
}
