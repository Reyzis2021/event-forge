package com.reyzarium.eventforge.ticketservice.common.error;

import lombok.Getter;

@Getter
public class TicketServiceException extends RuntimeException {

    private final TicketErrorCode errorCode;

    public TicketServiceException(TicketErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
