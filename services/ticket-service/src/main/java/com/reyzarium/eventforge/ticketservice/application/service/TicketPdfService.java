package com.reyzarium.eventforge.ticketservice.application.service;

import java.util.UUID;

public interface TicketPdfService {

    String createFileKey(UUID ticketId);

    String createPdfUrl(String fileKey);
}
