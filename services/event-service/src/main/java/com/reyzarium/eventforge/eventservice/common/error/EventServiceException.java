package com.reyzarium.eventforge.eventservice.common.error;

import lombok.Getter;

@Getter
public class EventServiceException extends RuntimeException {

    private final EventErrorCode errorCode;

    public EventServiceException(EventErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}