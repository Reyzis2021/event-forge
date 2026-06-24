package com.reyzarium.eventforge.ticketservice.common.error;

public enum TicketErrorCode {
    TICKET_NOT_FOUND,
    TICKET_ACCESS_DENIED,
    TICKET_ALREADY_USED,
    TICKET_CANCELLED,
    QR_TOKEN_INVALID,
    TICKET_ISSUANCE_ALREADY_PROCESSED,
    PDF_GENERATION_FAILED
}
