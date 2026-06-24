package com.reyzarium.eventforge.ticketservice.application.service.impl;

import com.reyzarium.eventforge.ticketservice.application.service.TicketPdfService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockTicketPdfService implements TicketPdfService {

    private final String publicBaseUrl;

    public MockTicketPdfService(@Value("${ticket-service.public-base-url}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String createFileKey(UUID ticketId) {
        return "tickets/" + ticketId + ".pdf";
    }

    @Override
    public String createPdfUrl(String fileKey) {
        return publicBaseUrl + "/mock-files/" + fileKey;
    }
}
